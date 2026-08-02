package de.fourteen.watchparty.application;

import de.fourteen.watchparty.adapter.out.file.SnapshotStore;
import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Phase;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PlayerName;
import de.fourteen.watchparty.domain.model.Token;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Player;
import de.fourteen.watchparty.domain.model.Room;
import de.fourteen.watchparty.domain.model.Round;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jeder Sonderfall aus Abschnitt 6 des Snapshot-Plans (ADR-023), jeweils mit
 * dem Nachweis, dass danach weitergespielt werden kann. Deckt zusammen mit
 * {@code SnapshotTest} (Datenmodell) und {@code SnapshotStoreTest} (I/O) den
 * gesamten Weg Room -> Datei -> neuer RoomActor -> Room ab.
 */
class RestoreTest {

    private static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    private final RecordingClientGateway gateway = new RecordingClientGateway();

    /** Sitzungs-IDs muessen nur eindeutig sein; frueher lieferte sie der Mock. */
    private int sessionCounter;

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

    /**
     * Jeder in diesem Test gestartete Actor samt Store, damit beide am Ende
     * wieder anhalten. Ohne das schreibt der {@code snapshot-writer}-Thread
     * noch, waehrend JUnit das {@code @TempDir} schon loescht — der Test war
     * dadurch sporadisch rot ("Failed to delete temp directory"), und zwar
     * unabhaengig von dem, was er eigentlich prueft.
     */
    private final List<Running> gestartet = new ArrayList<>();

    @BeforeEach
    void setUp(@TempDir Path dir) {
        snapshotFile = dir.resolve("room.json");
        clock = new FakeClock(START);
        scheduler = new FakeScheduler();
    }

    @AfterEach
    void tearDown() {
        for (Running running : gestartet) {
            running.actor().shutdown();
            running.store().shutdown();
        }
        gestartet.clear();
    }

    private Running start(Scheduler withScheduler) {
        SnapshotStore store = new SnapshotStore(snapshotFile);
        RoomActor actor = new RoomActor(clock, withScheduler, store, gateway);
        actor.loadOnStartup();
        actor.awaitIdle();
        Running running = new Running(actor, store);
        gestartet.add(running);
        return running;
    }

    private Running start() {
        return start(scheduler);
    }

    private String connect(RoomActor actor, String name, String token) {
        String sessionId = name + "-" + (++sessionCounter);
        actor.connected(sessionId);
        actor.join(sessionId, name, token);
        actor.awaitIdle();
        return sessionId;
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
        String host = connect(first.actor(), "Host", null);
        String token = first.actor().getRoomForTest().byId(gateway.playerIdOf(host)).getToken().value();
        connect(first.actor(), "Anna", null);

        // Punktestand ueber eine echte, aufgeloeste Runde veraendert -- nicht
        // per Setter: Player.setPoints ist seit den Value Objects
        // paket-privat, Mutation laeuft ueber das Aggregat.
        first.actor().openBet(host, null);
        first.actor().placePick(host, "touchdown", 234);
        first.actor().closeBet(host);
        first.actor().resolve(host, "touchdown");
        first.settle();
        Points erwartetePunkte = first.actor().getRoomForTest().byId(gateway.playerIdOf(host)).getPoints();

        Running restarted = start();

        Player restoredHost = restarted.actor().getRoomForTest().byToken(Token.of(token));
        assertThat(restoredHost).isNotNull();
        assertThat(restoredHost.getName()).isEqualTo(PlayerName.of("Host"));
        assertThat(restoredHost.isConnected()).isFalse();
        assertThat(restoredHost.getPoints()).isEqualTo(erwartetePunkte);
        assertThat(restarted.actor().getRoomForTest().players()).hasSize(2);

        // Weiterspielen: derselbe Token reconnectet auf denselben Spieler.
        String reconnected = connect(restarted.actor(), "Host", token);
        assertThat(gateway.playerIdOf(reconnected)).isEqualTo(restoredHost.getId());
        assertThat(restarted.actor().getRoomForTest().byId(gateway.playerIdOf(reconnected)).isConnected()).isTrue();
    }

