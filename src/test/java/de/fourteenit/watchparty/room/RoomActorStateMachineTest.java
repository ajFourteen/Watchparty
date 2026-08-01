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
 * JSON-Protokoll zu parsen — das prueft der Rauchtest ueber die Leitung.
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

        actor.openBet(host, null);
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getPhase()).isEqualTo(Phase.OPEN);
        assertThat(room.getCurrentRound().getClosesAt()).isEqualTo(START.plusSeconds(15));
        assertThat(room.getCurrentRound().getParticipants()).hasSize(2);
    }

    @Test
    void nurDerHostDarfEineWetteOeffnen() {
        join("Host");
        ClientSession anna = join("Anna");

        actor.openBet(anna, null);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void openBetWaehrendLaufenderRundeIstEinFehler() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        actor.openBet(host, null);
        actor.awaitIdle();

        // Immer noch dieselbe Runde, keine zweite wurde angelegt.
        assertThat(actor.getRoomForTest().getCurrentRound().getId()).isEqualTo(1);
    }

    @Test
    void tippAbgabeInnerhalbDesFensterWirdUebernommenMitMindesteinsatzAlsStandard() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        actor.placePick(host, "touchdown", null);
        actor.awaitIdle();

        Pick pick = actor.getRoomForTest().getCurrentRound().getPicks().get(playerId(host));
        assertThat(pick.outcomeId()).isEqualTo("touchdown");
        assertThat(pick.stake()).isEqualTo(25);
    }

    @Test
    void tippNachAblaufVonClosesAtZaehltNichtMehrAuchOhneDassDerTimerSchonGefeuertHat() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        // ADR-011: Der Zeitvergleich beim Abarbeiten entscheidet, nicht ob
        // der geplante Auto-Close-Task schon gelaufen ist.
        clock.advance(Duration.ofSeconds(16));
        actor.placePick(host, "touchdown", null);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getCurrentRound().getPicks()).isEmpty();
    }

    @Test
    void zweiterTippDesselbenSpielersWirdAbgelehnt() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        actor.placePick(host, "touchdown", 100);
        actor.placePick(host, "punt", 50);
        actor.awaitIdle();

        Pick pick = actor.getRoomForTest().getCurrentRound().getPicks().get(playerId(host));
        assertThat(pick.stake()).isEqualTo(100);
        assertThat(pick.outcomeId()).isEqualTo("touchdown");
    }

    @Test
    void spielerUnterDemMindesteinsatzGehtZwangsweiseAllIn() {
        ClientSession host = join("Host");
        Player player = actor.getRoomForTest().byId(playerId(host));
        player.setPoints(10);

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 5);
        actor.awaitIdle();

        Pick pick = actor.getRoomForTest().getCurrentRound().getPicks().get(playerId(host));
        assertThat(pick.stake()).isEqualTo(10);
    }

    @Test
    void manuellesSchliessenBringtDieRundeNachClosedUndDeckdtTippsAufWennResolved() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.placePick(host, "touchdown", null);
        actor.closeBet(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void doppeltesSchliessenWirdStillIgnoriert() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.closeBet(host);
        actor.closeBet(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void autoCloseSchliesstDieRundeNachAblaufDesFensters() {
        ClientSession host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        clock.advance(Duration.ofSeconds(15));
        scheduler.fireAll();
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void veralteterAutoCloseAusEinerVorherigenRundeSchliesstDieNeueRundeNicht() {
        ClientSession host = join("Host");

        actor.openBet(host, null);
        actor.awaitIdle();
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();

        actor.openBet(host, null);
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

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.placePick(anna, "punt", 50);
        actor.closeBet(host);
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

        actor.openBet(host, null);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getCurrentRound().isAnnulled()).isTrue();
        assertThat(room.byId(playerId(host)).getPoints()).isEqualTo(Room.STARTING_POINTS);
    }

    /**
     * Anforderung 8.6: Der Host bricht ab, weil die offene Wette nicht mehr
     * zum Spiel passt. Entscheidend ist, dass trotz abgegebener Tipps kein
     * einziger Punkt bewegt wird — weder Einsatz noch Strafe.
     */
    @Test
    void annullierenAusOpenLaesstAlleKontenUnberuehrt() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        join("Ben"); // tippt nicht und darf trotzdem keine Strafe zahlen

        actor.openBet(host, "field-goal-attempt");
        actor.placePick(host, "good", 200);
        actor.placePick(anna, "no-good", 100);
        actor.annul(host);
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(room.getCurrentRound().isAnnulled()).isTrue();
        assertThat(room.getCurrentRound().isAnnulledByHost()).isTrue();
        assertThat(room.getCurrentRound().getPool()).isZero();
        assertThat(room.players()).allSatisfy(
                player -> assertThat(player.getPoints()).isEqualTo(Room.STARTING_POINTS));
    }

    @Test
    void annullierenGehtAuchNachDemSchliessenUndZaehltKeineVerpassteRunde() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String annaId = playerId(anna);

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", null);
        actor.disconnected(anna);
        actor.closeBet(host);
        actor.annul(host);
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(room.getCurrentRound().isAnnulledByHost()).isTrue();
        // Die Runde hat nicht stattgefunden, also faellt sie Anna auch nicht
        // als verpasste Runde zur Last (Anforderung 8.1).
        assertThat(room.byId(annaId).getMissedRounds()).isZero();
        assertThat(room.byId(annaId).getPoints()).isEqualTo(Room.STARTING_POINTS);
    }

    /**
     * Nach RESOLVED sind die Punkte verrechnet; ein Abbruch waere dann eine
     * Rueckabwicklung und keine Notbremse mehr. Genauso wenig laesst sich in
     * IDLE etwas annullieren.
     */
    @Test
    void annullierenGehtWederInIdleNochNachDemAufloesen() {
        ClientSession host = join("Host");

        actor.annul(host);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();
        int pointsAfterResolve = actor.getRoomForTest().byId(playerId(host)).getPoints();

        actor.annul(host);
        actor.awaitIdle();

        Round round = actor.getRoomForTest().getCurrentRound();
        assertThat(round.isAnnulled()).isFalse();
        assertThat(round.getWinningOutcomeId()).isEqualTo("touchdown");
        assertThat(actor.getRoomForTest().byId(playerId(host)).getPoints()).isEqualTo(pointsAfterResolve);
    }

    @Test
    void nurDerHostDarfAnnullieren() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");

        actor.openBet(host, null);
        actor.annul(anna);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);
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
        actor.openBet(host, null);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getCurrentRound().getParticipants()).doesNotContain(annaId);
        playAndResolveRoundWithoutAnna(host);
        assertThat(actor.getRoomForTest().byId(annaId).getPoints())
                .isEqualTo(Room.STARTING_POINTS - 50);
    }

    private void playAndResolveRoundWithoutAnna(ClientSession host) {
        actor.openBet(host, null);
        actor.placePick(host, "touchdown", null);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();
    }

    @Test
    void hostVerliertRolleSofortAberFruehererJoinerBekommtSieErstBeiResolvedZurueck() {
        ClientSession host = join("Host");
        ClientSession anna = join("Anna");
        String hostId = playerId(host);

        actor.openBet(host, null);
        actor.awaitIdle();

        // Host trennt sich mitten in OPEN: Verlust wirkt sofort.
        actor.disconnected(host);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(playerId(anna));

        // Host kommt waehrend OPEN zurueck -- die Rolle bleibt vorerst bei Anna.
        ClientSession hostReturned = rejoin("Host", hostId);
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(playerId(anna));

        // Erst beim Erreichen von RESOLVED reklamiert der fruehere Host die Rolle zurueck.
        actor.closeBet(anna);
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
