package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.application.port.in.RoomCommands;
import de.fourteen.watchparty.application.port.out.ClientGateway;
import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.application.port.out.SnapshotRepository;
import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.domain.model.Bet;
import de.fourteen.watchparty.domain.model.BetId;
import de.fourteen.watchparty.domain.model.Bets;
import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Params;
import de.fourteen.watchparty.domain.model.Phase;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.Player;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.PlayerName;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.Room;
import de.fourteen.watchparty.domain.model.Round;
import de.fourteen.watchparty.domain.model.RoundId;
import de.fourteen.watchparty.domain.model.Token;
import de.fourteen.watchparty.domain.service.Settlement;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
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
 * Der Eventloop des Raums (ADR-009) und zugleich die Umsetzung des
 * Eingangs-Ports {@link RoomCommands}.
 *
 * Alles, was Raum- oder Sitzungszustand aendert, wird hier eingereiht und von
 * genau einem Thread seriell abgearbeitet. Die Adapter rufen nur die
 * Port-Methoden auf und fassen den Zustand nie direkt an.
 *
 * Dadurch ist die gesamte Logik in {@code handle*} gewoehnlicher
 * Single-Thread-Code: kein synchronized, kein volatile, keine Concurrent-
 * Collections. Timer-vs-Pick und manueller-vs-automatischer Schluss loesen
 * sich allein ueber die Reihenfolge in dieser Queue auf (ADR-010, ADR-011).
 *
 * Diese Klasse traegt keine Framework-Annotation: Sie liegt im
 * Anwendungsring und weiss weder von Spring noch von WebSockets noch von
 * JSON. Verdrahtet wird sie in {@code config}, gesendet wird ueber den
 * {@link ClientGateway}, geschrieben ueber das {@link SnapshotRepository}.
 */
@Criticality(level = Criticality.Level.MEDIUM,
        requirements = { "5-a", "5-b", "5-c", "5-d", "8.6", "8.6-a", "8.6-b", "8.7", "8.7-a",
                "9-a", "9-b", "9-c", "10-a", "10-b", "10.1", "10.1-a", "10.1-b", "10.1-c" })
public class RoomActor implements RoomCommands {

    private static final Logger log = LoggerFactory.getLogger(RoomActor.class);

    /** Anforderung 5: 15 Sekunden zwischen Oeffnen und automatischem Schluss. */
    private static final Duration BETTING_WINDOW = Duration.ofSeconds(15);

    /** Anforderung 3.1: Startguthaben, Mindesteinsatz, Strafe an einer Stelle im Code. */
    private static final Params PARAMS = Params.DEFAULT;

    /**
     * Trennt "Neustart mitten im Abend" (wiederherstellen) von "naechster
     * Spielabend" (frisch anfangen) -- Frage C im Snapshot-Plan (ADR-023),
     * entschieden am 2026-08-02. Laenger als ein Spiel samt Pausen und
     * Verlaengerung, deutlich kuerzer als der Abstand zum naechsten Mal.
     */
    private static final Duration SNAPSHOT_TTL = Duration.ofHours(6);

