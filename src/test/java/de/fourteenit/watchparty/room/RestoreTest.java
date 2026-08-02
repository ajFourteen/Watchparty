package de.fourteenit.watchparty.room;

import de.fourteenit.watchparty.ws.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.socket.WebSocketSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Jeder Sonderfall aus Abschnitt 6 des Snapshot-Plans (ADR-023), jeweils mit
 * dem Nachweis, dass danach weitergespielt werden kann. Deckt zusammen mit
 * {@link SnapshotTest} (Datenmodell) und {@link SnapshotStoreTest} (I/O) den
 * gesamten Weg Room -> Datei -> neuer RoomActor -> Room ab.
 */
class RestoreTest {

    private static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    private Path snapshotFile;
    private FakeClock clock;
    private FakeScheduler scheduler;

    /** Ein Actor samt seinem SnapshotStore, damit Tests auf den Schreibvorgang warten koennen. */
    private record Running(RoomActor actor, SnapshotStore store) {
        void settle() {
            actor.awaitIdle();
            store.awaitWritten();
        }
    }

    @BeforeEach
    void setUp(@TempDir Path dir) {
        snapshotFile = dir.resolve("room.json");
        clock = new FakeClock(START);
        scheduler = new FakeScheduler();
    }

    private Running start(Scheduler withScheduler) {
        SnapshotStore store = new SnapshotStore(snapshotFile);
        RoomActor actor = new RoomActor(clock, withScheduler, store);
        actor.loadOnStartup();
        actor.awaitIdle();
        return new Running(actor, store);
    }

    private Running start() {
        return start(scheduler);
    }

    private ClientSession connect(RoomActor actor, String name, String token) {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn(name + "-" + System.identityHashCode(socket));
        when(socket.isOpen()).thenReturn(true);
        ClientSession session = new ClientSession(socket, Runnable::run);
        actor.connected(session);
        actor.join(session, name, token);
        actor.awaitIdle();
        return session;
    }

