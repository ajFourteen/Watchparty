package de.fourteen.watchparty.application;

import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Phase;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.Round;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reconnect gezielt in jeder Phase durchspielen (ADR-014). Das Handy mitten
 * im offenen Fenster zu sperren ist der Realfall, nicht die Ausnahme —
 * deshalb jede Phase einzeln. Ueber die WebSocket-Ebene laesst sich das
 * deterministisch nachstellen, indem dieselbe ClientSession disconnected und
 * eine neue mit demselben Token wieder verbunden wird (ADR-014).
 */
class ReconnectTest {

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
        return connect(name, null);
    }

    private String reconnect(String name, String token) {
        return connect(name, token);
    }

    private String connect(String name, String token) {
        String sessionId = name + "-" + (++sessionCounter);
        actor.connected(sessionId);
        actor.join(sessionId, name, token);
        actor.awaitIdle();
        return sessionId;
    }

    @Test
    void reconnectWaehrendIdleGibtDasselbeKontoZurueck() {
        String anna = join("Anna");
        String token = actor.getRoomForTest().byId(gateway.playerIdOf(anna)).getToken().value();
        PlayerId playerId = gateway.playerIdOf(anna);

        actor.disconnected(anna);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().byId(playerId).isConnected()).isFalse();

        String back = reconnect("Anna", token);
        assertThat(gateway.playerIdOf(back)).isEqualTo(playerId);
        assertThat(actor.getRoomForTest().byId(playerId).isConnected()).isTrue();
    }

    @Test
    void reconnectWaehrendOpenKannNochTippenWennNochNichtGetippt() {
        String host = join("Host");
        String anna = join("Anna");
        String token = actor.getRoomForTest().byId(gateway.playerIdOf(anna)).getToken().value();

        actor.openBet(host, null);
        actor.disconnected(anna);
        actor.awaitIdle();

        String back = reconnect("Anna", token);
        actor.placePick(back, "touchdown", null);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getCurrentRound().hasPick(gateway.playerIdOf(back))).isTrue();
    }

    @Test
    void reconnectWaehrendOpenErhaeltDenEigenenBereitsAbgegebenenTippErneut() {
        String host = join("Host");
        String anna = join("Anna");
        PlayerId annaId = gateway.playerIdOf(anna);
        String token = actor.getRoomForTest().byId(annaId).getToken().value();

        actor.openBet(host, null);
        actor.placePick(anna, "punt", 40);
        actor.disconnected(anna);
        actor.awaitIdle();

        // Reconnect mitten in OPEN: der Server kennt den Tipp weiterhin --
        // er geht separat als YOUR_PICK erneut an die neue Session (ADR-013).
        reconnect("Anna", token);
        Pick pick = actor.getRoomForTest().getCurrentRound().getPicks().get(annaId);
        assertThat(pick.outcomeId()).isEqualTo(OutcomeId.of("punt"));
        assertThat(pick.stake()).isEqualTo(Points.of(40));
    }

    @Test
    void reconnectWaehrendClosedLaesstDieAufgedecktenTippsUnveraendert() {
        String host = join("Host");
        String anna = join("Anna");
        String token = actor.getRoomForTest().byId(gateway.playerIdOf(anna)).getToken().value();

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
        String host = join("Host");
        String anna = join("Anna");
        String token = actor.getRoomForTest().byId(gateway.playerIdOf(anna)).getToken().value();

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
        assertThat(round.getWinningOutcomeId()).isEqualTo(OutcomeId.of("touchdown"));
        assertThat(round.getDeltas()).isNotEmpty();
    }

    @Test
    void reconnectSetztDenVerpassteRundenZaehlerZurueck() {
        String host = join("Host");
        String anna = join("Anna");
        PlayerId annaId = gateway.playerIdOf(anna);
        String token = actor.getRoomForTest().byId(annaId).getToken().value();

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
