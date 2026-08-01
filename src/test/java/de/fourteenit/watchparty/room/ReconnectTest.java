package de.fourteenit.watchparty.room;

import de.fourteenit.watchparty.ws.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reconnect gezielt in jeder Phase durchspielen (ADR-014). Das Handy mitten
 * im offenen Fenster zu sperren ist der Realfall, nicht die Ausnahme —
 * deshalb jede Phase einzeln. Ueber die WebSocket-Ebene laesst sich das
 * deterministisch nachstellen, indem dieselbe ClientSession disconnected und
 * eine neue mit demselben Token wieder verbunden wird (ADR-014).
 */
class ReconnectTest {

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
        return connect(name, null);
    }

    private ClientSession reconnect(String name, String token) {
        return connect(name, token);
    }

    private ClientSession connect(String name, String token) {
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
    void reconnectWaehrendIdleGibtDasselbeKontoZurueck() {
        ClientSession anna = join("Anna");
        String token = actor.getRoomForTest().byId(anna.getPlayerId()).getToken();
        String playerId = anna.getPlayerId();

        actor.disconnected(anna);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().byId(playerId).isConnected()).isFalse();

        ClientSession back = reconnect("Anna", token);
        assertThat(back.getPlayerId()).isEqualTo(playerId);
        assertThat(actor.getRoomForTest().byId(playerId).isConnected()).isTrue();
    }

    @Test
    void reconnectWaehrendOpenKannNochTippenWennNochNichtGetippt() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String token = actor.getRoomForTest().byId(anna.getPlayerId()).getToken();

        actor.openBet(host, null);
        actor.disconnected(anna);
        actor.awaitIdle();

        ClientSession back = reconnect("Anna", token);
        actor.placePick(back, "touchdown", null);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getCurrentRound().hasPick(back.getPlayerId())).isTrue();
    }

    @Test
    void reconnectWaehrendOpenErhaeltDenEigenenBereitsAbgegebenenTippErneut() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String annaId = anna.getPlayerId();
        String token = actor.getRoomForTest().byId(annaId).getToken();

        actor.openBet(host, null);
        actor.placePick(anna, "punt", 40);
        actor.disconnected(anna);
        actor.awaitIdle();

        // Reconnect mitten in OPEN: der Server kennt den Tipp weiterhin --
        // er geht separat als YOUR_PICK erneut an die neue Session (ADR-013).
        reconnect("Anna", token);
        Pick pick = actor.getRoomForTest().getCurrentRound().getPicks().get(annaId);
        assertThat(pick.outcomeId()).isEqualTo("punt");
        assertThat(pick.stake()).isEqualTo(40);
    }

    @Test
    void reconnectWaehrendClosedLaesstDieAufgedecktenTippsUnveraendert() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String token = actor.getRoomForTest().byId(anna.getPlayerId()).getToken();

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.placePick(anna, "punt", 50);
        actor.closeBet(host);
        actor.disconnected(anna);
        actor.awaitIdle();

        reconnect("Anna", token);

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
        assertThat(actor.getRoomForTest().getCurrentRound().getPicks()).hasSize(2);
    }

    @Test
    void reconnectWaehrendResolvedZeigtWeiterhinDasErgebnis() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String token = actor.getRoomForTest().byId(anna.getPlayerId()).getToken();

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.placePick(anna, "punt", 50);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.disconnected(anna);
        actor.awaitIdle();

        reconnect("Anna", token);

        Round round = actor.getRoomForTest().getCurrentRound();
        assertThat(round.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(round.getWinningOutcomeId()).isEqualTo("touchdown");
        assertThat(round.getDeltas()).isNotEmpty();
    }

    @Test
    void reconnectSetztDenVerpassteRundenZaehlerZurueck() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String annaId = anna.getPlayerId();
        String token = actor.getRoomForTest().byId(annaId).getToken();

        actor.disconnected(anna);
        actor.awaitIdle();

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", null);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().byId(annaId).getMissedRounds()).isEqualTo(1);

        reconnect("Anna", token);
        assertThat(actor.getRoomForTest().byId(annaId).getMissedRounds()).isZero();
        assertThat(actor.getRoomForTest().byId(annaId).isPaused()).isFalse();
    }
}
