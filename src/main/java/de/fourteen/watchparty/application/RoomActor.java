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
import de.fourteen.watchparty.domain.model.RoomCode;
import de.fourteen.watchparty.domain.model.RoomSnapshot;
import de.fourteen.watchparty.domain.model.Round;
import de.fourteen.watchparty.domain.model.RoundId;
import de.fourteen.watchparty.domain.model.Token;
import de.fourteen.watchparty.domain.service.Settlement;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
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
 * Der Eventloop aller Watchpartys (ADR-009, seit ADR-033 mehrere statt
 * einer) und zugleich die Umsetzung des Eingangs-Ports {@link RoomCommands}.
 *
 * Alles, was Raum- oder Sitzungszustand aendert, wird hier eingereiht und von
 * genau einem Thread seriell abgearbeitet — ein gemeinsamer Loop fuer alle
 * Watchpartys, keiner je Raum (ADR-033): die Threadzahl waechst nicht mit der
 * Zahl gleichzeitiger Watchpartys, und ein Raum kann die anderen nicht
 * ausbremsen, weil dieser Thread ohnehin nie blockiert (Invariante 2).
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
        requirements = { "5-a", "5-b", "5-c", "5-d", "5-g", "8.6", "8.6-a", "8.6-b", "8.7", "8.7-a",
                "9-a", "9-b", "9-c", "10-a", "10-b", "10.1", "10.1-a", "10.1-b", "10.1-c",
                "1-g", "1-h", "1-i", "1-j" })
public class RoomActor implements RoomCommands {

    private static final Logger log = LoggerFactory.getLogger(RoomActor.class);

    /** Anforderung 5: 15 Sekunden zwischen Oeffnen und automatischem Schluss. */
    private static final Duration BETTING_WINDOW = Duration.ofSeconds(15);

    /** Anforderung 3.1: Startguthaben, Mindesteinsatz, Strafe an einer Stelle im Code. */
    private static final Params PARAMS = Params.DEFAULT;

    /**
     * Dient seit ADR-033 zwei Zwecken mit derselben Zahl (Anforderung 1-j
     * begruendet das ausdruecklich als dieselben sechs Stunden): trennt beim
     * Laden "Neustart mitten im Abend" von "naechster Spielabend" (Frage C
     * im Snapshot-Plan, ADR-023, entschieden am 2026-08-02), und ist zur
     * Laufzeit die Frist, nach der eine Watchparty ohne Aktivitaet verworfen
     * wird. Laenger als ein Spiel samt Pausen und Verlaengerung, deutlich
     * kuerzer als der Abstand zum naechsten Mal.
     */
    private static final Duration ROOM_TTL = Duration.ofHours(6);

    /**
     * Wie oft der Aufraeum-Sweep (Anforderung 1-j) nach abgelaufenen
     * Watchpartys sucht. Muss nicht sekundengenau sein — ein stuendlicher
     * Sweep ist billig und nah genug an den sechs Stunden dran.
     */
    private static final Duration CLEANUP_INTERVAL = Duration.ofHours(1);

