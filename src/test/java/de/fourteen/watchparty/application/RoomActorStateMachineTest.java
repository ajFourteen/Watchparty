package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Phase;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.Player;
import de.fourteen.watchparty.domain.model.Room;
import de.fourteen.watchparty.domain.model.RoundId;
import de.fourteen.watchparty.domain.model.Round;
import de.fourteen.watchparty.teststrategy.PortTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der Zustandsautomat aus ADR-020, durchgespielt als Ereignis-Sequenzen ueber
 * die oeffentlichen Eintrittspunkte von {@link RoomActor}. Prueft ueber
 * {@link RoomActor#getRoomForTest()} den Raumzustand direkt, statt das
 * JSON-Protokoll zu parsen — das prueft der Rauchtest ueber die Leitung.
 */
@PortTest
class RoomActorStateMachineTest {

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

    private PlayerId playerId(String session) {
        return gateway.playerIdOf(session);
    }

    @Test
    void ersterJoinerWirdHostUndOeffnenBringtDieRundeInOpen() {
        String host = join("Host");
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
        String anna = join("Anna");

        actor.openBet(anna, null);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void openBetWaehrendLaufenderRundeIstEinFehler() {
        String host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        actor.openBet(host, null);
        actor.awaitIdle();

        // Immer noch dieselbe Runde, keine zweite wurde angelegt.
        assertThat(actor.getRoomForTest().getCurrentRound().getId()).isEqualTo(RoundId.of(1));
    }

    @Test
    void tippAbgabeInnerhalbDesFensterWirdUebernommenMitMindesteinsatzAlsStandard() {
        String host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        actor.placePick(host, "touchdown", null);
        actor.awaitIdle();

        Pick pick = actor.getRoomForTest().getCurrentRound().getPicks().get(playerId(host));
        assertThat(pick.outcomeId()).isEqualTo(OutcomeId.of("touchdown"));
        assertThat(pick.stake()).isEqualTo(Points.of(25));
    }

    @Test
    void tippNachAblaufVonClosesAtZaehltNichtMehrAuchOhneDassDerTimerSchonGefeuertHat() {
        String host = join("Host");
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
        String host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        actor.placePick(host, "touchdown", 100);
        actor.placePick(host, "punt", 50);
        actor.awaitIdle();

        Pick pick = actor.getRoomForTest().getCurrentRound().pickOf(playerId(host));
        assertThat(pick.stake()).isEqualTo(Points.of(100));
        assertThat(pick.outcomeId()).isEqualTo(OutcomeId.of("touchdown"));
    }

    // spielerUnterDemMindesteinsatzGehtZwangsweiseAllIn entfaellt hier: Die
    // Regel sitzt in Player.stakeFor und ist dort direkt getestet
    // (PlayerTest.unterDemMindesteinsatzGehtEsZwangsweiseAllIn). Player.setPoints
    // ist seit den Value Objects bewusst paket-privat -- Mutation laeuft ueber
    // das Aggregat, nicht von aussen.

    @Test
    void manuellesSchliessenBringtDieRundeNachClosedUndDeckdtTippsAufWennResolved() {
        String host = join("Host");
        actor.openBet(host, null);
        actor.placePick(host, "touchdown", null);
        actor.closeBet(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void doppeltesSchliessenWirdStillIgnoriert() {
        String host = join("Host");
        actor.openBet(host, null);
        actor.closeBet(host);
        actor.closeBet(host);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void autoCloseSchliesstDieRundeNachAblaufDesFensters() {
        String host = join("Host");
        actor.openBet(host, null);
        actor.awaitIdle();

        clock.advance(Duration.ofSeconds(15));
        scheduler.fireAll();
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.CLOSED);
    }

    @Test
    void veralteterAutoCloseAusEinerVorherigenRundeSchliesstDieNeueRundeNicht() {
        String host = join("Host");

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
        String host = join("Host");
        String anna = join("Anna");

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.placePick(anna, "punt", 50);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();

        Room room = actor.getRoomForTest();
        assertThat(room.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(room.byId(playerId(host)).getPoints()).isEqualTo(Points.of(Room.STARTING_POINTS.value() + 50));
        assertThat(room.byId(playerId(anna)).getPoints()).isEqualTo(Points.of(Room.STARTING_POINTS.value() - 50));
        assertThat(room.getCurrentRound().getPool()).isEqualTo(Points.of(150));
        assertThat(room.getCurrentRound().isAnnulled()).isFalse();
    }

    @Test
    void aufloesenOhneJedenTippAnnulliertDieRundeOhneVerrechnung() {
        String host = join("Host");

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
        String host = join("Host");
        String anna = join("Anna");
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
        assertThat(room.getCurrentRound().getPool()).isEqualTo(Points.ZERO);
        assertThat(room.players()).allSatisfy(
                player -> assertThat(player.getPoints()).isEqualTo(Room.STARTING_POINTS));
    }

    @Test
    void annullierenGehtAuchNachDemSchliessenUndZaehltKeineVerpassteRunde() {
        String host = join("Host");
        String anna = join("Anna");
        PlayerId annaId = playerId(anna);

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
        String host = join("Host");

        actor.annul(host);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.IDLE);

        actor.openBet(host, null);
        actor.placePick(host, "touchdown", 100);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();
        Points pointsAfterResolve = actor.getRoomForTest().byId(playerId(host)).getPoints();

        actor.annul(host);
        actor.awaitIdle();

        Round round = actor.getRoomForTest().getCurrentRound();
        assertThat(round.isAnnulled()).isFalse();
        assertThat(round.getWinningOutcomeId()).isEqualTo(OutcomeId.of("touchdown"));
        assertThat(actor.getRoomForTest().byId(playerId(host)).getPoints()).isEqualTo(pointsAfterResolve);
    }

    @Test
    void nurDerHostDarfAnnullieren() {
        String host = join("Host");
        String anna = join("Anna");

        actor.openBet(host, null);
        actor.annul(anna);
        actor.awaitIdle();

        assertThat(actor.getRoomForTest().getPhase()).isEqualTo(Phase.OPEN);
    }

    @Test
    void getrennterSpielerZahltDieErstenZweiVerpasstenRundenUndPausiertAbDerDritten() {
        String host = join("Host");
        String anna = join("Anna");
        PlayerId annaId = playerId(anna);

        actor.disconnected(anna);
        actor.awaitIdle();

        // Runde 1: Anna ist im eingefrorenen Kreis und zahlt die Strafe.
        playAndResolveRoundWithoutAnna(host);
        assertThat(actor.getRoomForTest().byId(annaId).getPoints())
                .isEqualTo(Points.of(Room.STARTING_POINTS.value() - 25));
        assertThat(actor.getRoomForTest().byId(annaId).getMissedRounds()).isEqualTo(1);

        // Runde 2: noch einmal.
        playAndResolveRoundWithoutAnna(host);
        assertThat(actor.getRoomForTest().byId(annaId).getPoints())
                .isEqualTo(Points.of(Room.STARTING_POINTS.value() - 50));
        assertThat(actor.getRoomForTest().byId(annaId).getMissedRounds()).isEqualTo(2);
        assertThat(actor.getRoomForTest().byId(annaId).isPaused()).isTrue();

        // Runde 3: Anna ist pausiert, gehoert nicht mehr zum Teilnehmerkreis.
        actor.openBet(host, null);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getCurrentRound().getParticipants()).doesNotContain(annaId);
        playAndResolveRoundWithoutAnna(host);
        assertThat(actor.getRoomForTest().byId(annaId).getPoints())
                .isEqualTo(Points.of(Room.STARTING_POINTS.value() - 50));
    }

    private void playAndResolveRoundWithoutAnna(String host) {
        actor.openBet(host, null);
        actor.placePick(host, "touchdown", null);
        actor.closeBet(host);
        actor.resolve(host, "touchdown");
        actor.awaitIdle();
    }

    @Test
    void hostVerliertRolleSofortAberFruehererJoinerBekommtSieErstBeiResolvedZurueck() {
        String host = join("Host");
        String anna = join("Anna");
        PlayerId hostId = playerId(host);

        actor.openBet(host, null);
        actor.awaitIdle();

        // Host trennt sich mitten in OPEN: Verlust wirkt sofort.
        actor.disconnected(host);
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(playerId(anna));

        // Host kommt waehrend OPEN zurueck -- die Rolle bleibt vorerst bei Anna.
        String hostReturned = rejoin("Host", hostId);
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(playerId(anna));

        // Erst beim Erreichen von RESOLVED reklamiert der fruehere Host die Rolle zurueck.
        actor.closeBet(anna);
        actor.resolve(anna, "touchdown");
        actor.awaitIdle();
        assertThat(actor.getRoomForTest().getHostPlayerId()).isEqualTo(hostId);
        assertThat(hostReturned).isNotNull();
    }

    private String rejoin(String name, PlayerId expectedPlayerId) {
        String token = actor.getRoomForTest().byId(expectedPlayerId).getToken().value();
        String sessionId = name + "-rejoin-socket";
        actor.connected(sessionId);
        actor.join(sessionId, name, token);
        actor.awaitIdle();
        return sessionId;
    }
}
