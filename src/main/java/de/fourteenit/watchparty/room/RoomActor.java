package de.fourteenit.watchparty.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fourteenit.watchparty.protocol.Messages;
import de.fourteenit.watchparty.ws.ClientSession;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Der Eventloop des Raums (ADR-009).
 *
 * Alles, was Raum- oder Session-Zustand aendert, wird hier eingereiht und von
 * genau einem Thread seriell abgearbeitet. Die WebSocket-Threads rufen nur die
 * oeffentlichen Methoden auf und fassen den Zustand nie direkt an.
 *
 * Dadurch ist die gesamte Logik in {@code handle*} gewoehnlicher
 * Single-Thread-Code: kein synchronized, kein volatile, keine Concurrent-
 * Collections. Timer-vs-Bet und manueller-vs-automatischer Marktschluss loesen
 * sich allein ueber die Reihenfolge in dieser Queue auf (ADR-010, ADR-011).
 */
@Component
public class RoomActor {

    private static final Logger log = LoggerFactory.getLogger(RoomActor.class);

    /** Anforderung 5: 15 Sekunden zwischen Oeffnen und automatischem Schluss. */
    private static final Duration BETTING_WINDOW = Duration.ofSeconds(15);

    /** Anforderung 3.1: Startguthaben, Mindesteinsatz, Strafe an einer Stelle im Code. */
    private static final Params PARAMS = Params.DEFAULT;