    @Test
    void wiederherstellungMitOffenerRundeInDerZukunftBleibtOffenUndPlantAutoCloseNeu() {
        Running first = start();
        String host = connect(first.actor(), "Host", null);
        String hostToken = first.actor().getRoomForTest().byId(gateway.playerIdOf(host)).getToken().value();
        first.actor().openBet(host, null);
        first.settle();

        // Neustart nach 5 von 15 Sekunden -- das Fenster ist noch offen.
        clock.advance(Duration.ofSeconds(5));
        FakeScheduler newScheduler = new FakeScheduler();
        Running restarted = start(newScheduler);

        assertThat(restarted.actor().getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);
        assertThat(newScheduler.pendingCount()).isEqualTo(1);

        // Weiterspielen: noch offen, ein Tipp wird angenommen.
        String reconnectedHost = connect(restarted.actor(), "Host", hostToken);
        restarted.actor().placePick(reconnectedHost, "touchdown", 100);
        restarted.actor().awaitIdle();
        assertThat(restarted.actor().getRoomForTest().getCurrentRound().hasPick(gateway.playerIdOf(reconnectedHost)))
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
        String host = connect(first.actor(), "Host", null);
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
        String host = connect(first.actor(), "Host", null);
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
        String host = connect(first.actor(), "Host", null);
        String anna = connect(first.actor(), "Anna", null);
        first.actor().openBet(host, null);
        first.actor().placePick(host, "touchdown", 100);
        first.actor().placePick(anna, "punt", 50);
        first.actor().closeBet(host);
        first.actor().resolve(host, "touchdown");
        first.settle();

        Running restarted = start();

        Round round = restarted.actor().getRoomForTest().getCurrentRound();
        assertThat(round.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(round.getWinningOutcomeId()).isEqualTo(OutcomeId.of("touchdown"));
        assertThat(round.getDeltas()).isNotEmpty();
        assertThat(restarted.actor().getRoomForTest().byId(gateway.playerIdOf(host)).getPoints())
                .isEqualTo(Points.of(Room.STARTING_POINTS.value() + 50));
    }

    @Test
    void wiederherstellungMitUnbekannterWetteVerwirftNurDieRunde() throws Exception {
        Running first = start();
        String host = connect(first.actor(), "Host", null);
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
        assertThat(restarted.actor().getRoomForTest().byId(gateway.playerIdOf(host))).isNotNull();
    }

    @Test
    void wiederherstellungMitHostErlaubtDasZurueckholenNachResolved() {
        Running first = start();
        String host = connect(first.actor(), "Host", null);
        PlayerId hostId = gateway.playerIdOf(host);
        String hostToken = first.actor().getRoomForTest().byId(hostId).getToken().value();
        connect(first.actor(), "Anna", null);
        first.settle();

        Running restarted = start();
        assertThat(restarted.actor().getRoomForTest().getHostPlayerId()).isEqualTo(hostId);

        // Beim ersten JOIN nach dem Neustart ist der eigentliche Host noch
        // nicht wieder da (getrennt) -- die Rolle wandert an den ersten
        // Verbundenen (ADR-021), kein neuer Code dafuer noetig.
        String anna = connect(restarted.actor(), "Anna", null);
        assertThat(restarted.actor().getRoomForTest().getHostPlayerId()).isNotEqualTo(hostId);

        String hostReturned = connect(restarted.actor(), "Host", hostToken);
        assertThat(restarted.actor().getRoomForTest().getHostPlayerId()).isEqualTo(hostId);
        assertThat(gateway.playerIdOf(hostReturned)).isEqualTo(hostId);
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
        RoomActor actor = new RoomActor(clock, scheduler, new SnapshotStore(null), gateway);
        actor.loadOnStartup();
        connect(actor, "Host", null);
        actor.awaitIdle();

        assertThat(Files.exists(snapshotFile)).isFalse();
    }
}