    private final ExecutorService loop = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "room-actor");
        thread.setDaemon(true);
        return thread;
    });

    /** Nicht final: {@code RESET} ersetzt den Raum durch einen leeren. */
    private Room room = new Room();

    /**
     * Welche Sitzung auf welchen Spieler zeigt; {@code null} heisst verbunden,
     * aber noch nicht beigetreten. Zugleich der Empfaengerkreis fuer
     * {@code broadcastState}.
     *
     * Diese Zuordnung lag frueher als Feld in {@code ClientSession} und damit
     * in der Infrastruktur. Sie gehoert hierher: Nur vom Raum-Thread
     * beruehrt, daher eine gewoehnliche LinkedHashMap (Invariante 1).
     */
    private final Map<String, @Nullable PlayerId> playerIdBySession = new LinkedHashMap<>();

    /**
     * Massgebliche Uhr fuer {@code closesAt}-Vergleiche (ADR-011) und
     * Scheduler fuer den Auto-Close-Task (ADR-010). Per Konstruktor injiziert,
     * damit Tests eine Fake-Uhr und einen Scheduler unterschieben koennen, der
     * Tasks nur sammelt statt sie zeitgesteuert zu feuern.
     *
     * {@link Clock} bleibt bewusst der JDK-Typ statt eines eigenen Ports: Er
     * ist bereits genau die Abstraktion, die ein Port hier nur nachbauen
     * wuerde.
     */
    private final Clock clock;
    private final Scheduler scheduler;

    /** Nur eine Optimierung (ADR-010): die Absicherung ist der Runden-ID-Vergleich in handleAutoClose. */
    private Scheduler.@Nullable ScheduledTask autoCloseTask;

    private final SnapshotRepository snapshots;
    private final ClientGateway clients;

    public RoomActor(Clock clock, Scheduler scheduler, SnapshotRepository snapshots, ClientGateway clients) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.snapshots = snapshots;
        this.clients = clients;
    }

    /**
     * Reiht das Laden als erstes Kommando in die Actor-Queue ein (ADR-023):
     * Damit laeuft es auf dem Raum-Thread und ist garantiert vor dem ersten
     * {@code JOIN} fertig, ohne Sonderfall in Invariante 1 -- die Adapter
     * reihen ja nur ein und draengeln sich nicht vor.
     *
     * Wird von {@code config} als {@code initMethod} aufgerufen; frueher
     * stand hier ein {@code @PostConstruct} und damit Spring im Kern.
     */
    public void loadOnStartup() {
        loop.execute(this::handleRestore);
    }

    // --- Eingangs-Port (aufgerufen von Adapter-Threads) ----------------------

    @Override
    public void connected(String sessionId) {
        loop.execute(() -> playerIdBySession.put(sessionId, null));
    }

    @Override
    public void disconnected(String sessionId) {
        loop.execute(() -> handleDisconnected(sessionId));
    }

    @Override
    public void join(String sessionId, @Nullable String name, @Nullable String token) {
        loop.execute(() -> handleJoin(sessionId, name, token));
    }

    @Override
    public void openBet(String sessionId, @Nullable String betId) {
        loop.execute(() -> handleOpenBet(sessionId, betId));
    }

    @Override
    public void placePick(String sessionId, @Nullable String outcomeId, @Nullable Integer stake) {
        loop.execute(() -> handlePlacePick(sessionId, outcomeId, stake));
    }

    @Override
    public void closeBet(String sessionId) {
        loop.execute(() -> handleCloseBet(sessionId));
    }

    @Override
    public void resolve(String sessionId, @Nullable String outcomeId) {
        loop.execute(() -> handleResolve(sessionId, outcomeId));
    }

    @Override
    public void annul(String sessionId) {
        loop.execute(() -> handleAnnul(sessionId));
    }

    @Override
    public void reset(String sessionId) {
        loop.execute(() -> handleReset(sessionId));
    }

    // --- Verarbeitung (laeuft ausschliesslich auf dem Raum-Thread) -----------

    /**
     * Laedt den zuletzt geschriebenen Snapshot, falls einer da ist und noch
     * nicht abgelaufen (ADR-023). Ein fehlender oder kaputter Snapshot ist
     * kein Fehler -- das Repository traegt die Regel "im Zweifel leer starten"
     * schon selbst.
     *
     * Eine offene Runde, deren Fenster waehrend des Neustarts abgelaufen
     * ist, wird schlicht geschlossen: Ein Deploy faellt nicht in einen
     * echten Spielabend (ADR-019, README), betrifft also nur Tests -- kein
     * eigener Grund fuers Annullieren noetig.
     */
    private void handleRestore() {
        snapshots.load(clock.instant(), SNAPSHOT_TTL).ifPresent(snapshot -> {
            room = Room.fromSnapshot(snapshot);
            Round round = room.getCurrentRound();
            if (round != null && round.getPhase() == Phase.OPEN) {
                if (clock.instant().isBefore(round.getClosesAt())) {
                    RoundId roundId = round.getId();
                    Duration remaining = Duration.between(clock.instant(), round.getClosesAt());
                    autoCloseTask = scheduler.schedule(() -> loop.execute(() -> handleAutoClose(roundId)), remaining);
                } else {
                    room.closeCurrentRound();
                }
            }
            log.info("Zustand aus Snapshot wiederhergestellt: {} Spieler", room.players().size());
        });
    }

    private void handleJoin(String sessionId, @Nullable String rawName, @Nullable String rawToken) {
        // Die Regel steckt in PlayerName; die Meldung gehoert hierher, weil
        // die Domaene nicht entscheidet, was der Spieler zu lesen bekommt.
        // Der explizite Null-Check ist fuer NullAway da: isValid(null) ist
        // zwar bereits false, aber ein Aufruf allein narrowt rawName nicht.
        if (rawName == null || !PlayerName.isValid(rawName)) {
            clients.send(sessionId, new Messages.Error("Bitte einen Namen mit 1 bis 20 Zeichen eingeben."));
            return;
        }
        PlayerName name = PlayerName.of(rawName);

        Player player = room.rejoin(Token.ofNullable(rawToken), name);
        if (player == null) {
            player = room.addPlayer(
                    PlayerId.of(UUID.randomUUID().toString()),
                    Token.of(UUID.randomUUID().toString()),
                    name);
        }

        playerIdBySession.put(sessionId, player.getId());
        reassignHost();

        clients.send(sessionId, new Messages.Welcome(
                player.getId().value(), player.getToken().value(), RoomView.catalog()));
        sendYourPickIfAny(sessionId, player.getId());
        broadcastState();
        log.info("{} ist dabei ({} Spieler im Raum)", player.getName(), room.players().size());
    }

    private void handleDisconnected(String sessionId) {
        PlayerId playerId = playerIdBySession.remove(sessionId);
        if (playerId == null) {
            return;
        }
        // Nur wenn keine andere Sitzung mehr auf diesen Spieler zeigt.
        if (!playerIdBySession.containsValue(playerId)) {
            room.markDisconnected(playerId);
        }
        reassignHost();
        broadcastState();
    }

    private void handleOpenBet(String sessionId, @Nullable String betId) {
        if (!isHost(sessionId)) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann eine Wette öffnen."));
            return;
        }
        Phase phase = room.getPhase();
        if (phase == Phase.OPEN || phase == Phase.CLOSED) {
            clients.send(sessionId, new Messages.Error("Es läuft schon eine Runde."));
            return;
        }
        // Ohne Angabe der Drive-Ausgang: die mit Abstand haeufigste Wette, und
        // aeltere Clients kennen die Auswahl noch nicht.
        BetId requested = BetId.ofNullable(betId);
        Bet bet = requested == null ? Bets.DRIVE_OUTCOME : Bets.byId(requested);
        if (bet == null) {
            clients.send(sessionId, new Messages.Error("Unbekannte Wette."));
            return;
        }

        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }
        Round round = room.openBet(bet, clock.instant(), BETTING_WINDOW);
        RoundId roundId = round.getId();
        autoCloseTask = scheduler.schedule(() -> loop.execute(() -> handleAutoClose(roundId)), BETTING_WINDOW);

        broadcastState();
    }

    private void handlePlacePick(String sessionId, @Nullable String rawOutcomeId, @Nullable Integer requestedStake) {
        PlayerId playerId = playerIdBySession.get(sessionId);
        if (playerId == null) {
            clients.send(sessionId, new Messages.Error("Bitte zuerst beitreten."));
            return;
        }
        Player player = room.byId(playerId);
        if (player == null) {
            clients.send(sessionId, new Messages.Error("Bitte zuerst beitreten."));
            return;
        }

        Round round = room.getCurrentRound();
        // ADR-011: allein der Zeitvergleich beim Abarbeiten entscheidet, nicht
        // ob der Auto-Close-Task schon gefeuert hat.
        if (round == null || !round.isOpenAt(clock.instant())) {
            clients.send(sessionId, new Messages.Error("Das Wettfenster ist nicht offen."));
            return;
        }
        if (round.hasPick(playerId)) {
            clients.send(sessionId, new Messages.Error("Du hast in dieser Runde schon getippt."));
            return;
        }
        // Direkt auf outcomeId selbst geprueft (statt nur auf hasOutcome()),
        // damit der Compiler ab hier weiss, dass outcomeId nicht null ist --
        // dieselbe Fehlermeldung wie zuvor, jetzt nur nachweisbar richtig.
        OutcomeId outcomeId = OutcomeId.ofNullable(rawOutcomeId);
        if (outcomeId == null || !round.getBet().hasOutcome(outcomeId)) {
            clients.send(sessionId, new Messages.Error("Unbekannter Ausgang."));
            return;
        }

        Points stake = player.stakeFor(requestedStake, PARAMS);
        room.addPick(new Pick(playerId, outcomeId, stake));

        clients.send(sessionId, new Messages.YourPick(outcomeId.value(), stake.value()));
        broadcastState();
    }

    private void handleCloseBet(String sessionId) {
        if (!isHost(sessionId)) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann das Fenster schließen."));
            return;
        }
        Phase phase = room.getPhase();
        if (phase == Phase.IDLE || phase == Phase.RESOLVED) {
            clients.send(sessionId, new Messages.Error("Keine offene Wette."));
            return;
        }
        if (phase == Phase.CLOSED) {
            // ADR-020: manueller und automatischer Schluss treffen sich
            // regelmaessig, ein doppeltes Schliessen ist kein Fehler.
            return;
        }
        room.closeCurrentRound();
        broadcastState();
    }

    private void handleAutoClose(RoundId roundId) {
        Round round = room.getCurrentRound();
        if (round == null || round.getPhase() != Phase.OPEN || !round.getId().equals(roundId)) {
            // ADR-010: veralteter Timer einer bereits geschlossenen oder
            // schon wieder neuen Runde — ignorieren.
            return;
        }
        room.closeCurrentRound();
        broadcastState();
    }

    /**
     * Anforderung 8.6: Die offene Wette passt nicht mehr zum Spiel — etwa weil
     * das Team statt des Field Goals doch auf den vierten Versuch geht. Dann
     * gibt es keinen ehrlichen Ausgang, und der Host dreht die Runde zurueck.
     *
     * Nach RESOLVED ist Schluss — dann waere es kein Abbruch mehr, sondern
     * eine Rueckabwicklung.
     */
    private void handleAnnul(String sessionId) {
        if (!isHost(sessionId)) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann eine Runde annullieren."));
            return;
        }
        Round round = room.getCurrentRound();
        Phase phase = room.getPhase();
        if (round == null || (phase != Phase.OPEN && phase != Phase.CLOSED)) {
            clients.send(sessionId, new Messages.Error("Es läuft keine Runde, die sich annullieren ließe."));
            return;
        }

        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }
        room.annulCurrentRound();

        // RESOLVED erlaubt das Zurueckholen der Host-Rolle (ADR-021).
        reassignHost();

        log.info("Runde {} vom Host annulliert", round.getId());
        broadcastState();
    }

    /**
     * Setzt den ganzen Raum zurueck (ADR-023): Ohne den Neustart als
     * impliziten Reset braucht es einen expliziten. Anders als {@code ANNUL}
     * nicht auf eine Phase beschraenkt und nimmt auch die Spieler mit --
     * Testrunden vom Aufbau oder ein doppelt beigetretener Spieler sollen
     * verschwinden koennen, nicht nur der Punktestand.
     *
     * Invariante 5 (Nullsumme) gilt innerhalb eines Spiels; RESET beendet
     * das Spiel, statt Punkte zu verschieben.
     */
    private void handleReset(String sessionId) {
        if (!isHost(sessionId)) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann den Raum zurücksetzen."));
            return;
        }
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
            autoCloseTask = null;
        }
        room = new Room();
        // Die Sitzungen bleiben offen, zeigen aber auf niemanden mehr -- der
        // Client erkennt das am fehlenden eigenen Spieler im naechsten
        // STATE und faellt in die Beitrittsansicht zurueck. Bewusst kein
        // automatisches Wiederbeitreten, sonst waere RESET nur Anzeige.
        playerIdBySession.replaceAll((sessionKey, playerId) -> null);

        log.info("Raum vom Host zurückgesetzt");
        broadcastState();
    }

    private void handleResolve(String sessionId, @Nullable String rawWinningOutcomeId) {
        if (!isHost(sessionId)) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann auflösen."));
            return;
        }
        Round round = room.getCurrentRound();
        if (round == null || round.getPhase() != Phase.CLOSED) {
            clients.send(sessionId, new Messages.Error("Die Wette ist nicht geschlossen."));
            return;
        }
        OutcomeId winningOutcomeId = OutcomeId.ofNullable(rawWinningOutcomeId);
        if (winningOutcomeId == null || !round.getBet().hasOutcome(winningOutcomeId)) {
            clients.send(sessionId, new Messages.Error("Unbekannter Ausgang."));
            return;
        }

        Set<PlayerId> nonPickers = round.nonPickers();

        Map<PlayerId, Points> balances = new LinkedHashMap<>();
        for (Player player : room.players()) {
            balances.put(player.getId(), player.getPoints());
        }

        // Die gesamte Punkte-Oekonomie steckt in dieser einen Zeile: Deltas,
        // Pool und die Annullierung nach 8.4 kommen aus derselben Rechnung.
        // Der Actor wendet sie nur an (ADR-020).
        Settlement.Result settlement = Settlement.settle(
                round.picksInOrder(), nonPickers, balances, winningOutcomeId, PARAMS);

        room.countMissedRounds(nonPickers);
        room.resolveCurrentRound(winningOutcomeId, settlement.deltas(), settlement.pool(), settlement.annulled());

        // RESOLVED erlaubt das Zurueckholen der Host-Rolle (ADR-021).
        reassignHost();

        broadcastState();
    }

    private boolean isHost(String sessionId) {
        return room.isHost(playerIdBySession.get(sessionId));
    }

    private void reassignHost() {
        Phase phase = room.getPhase();
        boolean allowPickup = phase == Phase.IDLE || phase == Phase.RESOLVED;
        room.reassignHostIfNeeded(allowPickup);
    }

    private void sendYourPickIfAny(String sessionId, PlayerId playerId) {
        Round round = room.getCurrentRound();
        if (round == null || round.getPhase() != Phase.OPEN) {
            return;
        }
        Pick pick = round.pickOf(playerId);
        if (pick != null) {
            clients.send(sessionId, new Messages.YourPick(pick.outcomeId().value(), pick.stake().value()));
        }
    }

    // --- Ausgang ------------------------------------------------------------

    private void broadcastState() {
        Messages.State state = RoomView.state(room, clock.instant().toEpochMilli());
        // Der Empfaengerkreis wird hier auf dem Raum-Thread festgelegt und als
        // Kopie uebergeben; der Adapter darf ihn nicht ueberdauern (Invariante 1).
        clients.sendToAll(List.copyOf(playerIdBySession.keySet()), state);
        // "Zustand geaendert, aber nicht gespeichert" soll gar nicht erst
        // als Zustand existieren (ADR-023) -- deshalb an derselben Stelle
        // wie das Senden, nicht an einer eigenen.
        snapshots.save(room.toSnapshot(clock.instant().toEpochMilli()));
    }

    /** Paket-privater Testzugang zum Raumzustand, ohne ihn ueber die Nachrichten pruefen zu muessen. */
    Room getRoomForTest() {
        return room;
    }

    /**
     * Testzugang: blockiert, bis alle bis hierhin eingereihten Kommandos
     * abgearbeitet sind. Ohne ihn waeren Port-to-Port-Szenarien race-behaftet,
     * weil {@code loop.execute(...)} asynchron ist. Oeffentlich, weil die
     * JGiven-Stufen im Stufen-Paket (docs/teststrategie.md, Abschnitt 8) in
     * einem anderen Paket liegen als {@code application}.
     */
    public void awaitIdle() {
        try {
            loop.submit(() -> null).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Warten auf den Raum-Thread unterbrochen", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Raum-Thread wurde nicht rechtzeitig leer", e);
        }
    }

    /** Wird von {@code config} als {@code destroyMethod} aufgerufen. */
    public void shutdown() {
        loop.shutdownNow();
    }
}
