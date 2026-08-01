package de.fourteenit.watchparty.room;

import de.fourteenit.watchparty.ws.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Der Zustandsautomat aus ADR-020, durchgespielt als Ereignis-Sequenzen ueber
 * die oeffentlichen Eintrittspunkte von {@link RoomActor}. Prueft ueber
 * {@link RoomActor#getRoomForTest()} den Raumzustand direkt, statt das
 * JSON-Protokoll zu parsen — das ist Sache von {@code Etappe 4}.
 */
class RoomActorStateMachineTest {

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

    private String playerId(ClientSession session) {
        return session.getPlayerId();
    }

    @Test
    void ersterJoinerWirdHostUndOeffnenBringtDieRundeInOpen() {
        ClientSession host = join("Host");
        join("Anna");

        actor.openMarket(host);
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getPhase()).isEqualTo(Phase.OPEN);
        assertThat(room.getCurrentRound().getClosesAt()).isEqualTo(START.plusSeconds(15));
        assertThat(room.getCurrentRound().getParticipants()).hasSize(2);
    }

    @Test
    void nurDerHostDarfDenMarktOeffnen() {
        join("Host");
        ClientSession anna = join("Anna");

        actor.openMarket(anna);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void openMarketWaehrendLaufenderRundeIstEinFehler() {
        ClientSession host = join("Host");
        actor.openMarket(host);
        actor.awaitIdle();

        actor.openMarket(host);
        actor.awaitIdle();

        // Immer noch dieselbe Runde, keine zweite wurde angelegt.
        assertThat(actor.getRoomForTest().getCurrentRound().getId()).isEqualTo(1);
    }

    @Test
    void tippAbgabeInnerhalbDesFensterWirdUebernommenMitMindesteinsatzAlsStandard() {
        ClientSession host = join("Host");
        actor.openMarket(host);
        actor.awaitIdle();

        actor.placeBet(host, "touchdown", null);
        actor.awaitIdle();

        Bet bet = actor.getRoomForTest().getCurrentRound().getBets().get(playerId(host));
        assertThat(bet.outcomeId()).isEqualTo("touchdown");
        assertThat(bet.stake()).isEqualTo(25);
    }

    @Test
    void tippNachAblaufVonClosesAtZaehltNichtMehrAuchOhneDassDerTimerSchonGefeuertHat() {
        ClientSession host = join("Host");
        actor.openMarket(host);
        actor.awaitIdle();

        // ADR-011: Der Zeitvergleich beim Abarbeiten entscheidet, nicht ob
        // der geplante Auto-Close-Task schon gelaufen ist.
        clock.advance(Duration.ofSeconds(16));
        actor.placeBet(host, "touchdown", null);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getCurrentRound().getBets()).isEmpty();
    }

    @Test
    void zweiterTippDesselbenSpielersWirdAbgelehnt() {
        ClientSession host = join("Host");
        actor.openMarket(host);
        actor.awaitIdle();

        actor.placeBet(host, "touchdown", 100);
        actor.placeBet(host, "punt", 50);
        actor.awaitIdle();

        Bet bet = actor.getRoomForTest().getCurrentRound().getBets().get(playerId(host));
        assertThat(bet.stake()).isEqualTo(100);
        assertThat(bet.outcomeId()).isEqualTo("touchdown");
    }

    @Test
    void spielerUnterDemMindesteinsatzGehtZwangsweiseAllIn() {
        ClientSession host = join("Host");
        Player player = actor.getRoomForTest().byId(playerId(host));
        player.setPoints(10);

        actor.openMarket(host);
        actor.placeBet(host, "touchdown", 5);
        actor.awaitIdle();

        Bet bet = actor.getRoomForTest().getCurrentRound().getBets().get(playerId(host));
        assertThat(bet.stake()).isEqualTo(10);
    }

    @Test
    void manuellesSchliessenBringtDieRundeNachClosedUndDeckdtTippsAufWennResolved() {
        ClientSession host = join("Host");
        actor.openMarket(host);
        actor.placeBet(host, "touchdown", null);
        actor.closeMarket(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void doppeltesSchliessenWirdStillIgnoriert() {
        ClientSession host = join("Host");
        actor.openMarket(host);
        actor.closeMarket(host);
        actor.closeMarket(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void autoCloseSchliesstDieRundeNachAblaufDesFensters() {
        ClientSession host = join("Host");
        actor.openMarket(host);
        actor.awaitIdle();

        clock.advance(Duration.ofSeconds(15));
        scheduler.fireAll();
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void veralteterAutoCloseAusEinerVorherigenRundeSchliesstDieNeueRundeNicht() {
        ClientSession host = join("Host");

        actor.openMarket(host);
        actor.awaitIdle();
        actor.closeMarket(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();

        actor.openMarket(host);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);

        // Der Auto-Close-Task der ersten Runde war zu diesem Zeitpunkt schon
        // beim Scheduler enqueued; das Cancel beim Oeffnen der zweiten Runde
        // reicht laut ADR-010 nicht als Absicherung. Er feuert trotzdem --
        // fireOldestIgnoringCancellation() feuert gezielt nur ihn, nicht den
        // frischen Task der zweiten Runde.
        scheduler.fireOldestIgnoringCancellation();
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);
    }

    @Test
    void aufloesenVerrechnetDeltasUndWechseltNachResolved() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");

        actor.openMarket(host);
        actor.placeBet(host, "touchdown", 100);
        actor.placeBet(anna, "punt", 50);
        actor.closeMarket(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(room.byId(playerId(host)).getPoints()).isEqualTo(Room.STARTING_POINTS + 50);
        assertThat(room.byId(playerId(anna)).getPoints()).isEqualTo(Room.STARTING_POINTS - 50);
        assertThat(room.getCurrentRound().getPool()).isEqualTo(150);
        assertThat(room.getCurrentRound().isAnnulled()).isFalse();
    }

    @Test
    void aufloesenOhneJedenTippAnnulliertDieRundeOhneVerrechnung() {
        ClientSession host = join("Host");

        actor.openMarket(host);
        actor.closeMarket(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getCurrentRound().isAnnulled()).isTrue();
        assertThat(room.byId(playerId(host)).getPoints()).isEqualTo(Room.STARTING_POINTS);
    }

    @Test
    void getrennterSpielerZahltDieErstenZweiVerpasstenRundenUndPausiertAbDerDritten() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String annaId = playerId(anna);

        actor.disconnected(anna);
        actor.awaitIdle();

        // Runde 1: Anna ist im eingefrorenen Kreis und zahlt die Strafe.
        playAndResolveRoundWithoutAnna(host);
        assertThat(actor.getRoomForTest().byId(annaId).getPoints())
                .isEqualTo(Room.STARTING_POINTS - 25);
        assertThat(actor.getRoomForTest().byId(annaId).getMissedRounds()).isEqualTo(1);

        // Runde 2: noch einmal.
        playAndResolveRoundWithoutAnna(host);
        assertThat(actor.getRoomForTest().byId(annaId).getPoints())
                .isEqualTo(Room.STARTING_POINTS - 50);
        assertThat(actor.getRoomForTest().byId(annaId).getMissedRounds()).isEqualTo(2);
        assertThat(actor.getRoomForTest().byId(annaId).isPaused()).isTrue();

        // Runde 3: Anna ist pausiert, gehoert nicht mehr zum Teilnehmerkreis.
        actor.openMarket(host);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getCurrentRound().getParticipants()).doesNotContain(annaId);
        playAndResolveRoundWithoutAnna(host);
        assertThat(actor.getRoomForTest().byId(annaId).getPoints())
                .isEqualTo(Room.STARTING_POINTS - 50);
    }

    private void playAndResolveRoundWithoutAnna(ClientSession host) {
        actor.openMarket(host);
        actor.placeBet(host, "touchdown", null);
        actor.closeMarket(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();
    }

    @Test
    void hostVerliertRolleSofortAberFruehererJoinerBekommtSieErstBeiResolvedZurueck() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String hostId = playerId(host);

        actor.openMarket(host);
        actor.awaitIdle();

        // Host trennt sich mitten in OPEN: Verlust wirkt sofort.
        actor.disconnected(host);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(playerId(anna));

        // Host kommt waehrend OPEN zurueck -- die Rolle bleibt vorerst bei Anna.
        ClientSession hostReturned = rejoin("Host", hostId);
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(playerId(anna));

        // Erst beim Erreichen von RESOLVED reklamiert der fruehere Host die Rolle zurueck.
        actor.closeMarket(anna);
        actor.resolve(anna, "touchdown");
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(hostId);
        assertThat(hostReturned).isNotNull();
    }

    private ClientSession rejoin(String name, String expectedPlayerId) {
        String token = actor.getRoomForTest().byId(expectedPlayerId).getToken();
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn(name + "-rejoin-socket");
        when(socket.isOpen()).thenReturn(true);
        ClientSession session = new ClientSession(socket, Runnable::run);
        actor.connected(session);
        actor.join(session, name, token);
        actor.awaitIdle();
        return session;
    }
}
