package de.fourteen.watchparty.room;

import de.fourteen.watchparty.ws.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code RESET} (Abschnitt 12 des Snapshot-Plans, ADR-023): Ohne den
 * Neustart als impliziten Reset braucht der Host einen expliziten, der
 * anders als {@code ANNUL} auch die Spieler mitnimmt.
 */
class RoomActorResetTest {

    private static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    private FakeClock clock;
    private FakeScheduler scheduler;
    private RoomActor actor;

    @BeforeEach
    void setUp() {
        clock = new FakeClock(START);
        scheduler = new FakeScheduler();
        actor = new RoomActor(clock, scheduler);
    }

    private ClientSession join(String name) {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn(name + "-socket");
        when(socket.isOpen()).thenReturn(true);
        ClientSession session = new ClientSession(socket, Runnable::run);
        actor.connected(session);
        actor.join(session, name, null);
        actor.awaitIdle();
        return session;
    }

    @Test
    void nurDerHostDarfZuruecksetzen() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");

        actor.reset(anna);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().players()).hasSize(2);
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(host.getPlayerId());
    }

    @Test
    void resetRaeumtSpielerPunkteUndLaufendeRundeVollstaendigWeg() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.awaitIdle();

        actor.reset(host);
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.players()).isEmpty();
        assertThat(room.getPhase()).isEqualTo(Phase.IDLE);
        assertThat(room.getHostPlayerId()).isNull();
        assertThat(room.byId(host.getPlayerId())).isNull();
        assertThat(room.byId(anna.getPlayerId())).isNull();
    }

    @Test
    void resetFunktioniertAuchMittenInEinerOffenenRunde() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);

        actor.reset(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void resetBrichtDenAutoCloseTaskDerAltenRundeAb() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();
        assertThat(scheduler.pendingCount()).isEqualTo(1);

        actor.reset(host);
        actor.awaitIdle();

        assertThat(scheduler.pendingCount()).isZero();
    }

    @Test
    void nachResetZeigenAlleSessionsAufKeinenSpielerMehr() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");

        actor.reset(host);
        actor.awaitIdle();

        assertThat(host.getPlayerId()).isNull();
        assertThat(anna.getPlayerId()).isNull();
    }

    @Test
    void erstJoinerNachResetWirdDerNeueHost() {
        ClientSession host = join("Host");
        join("Anna");
        actor.reset(host);
        actor.awaitIdle();

        ClientSession erstesNachDemReset = join("Charlie");

        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(erstesNachDemReset.getPlayerId());
    }
}