    @Test
    void fehlendeDateiStartetLeerenRaum() {
        Running restored = start();

        assertThat(restored.actor().getRoomForTest().players()).isEmpty();
        assertThat(restored.actor().getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void wiederherstellungInIdleErhaeltPunkteNamenUndToken() {
        Running first = start();
        ClientSession host = connect(first.actor(), "Host", null);
        String token = first.actor().getRoomForTest().byId(host.getPlayerId()).getToken();
        first.actor().getRoomForTest().byId(host.getPlayerId()).setPoints(1234);
        // Punktestand direkt geaendert, ohne dass eine Runde lief -- die
        // naechste STATE-aendernde Aktion persistiert ihn erst.
        connect(first.actor(), "Anna", null);
        first.settle();

        Running restarted = start();

        Player restoredHost = restarted.actor().getRoomForTest().byToken(token);
        assertThat(restoredHost).isNotNull();
        assertThat(restoredHost.getName()).isEqualTo("Host");
        assertThat(restoredHost.isConnected()).isFalse();
        assertThat(restarted.actor().getRoomForTest().players()).hasSize(2);

        // Weiterspielen: derselbe Token reconnectet auf denselben Spieler.
        ClientSession reconnected = connect(restarted.actor(), "Host", token);
        assertThat(reconnected.getPlayerId()).isEqualTo(restoredHost.getId());
        assertThat(restarted.actor().getRoomForTest().byId(reconnected.getPlayerId()).isConnected()).isTrue();
    }

    @Test
    void wiederherstellungMitOffenerRundeInDerZukunftBleibtOffenUndPlantAutoCloseNeu() {
        Running first = start();
        ClientSession host = connect(first.actor(), "Host", null);
        String hostToken = first.actor().getRoomForTest().byId(host.getPlayerId()).getToken();
        first.actor().openBet(host, null);
        first.settle();

        // Neustart nach 5 von 15 Sekunden -- das Fenster ist noch offen.
        clock.advance(Duration.ofSeconds(5));
        FakeScheduler newScheduler = new FakeScheduler();
        Running restarted = start(newScheduler);

        assertThat(restarted.actor().getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);
        assertThat(newScheduler.pendingCount()).isEqualTo(1);

        // Weiterspielen: noch offen, ein Tipp wird angenommen.
        ClientSession reconnectedHost = connect(restarted.actor(), "Host", hostToken);
        restarted.actor().placePick(reconnectedHost, "touchdown", 100);
        restarted.actor().awaitIdle();
        assertThat(restarted.actor().getRoomForTest().getCurrentRound().hasPick(reconnectedHost.getPlayerId()))
                .isTrue();

        // Die neu eingeplante Restzeit schliesst zum urspruenglichen Zeitpunkt.
        clock.advance(Duration.ofSeconds(10));
        newScheduler.fireAll();
        restarted.actor().awaitIdle();
        assertThat(restarted.actor().getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void wiederherstellungMitAbgelaufenerOffenerRundeSchliesstSieOhneSonderfall() {
        Running first = start();
        ClientSession host = connect(first.actor(), "Host", null);
        first.actor().openBet(host, null);
        first.actor().placePick(host, "touchdown", 100);
        first.settle();

        // Neustart nach 20 von 15 Sekunden -- das Fenster ist bereits abgelaufen.
        clock.advance(Duration.ofSeconds(20));
        Running restarted = start();

        assertThat(restarted.actor().getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
        assertThat(restarted.actor().getRoomForTest().getCurrentRound().getPicks()).hasSize(1);
    }

    @Test
    void wiederherstellungMitGeschlossenerRundeBleibtUnveraendert() {
        Running first = start();
        ClientSession host = connect(first.actor(), "Host", null);
        first.actor().openBet(host, null);
        first.actor().placePick(host, "touchdown", 100);
        first.actor().closeBet(host);
        first.settle();

        Running restarted = start();

        assertThat(restarted.actor().getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
        assertThat(restarted.actor().getRoomForTest().getCurrentRound().getPicks()).hasSize(1);
    }

    @Test
    void wiederherstellungMitAufgeloesterRundeZeigtWeiterhinDasErgebnis() {
        Running first = start();
        ClientSession host = connect(first.actor(), "Host", null);
        ClientSession anna = connect(first.actor(), "Anna", null);
        first.actor().openBet(host, null);
        first.actor().placePick(host, "touchdown", 100);
        first.actor().placePick(anna, "punt", 50);
        first.actor().closeBet(host);
        first.actor().resolve(host, "touchdown");
        first.settle();

        Running restarted = start();

        Round round = restarted.actor().getRoomForTest().getCurrentRound();
        assertThat(round.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(round.getWinningOutcomeId()).isEqualTo("touchdown");
        assertThat(round.getDeltas()).isNotEmpty();
        assertThat(restarted.actor().getRoomForTest().byId(host.getPlayerId()).getPoints())
                .isEqualTo(Room.STARTING_POINTS + 50);
    }

    @Test
    void wiederherstellungMitUnbekannterWetteVerwirftNurDieRunde() throws Exception {
        Running first = start();
        ClientSession host = connect(first.actor(), "Host", null);
        first.actor().openBet(host, null);
        first.settle();

        // Der Katalog hat sich zwischen den Versionen geaendert: dieselbe
        // Datei manuell so umschreiben, als kaeme sie von einer alten
        // Version mit einer inzwischen entfernten Wette.
        String kaputt = Files.readString(snapshotFile)
                .replace("\"drive-outcome\"", "\"es-gibt-diese-wette-nicht-mehr\"");
        Files.writeString(snapshotFile, kaputt);

        Running restarted = start();

        assertThat(restarted.actor().getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
        assertThat(restarted.actor().getRoomForTest().players()).hasSize(1);
        assertThat(restarted.actor().getRoomForTest().byId(host.getPlayerId())).isNotNull();
    }

    @Test
    void wiederherstellungMitHostErlaubtDasZurueckholenNachResolved() {
        Running first = start();
        ClientSession host = connect(first.actor(), "Host", null);
        String hostId = host.getPlayerId();
        String hostToken = first.actor().getRoomForTest().byId(hostId).getToken();
        connect(first.actor(), "Anna", null);
        first.settle();

        Running restarted = start();
        assertThat(restarted.actor().getRoomForTest().getHostPlayerId()).isEqualTo(hostId);

        // Beim ersten JOIN nach dem Neustart ist der eigentliche Host noch
        // nicht wieder da (getrennt) -- die Rolle wandert an den ersten
        // Verbundenen (ADR-021), kein neuer Code dafuer noetig.
        ClientSession anna = connect(restarted.actor(), "Anna", null);
        assertThat(restarted.actor().getRoomForTest().getHostPlayerId()).isNotEqualTo(hostId);

        ClientSession hostReturned = connect(restarted.actor(), "Host", hostToken);
        assertThat(restarted.actor().getRoomForTest().getHostPlayerId()).isEqualTo(hostId);
        assertThat(hostReturned.getPlayerId()).isEqualTo(hostId);
        assertThat(anna).isNotNull();
    }

    @Test
    void abgelaufenerSnapshotStartetLeerenRaumTrotzVorhandenerDatei() {
        Running first = start();
        connect(first.actor(), "Host", null);
        first.settle();

        clock.advance(Duration.ofHours(6).plusSeconds(1));
        Running restarted = start();

        assertThat(restarted.actor().getRoomForTest().players()).isEmpty();
    }

    @Test
    void kaputteDateiStartetLeerenRaumStattDenStartZuZerschiessen() throws Exception {
        Files.writeString(snapshotFile, "{ das ist kein json");

        Running restarted = start();

        assertThat(restarted.actor().getRoomForTest().players()).isEmpty();
        assertThat(restarted.actor().getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void deaktiviertePersistenzLaedtNieUndSchreibtNie() {
        RoomActor actor = new RoomActor(clock, scheduler, new SnapshotStore(null));
        actor.loadOnStartup();
        connect(actor, "Host", null);
        actor.awaitIdle();

        assertThat(Files.exists(snapshotFile)).isFalse();
    }
}