    private final ExecutorService loop = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "room-actor");
        thread.setDaemon(true);
        return thread;
    });

    /** Alle lebenden Watchpartys, nach ihrem Code. */
    private final Map<RoomCode, Room> rooms = new LinkedHashMap<>();

    /**
     * Wann eine Watchparty zuletzt ein zustandsaenderndes Kommando gesehen
     * hat — dieselbe Marke wie das Speichern des Snapshots (Anforderung
     * 1-j), aktualisiert an derselben Stelle in {@link #broadcastState}.
     */
    private final Map<RoomCode, Instant> lastActivity = new LinkedHashMap<>();

    /** Je Watchparty hoechstens ein ausstehender Auto-Close-Task. */
    private final Map<RoomCode, Scheduler.ScheduledTask> autoCloseTasks = new LinkedHashMap<>();

    /** Zu welcher Watchparty und welchem Spieler eine Sitzung gehoert. */
    private record SessionBinding(RoomCode roomCode, PlayerId playerId) {
    }

    /**
     * Welche Sitzung auf welche Watchparty und welchen Spieler zeigt. Eine
     * Sitzung landet hier erst, wenn {@code JOIN} erfolgreich war (ADR-033)
     * — vorher gehoert sie zu keiner Watchparty und bekommt folgerichtig
     * auch keine Zustandsmeldung (Anforderung 1-i). Zugleich der
     * Empfaengerkreis fuer {@code broadcastState}, gefiltert auf den
     * jeweiligen Raum.
     *
     * Nur vom Raum-Thread beruehrt, daher eine gewoehnliche LinkedHashMap
     * (Invariante 1).
     */
    private final Map<String, SessionBinding> sessions = new LinkedHashMap<>();

    /** Eine erfolgreich aufgeloeste Sitzung: ihre Watchparty, der Raum selbst, ihr Spieler. */
    private record JoinedSession(RoomCode roomCode, Room room, PlayerId playerId) {
    }

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
        // Nichts zu vermerken: Eine Sitzung gehoert erst zu einer Watchparty,
        // wenn JOIN erfolgreich war (ADR-033) -- vorher gaebe es auch keinen
        // Raum, dessen Empfaengerkreis sie aufnehmen koennte.
    }

    @Override
    public void disconnected(String sessionId) {
        loop.execute(() -> handleDisconnected(sessionId));
    }

    @Override
    public void createRoom(String sessionId, @Nullable String name) {
        loop.execute(() -> handleCreateRoom(sessionId, name));
    }

    @Override
    public void join(String sessionId, @Nullable String name, @Nullable String token, @Nullable String roomCode) {
        loop.execute(() -> handleJoin(sessionId, name, token, roomCode));
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
     * Laedt alle zuletzt geschriebenen, noch nicht abgelaufenen Watchpartys
     * (ADR-023, seit ADR-033 mehrere statt einer). Ein fehlender oder
     * kaputter Snapshot ist kein Fehler -- das Repository traegt die Regel
     * "im Zweifel weglassen" schon selbst.
     *
     * Eine offene Runde, deren Fenster waehrend des Neustarts abgelaufen
     * ist, wird schlicht geschlossen: Ein Deploy faellt nicht in einen
     * echten Spielabend (ADR-019, README), betrifft also nur Tests -- kein
     * eigener Grund fuers Annullieren noetig.
     *
     * Stoesst am Ende den wiederkehrenden Aufraeum-Sweep an (Anforderung
     * 1-j) -- einmalig hier, weil {@code loadOnStartup} garantiert genau
     * einmal laeuft.
     */
    private void handleRestore() {
        for (RoomSnapshot snapshot : snapshots.loadAll(clock.instant(), ROOM_TTL)) {
            Room room = Room.fromSnapshot(snapshot);
            RoomCode roomCode = room.getCode();
            rooms.put(roomCode, room);
            lastActivity.put(roomCode, Instant.ofEpochMilli(snapshot.savedAt()));

            Round round = room.getCurrentRound();
            if (round != null && round.getPhase() == Phase.OPEN) {
                if (clock.instant().isBefore(round.getClosesAt())) {
                    RoundId roundId = round.getId();
                    Duration remaining = Duration.between(clock.instant(), round.getClosesAt());
                    autoCloseTasks.put(roomCode,
                            scheduler.schedule(() -> loop.execute(() -> handleAutoClose(roomCode, roundId)),
                                    remaining));
                } else {
                    room.closeCurrentRound();
                }
            }
        }
        log.info("{} Watchparty(s) aus Snapshot wiederhergestellt", rooms.size());
        scheduleCleanup();
    }

    private void scheduleCleanup() {
        scheduler.schedule(() -> loop.execute(this::handleCleanup), CLEANUP_INTERVAL);
    }

    /**
     * Anforderung 1-j: Eine Watchparty ohne Aktivitaet seit {@link
     * #ROOM_TTL} wird verworfen, samt ihrem Snapshot. Ein leerer Raum bleibt
     * bewusst bestehen, solange noch Aktivitaet vorliegt -- "Aktivitaet" ist
     * jedes zustandsaendernde Kommando, nicht die Anwesenheit von Spielern.
     *
     * Plant sich am Ende selbst neu (Endlosschleife ueber den Scheduler statt
     * eine Wiederholung im Scheduler-Port selbst, der nur einmalige
     * Verzoegerung kennt).
     */
    private void handleCleanup() {
        Instant now = clock.instant();
        List<RoomCode> abgelaufen = lastActivity.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue(), now).compareTo(ROOM_TTL) > 0)
                .map(Map.Entry::getKey)
                .toList();
        for (RoomCode roomCode : abgelaufen) {
            Scheduler.ScheduledTask task = autoCloseTasks.remove(roomCode);
            if (task != null) {
                task.cancel();
            }
            rooms.remove(roomCode);
            lastActivity.remove(roomCode);
            sessions.entrySet().removeIf(entry -> entry.getValue().roomCode().equals(roomCode));
            snapshots.delete(roomCode.value());
            log.info("Watchparty {} nach sechs Stunden Inaktivität entfernt", roomCode);
        }
        scheduleCleanup();
    }

    /**
     * Loest eine Sitzung auf ihre Watchparty auf, sofern sie einer beigetreten
     * ist und diese noch existiert (sie kann zwischenzeitlich aufgeraeumt
     * worden sein, Anforderung 1-j). {@code null} heisst fuer den Aufrufer
     * durchgehend "wie noch nicht beigetreten".
     */
    private @Nullable JoinedSession resolveSession(String sessionId) {
        SessionBinding binding = sessions.get(sessionId);
        if (binding == null) {
            return null;
        }
        Room room = rooms.get(binding.roomCode());
        if (room == null) {
            return null;
        }
        return new JoinedSession(binding.roomCode(), room, binding.playerId());
    }

    /**
     * Anforderung 1-g: Erzeugt eine neue Watchparty, deren Ersteller ihr Host
     * wird (ADR-016 -- der erste Beitretende). Fachlich ein eigenes Kommando,
     * kein Sonderfall von {@link #handleJoin}: Erzeugen bringt ein Aggregat
     * erst in die Welt, Beitreten (ob zum ersten Mal oder als Reconnect --
     * das ist wiederum derselbe Vorgang, ADR-014) setzt eines voraus, das
     * schon existiert.
     */
    private void handleCreateRoom(String sessionId, @Nullable String rawName) {
        PlayerName name = validatedName(sessionId, rawName);
        if (name == null) {
            return;
        }
        RoomCode roomCode = freeRoomCode();
        Room room = new Room(roomCode);
        rooms.put(roomCode, room);
        Player player = room.addPlayer(
                PlayerId.of(UUID.randomUUID().toString()),
                Token.of(UUID.randomUUID().toString()),
                name,
                PARAMS.startingPoints());

        bindAndWelcome(sessionId, roomCode, room, player);
        log.info("{} hat Watchparty {} erzeugt und ist ihr Host", player.getName(), roomCode);
    }

    /**
     * Anforderung 1-h/1-i: Tritt der Watchparty zu {@code rawRoomCode} bei --
     * neu oder erneut, das entscheidet allein, ob {@code rawToken} zu einem
     * bestehenden Spieler gehoert (ADR-014). Ein unbekannter oder nicht
     * wohlgeformter Code fuehrt zum selben Fehler (Anforderung 1-i,
     * Kriterium 3) -- ein Tippfehler beim Vorlesen soll niemanden in ein
     * leeres eigenes Wohnzimmer setzen, das es so gar nicht gibt.
     */
    private void handleJoin(String sessionId, @Nullable String rawName, @Nullable String rawToken,
            @Nullable String rawRoomCode) {
        PlayerName name = validatedName(sessionId, rawName);
        if (name == null) {
            return;
        }
        Room room = lookupRoom(sessionId, rawRoomCode);
        if (room == null) {
            return;
        }
        RoomCode roomCode = room.getCode();

        Player player = room.rejoin(Token.ofNullable(rawToken), name);
        if (player == null) {
            player = room.addPlayer(
                    PlayerId.of(UUID.randomUUID().toString()),
                    Token.of(UUID.randomUUID().toString()),
                    name,
                    PARAMS.startingPoints());
        }

        bindAndWelcome(sessionId, roomCode, room, player);
        log.info("{} ist dabei ({} Spieler in Watchparty {})", player.getName(), room.players().size(), roomCode);
    }

    // Die Regel steckt in PlayerName; die Meldung gehoert hierher, weil die
    // Domaene nicht entscheidet, was der Spieler zu lesen bekommt. Der
    // explizite Null-Check ist fuer NullAway da: isValid(null) ist zwar
    // bereits false, aber ein Aufruf allein narrowt rawName nicht.
    private @Nullable PlayerName validatedName(String sessionId, @Nullable String rawName) {
        if (rawName == null || !PlayerName.isValid(rawName)) {
            clients.send(sessionId, new Messages.Error("Bitte einen Namen mit 1 bis 20 Zeichen eingeben."));
            return null;
        }
        return PlayerName.of(rawName);
    }

    // Zwei getrennte Nullchecks statt einem zusammengesetzten: Erst narrowt
    // der eine parsed, dann kann der zweite rooms.get(parsed) unmittelbar
    // darauf aufbauen -- ein unbekannter und ein nicht wohlgeformter Code
    // fuehren bewusst zur selben Fehlermeldung (Anforderung 1-i, Kriterium 3).
    private @Nullable Room lookupRoom(String sessionId, @Nullable String rawRoomCode) {
        RoomCode parsed = RoomCode.parse(rawRoomCode);
        if (parsed == null) {
            clients.send(sessionId, new Messages.Error("Unbekannter Raum-Code."));
            return null;
        }
        Room room = rooms.get(parsed);
        if (room == null) {
            clients.send(sessionId, new Messages.Error("Unbekannter Raum-Code."));
            return null;
        }
        return room;
    }

    private void bindAndWelcome(String sessionId, RoomCode roomCode, Room room, Player player) {
        sessions.put(sessionId, new SessionBinding(roomCode, player.getId()));
        reassignHost(room);

        clients.send(sessionId, new Messages.Welcome(
                roomCode.value(), player.getId().value(), player.getToken().value(), RoomView.catalog(),
                RoomView.params(PARAMS)));
        sendYourPickIfAny(sessionId, room, player.getId());
        broadcastState(roomCode, room);
    }

    /** Zieht Codes, bis einer noch von keiner lebenden Watchparty belegt ist. */
    private RoomCode freeRoomCode() {
        RoomCode candidate;
        do {
            candidate = RoomCode.random();
        } while (rooms.containsKey(candidate));
        return candidate;
    }

    private void handleDisconnected(String sessionId) {
        SessionBinding binding = sessions.remove(sessionId);
        if (binding == null) {
            return;
        }
        Room room = rooms.get(binding.roomCode());
        if (room == null) {
            return;
        }
        // Nur wenn keine andere Sitzung mehr auf diesen Spieler zeigt.
        boolean weitereSitzungFuerDenselbenSpieler = sessions.values().stream()
                .anyMatch(other -> other.playerId().equals(binding.playerId()));
        if (!weitereSitzungFuerDenselbenSpieler) {
            room.markDisconnected(binding.playerId());
        }
        reassignHost(room);
        broadcastState(binding.roomCode(), room);
    }

    private void handleOpenBet(String sessionId, @Nullable String betId) {
        JoinedSession joined = resolveSession(sessionId);
        if (joined == null || !joined.room().isHost(joined.playerId())) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann eine Wette öffnen."));
            return;
        }
        RoomCode roomCode = joined.roomCode();
        Room room = joined.room();
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

        Scheduler.ScheduledTask previous = autoCloseTasks.remove(roomCode);
        if (previous != null) {
            previous.cancel();
        }
        Round round = room.openBet(bet, clock.instant(), BETTING_WINDOW);
        RoundId roundId = round.getId();
        autoCloseTasks.put(roomCode,
                scheduler.schedule(() -> loop.execute(() -> handleAutoClose(roomCode, roundId)), BETTING_WINDOW));

        broadcastState(roomCode, room);
    }

    private void handlePlacePick(String sessionId, @Nullable String rawOutcomeId, @Nullable Integer requestedStake) {
        JoinedSession joined = resolveSession(sessionId);
        if (joined == null) {
            clients.send(sessionId, new Messages.Error("Bitte zuerst beitreten."));
            return;
        }
        RoomCode roomCode = joined.roomCode();
        Room room = joined.room();
        Player player = room.byId(joined.playerId());
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
        if (round.hasPick(player.getId())) {
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
        room.addPick(new Pick(player.getId(), outcomeId, stake));

        clients.send(sessionId, new Messages.YourPick(outcomeId.value(), stake.value()));

        // Anforderung 5-g: der dritte Ausloeser fuers Schliessen neben
        // Countdown und Host-Klick. Vor dem broadcastState, damit es keinen
        // Zwischenzustand "alle haben getippt, Fenster noch offen" gibt --
        // der letzte Tipper saehe sonst fuer einen Frame ein Fenster, in dem
        // niemand mehr tippen kann.
        //
        // Das Cancel ist wie in ADR-010 nur eine Optimierung; die
        // Absicherung gegen den trotzdem feuernden Timer ist der
        // Runden-ID-Vergleich in handleAutoClose.
        if (round.allParticipantsPicked()) {
            Scheduler.ScheduledTask task = autoCloseTasks.remove(roomCode);
            if (task != null) {
                task.cancel();
            }
            room.closeCurrentRound();
        }
        broadcastState(roomCode, room);
    }

    private void handleCloseBet(String sessionId) {
        JoinedSession joined = resolveSession(sessionId);
        if (joined == null || !joined.room().isHost(joined.playerId())) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann das Fenster schließen."));
            return;
        }
        RoomCode roomCode = joined.roomCode();
        Room room = joined.room();
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
        broadcastState(roomCode, room);
    }

    private void handleAutoClose(RoomCode roomCode, RoundId roundId) {
        Room room = rooms.get(roomCode);
        if (room == null) {
            // Die Watchparty wurde inzwischen zurueckgesetzt oder aufgeraeumt.
            return;
        }
        Round round = room.getCurrentRound();
        if (round == null || round.getPhase() != Phase.OPEN || !round.getId().equals(roundId)) {
            // ADR-010: veralteter Timer einer bereits geschlossenen oder
            // schon wieder neuen Runde — ignorieren.
            return;
        }
        room.closeCurrentRound();
        broadcastState(roomCode, room);
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
        JoinedSession joined = resolveSession(sessionId);
        if (joined == null || !joined.room().isHost(joined.playerId())) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann eine Runde annullieren."));
            return;
        }
        RoomCode roomCode = joined.roomCode();
        Room room = joined.room();
        Round round = room.getCurrentRound();
        Phase phase = room.getPhase();
        if (round == null || (phase != Phase.OPEN && phase != Phase.CLOSED)) {
            clients.send(sessionId, new Messages.Error("Es läuft keine Runde, die sich annullieren ließe."));
            return;
        }

        Scheduler.ScheduledTask task = autoCloseTasks.remove(roomCode);
        if (task != null) {
            task.cancel();
        }
        room.annulCurrentRound();

        // RESOLVED erlaubt das Zurueckholen der Host-Rolle (ADR-021).
        reassignHost(room);

        log.info("Runde {} in Watchparty {} vom Host annulliert", round.getId(), roomCode);
        broadcastState(roomCode, room);
    }

    /**
     * Setzt eine einzelne Watchparty zurueck (ADR-023): Ohne den Neustart als
     * impliziten Reset braucht es einen expliziten. Anders als {@code ANNUL}
     * nicht auf eine Phase beschraenkt und nimmt auch die Spieler mit --
     * Testrunden vom Aufbau oder ein doppelt beigetretener Spieler sollen
     * verschwinden koennen, nicht nur der Punktestand.
     *
     * Seit ADR-033 ausdruecklich nur die eigene Watchparty (Anforderung
     * 1-i): eine andere Watchparty desselben Prozesses bleibt unberuehrt.
     *
     * Invariante 5 (Nullsumme) gilt innerhalb eines Spiels; RESET beendet
     * das Spiel, statt Punkte zu verschieben.
     */
    private void handleReset(String sessionId) {
        JoinedSession joined = resolveSession(sessionId);
        if (joined == null || !joined.room().isHost(joined.playerId())) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann den Raum zurücksetzen."));
            return;
        }
        RoomCode roomCode = joined.roomCode();

        Scheduler.ScheduledTask task = autoCloseTasks.remove(roomCode);
        if (task != null) {
            task.cancel();
        }
        Room freshRoom = new Room(roomCode);
        rooms.put(roomCode, freshRoom);

        log.info("Watchparty {} vom Host zurückgesetzt", roomCode);
        // Broadcast zuerst: der Client erkennt am fehlenden eigenen Spieler
        // im naechsten STATE, dass er zurueckgesetzt wurde, und faellt in
        // die Beitrittsansicht zurueck (kein automatisches Wiederbeitreten,
        // sonst waere RESET nur Anzeige). Erst danach die Bindung loesen --
        // sonst bekaeme niemand mehr genau dieses STATE zugestellt.
        broadcastState(roomCode, freshRoom);
        // Nur die Sitzungen dieser einen Watchparty, nicht global (1-i) --
        // sie bleiben offen, zeigen aber auf niemanden mehr.
        sessions.entrySet().removeIf(entry -> entry.getValue().roomCode().equals(roomCode));
    }

    private void handleResolve(String sessionId, @Nullable String rawWinningOutcomeId) {
        JoinedSession joined = resolveSession(sessionId);
        if (joined == null || !joined.room().isHost(joined.playerId())) {
            clients.send(sessionId, new Messages.Error("Nur der Host kann auflösen."));
            return;
        }
        RoomCode roomCode = joined.roomCode();
        Room room = joined.room();
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
        reassignHost(room);

        broadcastState(roomCode, room);
    }

    private void reassignHost(Room room) {
        Phase phase = room.getPhase();
        boolean allowPickup = phase == Phase.IDLE || phase == Phase.RESOLVED;
        room.reassignHostIfNeeded(allowPickup);
    }

    private void sendYourPickIfAny(String sessionId, Room room, PlayerId playerId) {
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

    private void broadcastState(RoomCode roomCode, Room room) {
        Messages.State state = RoomView.state(room, clock.instant().toEpochMilli());
        // Der Empfaengerkreis wird hier auf dem Raum-Thread festgelegt und als
        // Kopie uebergeben; der Adapter darf ihn nicht ueberdauern (Invariante 1).
        // Nur Sitzungen dieser einen Watchparty (Anforderung 1-i) -- eine
        // fremde Sitzung bekommt hier strukturell nichts zu sehen.
        List<String> recipients = sessions.entrySet().stream()
                .filter(entry -> entry.getValue().roomCode().equals(roomCode))
                .map(Map.Entry::getKey)
                .toList();
        clients.sendToAll(recipients, state);
        // "Zustand geaendert, aber nicht gespeichert" soll gar nicht erst
        // als Zustand existieren (ADR-023) -- deshalb an derselben Stelle
        // wie das Senden, nicht an einer eigenen. Dieselbe Marke haelt fest,
        // dass hier Aktivitaet war (Anforderung 1-j).
        snapshots.save(room.toSnapshot(clock.instant().toEpochMilli()));
        lastActivity.put(roomCode, clock.instant());
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