    private final ExecutorService loop = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "room-actor");
        thread.setDaemon(true);
        return thread;
    });

    private final ObjectMapper mapper = new ObjectMapper();
    private final Room room = new Room();

    /** Nur vom Raum-Thread beruehrt, daher eine gewoehnliche HashMap. */
    private final Map<String, ClientSession> sessions = new LinkedHashMap<>();

    /**
     * Massgebliche Uhr fuer {@code closesAt}-Vergleiche (ADR-011) und
     * Scheduler fuer den Auto-Close-Task (ADR-010). Per Konstruktor injiziert,
     * damit Tests eine Fake-Uhr und einen Scheduler unterschieben koennen, der
     * Tasks nur sammelt statt sie zeitgesteuert zu feuern.
     */
    private final Clock clock;
    private final Scheduler scheduler;

    /** Nur eine Optimierung (ADR-010): die Absicherung ist der Runden-ID-Vergleich in handleAutoClose. */
    private Scheduler.ScheduledTask autoCloseTask;

    public RoomActor(Clock clock, Scheduler scheduler) {
        this.clock = clock;
        this.scheduler = scheduler;
    }

    // --- Eintrittspunkte (aufgerufen von WebSocket-Threads) ------------------

    public void connected(ClientSession session) {
        loop.execute(() -> sessions.put(session.getId(), session));
    }

    public void disconnected(ClientSession session) {
        loop.execute(() -> handleDisconnected(session));
    }

    public void join(ClientSession session, String name, String token) {
        loop.execute(() -> handleJoin(session, name, token));
    }

    public void openMarket(ClientSession session) {
        loop.execute(() -> handleOpenMarket(session));
    }

    public void placeBet(ClientSession session, String outcomeId, Integer stake) {
        loop.execute(() -> handlePlaceBet(session, outcomeId, stake));
    }

    public void closeMarket(ClientSession session) {
        loop.execute(() -> handleCloseMarket(session));
    }

    public void resolve(ClientSession session, String outcomeId) {
        loop.execute(() -> handleResolve(session, outcomeId));
    }

    // --- Verarbeitung (laeuft ausschliesslich auf dem Raum-Thread) -----------

    private void handleJoin(ClientSession session, String rawName, String token) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.length() > 20) {
            sendTo(session, new Messages.Error("Bitte einen Namen mit 1 bis 20 Zeichen eingeben."));
            return;
        }

        Player player = room.byToken(token);
        if (player != null) {
            // Reconnect (ADR-014): dasselbe Konto, neue Verbindung. Der
            // Verpasste-Runden-Zaehler beginnt von vorn (Anforderung 8.1).
            player.setName(name);
            player.setConnected(true);
            player.resetMissedRounds();
        } else {
            player = room.addPlayer(UUID.randomUUID().toString(), UUID.randomUUID().toString(), name);
        }

        session.setPlayerId(player.getId());
        reassignHost();

        sendTo(session, new Messages.Welcome(player.getId(), player.getToken()));
        sendYourBetIfAny(session, player.getId());
        broadcastState();
        log.info("{} ist dabei ({} Spieler im Raum)", player.getName(), room.players().size());
    }

    private void handleDisconnected(ClientSession session) {
        sessions.remove(session.getId());
        String playerId = session.getPlayerId();
        if (playerId == null) {
            return;
        }
        // Nur wenn keine andere Session mehr auf diesen Spieler zeigt.
        boolean stillOnline = sessions.values().stream()
                .anyMatch(other -> playerId.equals(other.getPlayerId()));
        if (!stillOnline) {
            Player player = room.byId(playerId);
            if (player != null) {
                player.setConnected(false);
            }
        }
        reassignHost();
        broadcastState();
    }

    private void handleOpenMarket(ClientSession session) {
        if (!room.isHost(session.getPlayerId())) {
            sendTo(session, new Messages.Error("Nur der Host kann den Markt oeffnen."));
            return;
        }
        Phase phase = room.getPhase();
        if (phase == Phase.OPEN || phase == Phase.CLOSED) {
            sendTo(session, new Messages.Error("Es laeuft schon eine Runde."));
            return;
        }

        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }
        Round round = room.openMarket(Markets.DRIVE_OUTCOME, clock.instant(), BETTING_WINDOW);
        long roundId = round.getId();
        autoCloseTask = scheduler.schedule(() -> loop.execute(() -> handleAutoClose(roundId)), BETTING_WINDOW);

        broadcastState();
    }

    private void handlePlaceBet(ClientSession session, String outcomeId, Integer requestedStake) {
        String playerId = session.getPlayerId();
        Player player = playerId == null ? null : room.byId(playerId);
        if (player == null) {
            sendTo(session, new Messages.Error("Bitte zuerst beitreten."));
            return;
        }

        Round round = room.getCurrentRound();
        // ADR-011: allein der Zeitvergleich beim Abarbeiten entscheidet, nicht
        // ob der Auto-Close-Task schon gefeuert hat.
        if (round == null || round.getPhase() != Phase.OPEN || !clock.instant().isBefore(round.getClosesAt())) {
            sendTo(session, new Messages.Error("Das Wettfenster ist nicht offen."));
            return;
        }
        if (round.hasBet(playerId)) {
            sendTo(session, new Messages.Error("Du hast in dieser Runde schon getippt."));
            return;
        }
        boolean validOutcome = round.getMarket().outcomes().stream()
                .anyMatch(outcome -> outcome.id().equals(outcomeId));
        if (!validOutcome) {
            sendTo(session, new Messages.Error("Unbekannter Ausgang."));
            return;
        }

        int stake = resolveStake(player, requestedStake);
        round.addBet(new Bet(playerId, outcomeId, stake));

        sendTo(session, new Messages.YourBet(outcomeId, stake));
        broadcastState();
    }

    /**
     * Anforderung 6/8.3: Der Mindesteinsatz ist der Standard-Einsatz. Wer
     * weniger Punkte als den Mindesteinsatz hat, geht zwangsweise All-in,
     * auch mit 0 Punkten — unabhaengig davon, was angefragt wurde.
     */
    private int resolveStake(Player player, Integer requestedStake) {
        int points = player.getPoints();
        int minStake = PARAMS.minStake();
        if (points < minStake) {
            return points;
        }
        int wanted = requestedStake == null ? minStake : requestedStake;
        return Math.max(minStake, Math.min(wanted, points));
    }

    private void handleCloseMarket(ClientSession session) {
        if (!room.isHost(session.getPlayerId())) {
            sendTo(session, new Messages.Error("Nur der Host kann das Fenster schliessen."));
            return;
        }
        Phase phase = room.getPhase();
        if (phase == Phase.IDLE || phase == Phase.RESOLVED) {
            sendTo(session, new Messages.Error("Kein offener Markt."));
            return;
        }
        if (phase == Phase.CLOSED) {
            // ADR-020: manueller und automatischer Schluss treffen sich
            // regelmaessig, ein doppeltes Schliessen ist kein Fehler.
            return;
        }
        closeRound(room.getCurrentRound());
        broadcastState();
    }

    private void handleAutoClose(long roundId) {
        Round round = room.getCurrentRound();
        if (round == null || round.getPhase() != Phase.OPEN || round.getId() != roundId) {
            // ADR-010: veralteter Timer einer bereits geschlossenen oder
            // schon wieder neuen Runde — ignorieren.
            return;
        }
        closeRound(round);
        broadcastState();
    }

    private void closeRound(Round round) {
        round.setPhase(Phase.CLOSED);
    }

    private void handleResolve(ClientSession session, String winningOutcomeId) {
        if (!room.isHost(session.getPlayerId())) {
            sendTo(session, new Messages.Error("Nur der Host kann aufloesen."));
            return;
        }
        Round round = room.getCurrentRound();
        if (round == null || round.getPhase() != Phase.CLOSED) {
            sendTo(session, new Messages.Error("Der Markt ist nicht geschlossen."));
            return;
        }
        boolean validOutcome = round.getMarket().outcomes().stream()
                .anyMatch(outcome -> outcome.id().equals(winningOutcomeId));
        if (!validOutcome) {
            sendTo(session, new Messages.Error("Unbekannter Ausgang."));
            return;
        }

        List<Bet> bets = List.copyOf(round.getBets().values());
        Set<String> nonBettors = new LinkedHashSet<>(round.getParticipants());
        nonBettors.removeAll(round.getBets().keySet());

        Map<String, Integer> balances = new LinkedHashMap<>();
        for (Player player : room.players()) {
            balances.put(player.getId(), player.getPoints());
        }

        Map<String, Integer> deltas = Settlement.settle(bets, nonBettors, balances, winningOutcomeId, PARAMS);
        for (Map.Entry<String, Integer> entry : deltas.entrySet()) {
            Player player = room.byId(entry.getKey());
            if (player != null) {
                player.setPoints(player.getPoints() + entry.getValue());
            }
        }

        // 8.4: Ohne einen einzigen Tipp gibt es keinen Pool und keine
        // Auszahlung; die Runde ist annulliert.
        boolean annulled = bets.isEmpty();
        int pool = 0;
        if (!annulled) {
            int totalStakes = bets.stream().mapToInt(Bet::stake).sum();
            int collectedPenalties = nonBettors.stream()
                    .mapToInt(id -> Math.min(PARAMS.penalty(), balances.getOrDefault(id, 0)))
                    .sum();
            pool = totalStakes + collectedPenalties;
        }

        // Anforderung 8.1: Nur getrennte Nicht-Tipper zaehlen fuer die Pause;
        // wer verbunden ist und nicht tippt, zahlt jede Runde ohne Pause.
        for (String nonBettorId : nonBettors) {
            Player player = room.byId(nonBettorId);
            if (player != null && !player.isConnected()) {
                player.incrementMissedRounds();
            }
        }

        round.setWinningOutcomeId(winningOutcomeId);
        round.setDeltas(deltas);
        round.setPool(pool);
        round.setAnnulled(annulled);
        round.setPhase(Phase.RESOLVED);

        // RESOLVED erlaubt das Zurueckholen der Host-Rolle (ADR-021).
        reassignHost();

        broadcastState();
    }

    private void reassignHost() {
        Phase phase = room.getPhase();
        boolean allowPickup = phase == Phase.IDLE || phase == Phase.RESOLVED;
        room.reassignHostIfNeeded(allowPickup);
    }

    private void sendYourBetIfAny(ClientSession session, String playerId) {
        Round round = room.getCurrentRound();
        if (round == null || round.getPhase() != Phase.OPEN) {
            return;
        }
        Bet bet = round.getBets().get(playerId);
        if (bet != null) {
            sendTo(session, new Messages.YourBet(bet.outcomeId(), bet.stake()));
        }
    }

    // --- Ausgang ------------------------------------------------------------

    private void broadcastState() {
        Messages.State state = snapshot();
        String payload = serialize(state);
        if (payload == null) {
            return;
        }
        for (ClientSession session : sessions.values()) {
            session.send(payload);
        }
    }

    private Messages.State snapshot() {
        List<Messages.PlayerView> views = new ArrayList<>();
        for (Player player : room.players()) {
            views.add(new Messages.PlayerView(
                    player.getId(),
                    player.getName(),
                    player.getPoints(),
                    player.isConnected(),
                    player.isPaused(),
                    room.isHost(player.getId())));
        }

        Round round = room.getCurrentRound();
        Phase phase = room.getPhase();
        Messages.MarketView market = null;
        Long roundId = null;
        Long closesAt = null;
        Integer betCount = null;
        Integer participantCount = null;
        List<Messages.RevealedBet> revealedBets = null;
        String winningOutcomeId = null;
        Integer pool = null;
        Boolean annulled = null;
        Map<String, Integer> deltas = null;

        if (round != null) {
            roundId = round.getId();
            market = new Messages.MarketView(
                    round.getMarket().id(), round.getMarket().question(), round.getMarket().outcomes());

            if (phase == Phase.OPEN) {
                // Invariante 4 / ADR-013: waehrend OPEN nur der Zaehler, nie
                // einzelne Tipps.
                closesAt = round.getClosesAt().toEpochMilli();
                betCount = round.getBets().size();
                participantCount = round.getParticipants().size();
            } else if (phase == Phase.CLOSED || phase == Phase.RESOLVED) {
                revealedBets = round.getBets().values().stream()
                        .map(bet -> new Messages.RevealedBet(bet.playerId(), bet.outcomeId(), bet.stake()))
                        .toList();
                if (phase == Phase.RESOLVED) {
                    winningOutcomeId = round.getWinningOutcomeId();
                    pool = round.getPool();
                    annulled = round.isAnnulled();
                    deltas = round.getDeltas();
                }
            }
        }

        return new Messages.State(views, room.getHostPlayerId(), phase.name(), roundId, market,
                closesAt, clock.instant().toEpochMilli(), betCount, participantCount, revealedBets,
                winningOutcomeId, pool, annulled, deltas);
    }

    private void sendTo(ClientSession session, Object message) {
        String payload = serialize(message);
        if (payload != null) {
            session.send(payload);
        }
    }

    private String serialize(Object message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Nachricht nicht serialisierbar", e);
            return null;
        }
    }

    /** Paket-privater Testzugang zum Raumzustand, ohne ihn ueber die WebSocket-JSON pruefen zu muessen. */
    Room getRoomForTest() {
        return room;
    }

    /**
     * Paket-privater Testzugang: blockiert, bis alle bis hierhin eingereihten
     * Kommandos abgearbeitet sind. Ohne ihn waeren Actor-Tests race-behaftet,
     * weil {@code loop.execute(...)} asynchron ist.
     */
    void awaitIdle() {
        try {
            loop.submit(() -> null).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Warten auf den Raum-Thread unterbrochen", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Raum-Thread wurde nicht rechtzeitig leer", e);
        }
    }

    @PreDestroy
    void shutdown() {
        loop.shutdownNow();
    }
}
