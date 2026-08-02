package de.fourteen.watchparty.room;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hin- und Rückweg {@code Room -> RoomSnapshot -> Room} für jede Phase
 * (ADR-023). Reines Datenmodell, ohne {@link SnapshotStore} oder Dateisystem
 * — die Serialisierung auf die Platte prüft {@code SnapshotStoreTest}, das
 * Laden beim Start {@code RestoreTest}.
 */
class SnapshotTest {

    private static final Instant NOW = Instant.parse("2026-08-01T20:00:00Z");

    @Test
    void idleRaumUeberstehtDenRundweg() {
        Room room = new Room();
        room.addPlayer("p1", "t1", "Anna");
        Player p2 = room.addPlayer("p2", "t2", "Bo");
        p2.setConnected(false);
        p2.incrementMissedRounds();

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        assertThat(restored.getPhase()).isEqualTo(Phase.IDLE);
        assertThat(restored.getHostPlayerId()).isEqualTo(room.getHostPlayerId());
        assertThat(restored.players()).hasSize(2);

        Player anna = restored.byId("p1");
        assertThat(anna.getName()).isEqualTo("Anna");
        assertThat(anna.getToken()).isEqualTo("t1");
        assertThat(anna.getPoints()).isEqualTo(Room.STARTING_POINTS);
        assertThat(anna.isConnected()).isFalse();

        assertThat(restored.byId("p2").getMissedRounds()).isEqualTo(1);
        assertThat(restored.byToken("t2").getId()).isEqualTo("p2");
    }

    @Test
    void offeneRundeUeberstehtDenRundwegMitTippsUndTeilnehmerkreis() {
        Room room = new Room();
        room.addPlayer("host", "th", "Host");
        room.addPlayer("p2", "t2", "Bo");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.addPick(new Pick("p2", "touchdown", 100));

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        assertThat(restored.getPhase()).isEqualTo(Phase.OPEN);
        Round restoredRound = restored.getCurrentRound();
        assertThat(restoredRound.getId()).isEqualTo(round.getId());
        assertThat(restoredRound.getBet().id()).isEqualTo("drive-outcome");
        assertThat(restoredRound.getClosesAt()).isEqualTo(round.getClosesAt());
        assertThat(restoredRound.getParticipants()).containsExactlyInAnyOrder("host", "p2");
        assertThat(restoredRound.getPicks().get("p2").outcomeId()).isEqualTo("touchdown");
        assertThat(restoredRound.getPicks().get("p2").stake()).isEqualTo(100);
    }

    @Test
    void geschlosseneRundeUeberstehtDenRundwegMitAllenAufgedecktenTipps() {
        Room room = new Room();
        room.addPlayer("host", "th", "Host");
        room.addPlayer("p2", "t2", "Bo");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.addPick(new Pick("host", "punt", 25));
        round.addPick(new Pick("p2", "touchdown", 100));
        round.setPhase(Phase.CLOSED);

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        assertThat(restored.getPhase()).isEqualTo(Phase.CLOSED);
        assertThat(restored.getCurrentRound().getPicks()).hasSize(2);
    }

    @Test
    void aufgeloesteRundeUeberstehtDenRundwegMitErgebnisPoolUndDeltas() {
        Room room = new Room();
        room.addPlayer("host", "th", "Host");
        room.addPlayer("p2", "t2", "Bo");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.addPick(new Pick("host", "touchdown", 100));
        round.addPick(new Pick("p2", "punt", 50));
        round.setPhase(Phase.CLOSED);
        round.setWinningOutcomeId("touchdown");
        round.setDeltas(Map.of("host", 50, "p2", -50));
        round.setPool(150);
        round.setAnnulled(false);
        round.setPhase(Phase.RESOLVED);

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        Round restoredRound = restored.getCurrentRound();
        assertThat(restoredRound.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(restoredRound.getWinningOutcomeId()).isEqualTo("touchdown");
        assertThat(restoredRound.getPool()).isEqualTo(150);
        assertThat(restoredRound.isAnnulled()).isFalse();
        assertThat(restoredRound.getDeltas()).containsEntry("host", 50).containsEntry("p2", -50);
    }

    @Test
    void vomHostAnnullierteRundeUeberstehtDenRundweg() {
        Room room = new Room();
        room.addPlayer("host", "th", "Host");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.setPhase(Phase.CLOSED);
        round.setDeltas(Map.of());
        round.setPool(0);
        round.setAnnulled(true);
        round.setAnnulledByHost(true);
        round.setPhase(Phase.RESOLVED);

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        Round restoredRound = restored.getCurrentRound();
        assertThat(restoredRound.isAnnulled()).isTrue();
        assertThat(restoredRound.isAnnulledByHost()).isTrue();
        assertThat(restoredRound.getPool()).isZero();
    }

    @Test
    void unbekannteWetteImSnapshotVerwirftDieRundeAberNichtDieSpieler() {
        Room room = new Room();
        room.addPlayer("host", "th", "Host");
        room.addPlayer("p2", "t2", "Bo");
        room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        RoomSnapshot original = room.toSnapshot(NOW.toEpochMilli());
        RoomSnapshot.RoundSnapshot roundMitUnbekannterWette = new RoomSnapshot.RoundSnapshot(
                original.round().id(), "es-gibt-diese-wette-nicht-mehr", original.round().closesAt(),
                original.round().phase(), original.round().participants(), List.of(),
                null, null, 0, false, false);
        RoomSnapshot manipuliert = new RoomSnapshot(original.schemaVersion(), original.savedAt(),
                original.hostPlayerId(), original.nextRoundId(), original.players(), roundMitUnbekannterWette);

        Room restored = Room.fromSnapshot(manipuliert);

        assertThat(restored.getPhase()).isEqualTo(Phase.IDLE);
        assertThat(restored.players()).hasSize(2);
        assertThat(restored.byId("p2").getName()).isEqualTo("Bo");
    }
}
