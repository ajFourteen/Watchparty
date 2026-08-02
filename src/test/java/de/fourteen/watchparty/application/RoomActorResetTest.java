package de.fourteen.watchparty.application;

import de.fourteen.watchparty.domain.model.Phase;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Room;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code RESET} (Abschnitt 12 des Snapshot-Plans, ADR-023): Ohne den
 * Neustart als impliziten Reset braucht der Host einen expliziten, der
 * anders als {@code ANNUL} auch die Spieler mitnimmt.
 */
class RoomActorResetTest {

    private static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    private final RecordingClientGateway gateway = new RecordingClientGateway();

    /** Sitzungs-IDs muessen nur eindeutig sein; frueher lieferte sie der Mock. */
    private int sessionCounter;

    private FakeClock clock;
    private FakeScheduler scheduler;
    private RoomActor actor;

    @BeforeEach
    void setUp() {
        clock = new FakeClock(START);
        scheduler = new FakeScheduler();
        actor = new RoomActor(clock, scheduler, new NoSnapshots(), gateway);
    }

    private String join(String name) {
        String sessionId = name + "-socket";
        actor.connected(sessionId);
        actor.join(sessionId, name, null);
        actor.awaitIdle();
        return sessionId;
    }

    @Test
    void nurDerHostDarfZuruecksetzen() {
        String host = join("Host");
        String anna = join("Anna");

        actor.reset(anna);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().players()).hasSize(2);
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(gateway.playerIdOf(host));
    }

    @Test
    void resetRaeumtSpielerPunkteUndLaufendeRundeVollstaendigWeg() {
        String host = join("Host");
        String anna = join("Anna");
        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.awaitIdle();

        actor.reset(host);
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.players()).isEmpty();
        assertThat(room.getPhase()).isEqualTo(Phase.IDLE);
        assertThat(room.getHostPlayerId()).isNull();
        assertThat(room.byId(gateway.playerIdOf(host))).isNull();
        assertThat(room.byId(gateway.playerIdOf(anna))).isNull();
    }

    @Test
    void resetFunktioniertAuchMittenInEinerOffenenRunde() {
        String host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);

        actor.reset(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void resetBrichtDenAutoCloseTaskDerAltenRundeAb() {
        String host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();
        assertThat(scheduler.pendingCount()).isEqualTo(1);

        actor.reset(host);
        actor.awaitIdle();

        assertThat(scheduler.pendingCount()).isZero();
    }

    /**
     * Die Sitzungen bleiben offen, zeigen aber auf niemanden mehr. Geprueft
     * wird das am beobachtbaren Verhalten statt am Innenleben: Der naechste
     * Tipp aus einer Alt-Sitzung muss auf die Beitrittsansicht verweisen,
     * und im Zustand steht kein Spieler mehr.
     */
    @Test
    void nachResetZeigenAlleSessionsAufKeinenSpielerMehr() {
        String host = join("Host");
        String anna = join("Anna");

        actor.reset(host);
        actor.awaitIdle();

        assertThat(gateway.lastStateFor(anna).players()).isEmpty();

        actor.placePick(anna, "touchdown", 25);
        actor.awaitIdle();
        assertThat(gateway.errorsFor(anna)).contains("Bitte zuerst beitreten.");
        assertThat(gateway.errorsFor(host)).isEmpty();
    }

    @Test
    void erstJoinerNachResetWirdDerNeueHost() {
        String host = join("Host");
        join("Anna");
        actor.reset(host);
        actor.awaitIdle();

        String erstesNachDemReset = join("Charlie");

        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(gateway.playerIdOf(erstesNachDemReset));
    }
}
