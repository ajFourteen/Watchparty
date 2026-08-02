package de.fourteen.watchparty.domain.model;

import de.fourteen.watchparty.adapter.out.file.SnapshotStore;

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

    private static PlayerId spieler(String id) {
        return PlayerId.of(id);
    }

    private static Player addPlayer(Room room, String id, String token, String name) {
        return room.addPlayer(PlayerId.of(id), Token.of(token), PlayerName.of(name));
    }

    private static Pick pick(String playerId, String outcomeId, int stake) {
        return new Pick(PlayerId.of(playerId), OutcomeId.of(outcomeId), Points.of(stake));
    }

    @Test
    void idleRaumUeberstehtDenRundweg() {
        Room room = new Room();
        addPlayer(room, "p1", "t1", "Anna");
        Player p2 = addPlayer(room, "p2", "t2", "Bo");
        p2.setConnected(false);
        p2.incrementMissedRounds();

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        assertThat(restored.getPhase()).isEqualTo(Phase.IDLE);
        assertThat(restored.getHostPlayerId()).isEqualTo(room.getHostPlayerId());
        assertThat(restored.players()).hasSize(2);

        Player anna = restored.byId(spieler("p1"));
        assertThat(anna.getName()).isEqualTo(PlayerName.of("Anna"));
        assertThat(anna.getToken()).isEqualTo(Token.of("t1"));
        assertThat(anna.getPoints()).isEqualTo(Room.STARTING_POINTS);
        assertThat(anna.isConnected()).isFalse();

        assertThat(restored.byId(spieler("p2")).getMissedRounds()).isEqualTo(1);
        assertThat(restored.byToken(Token.of("t2")).getId()).isEqualTo(spieler("p2"));
    }

    @Test
    void offeneRundeUeberstehtDenRundwegMitTippsUndTeilnehmerkreis() {
        Room room = new Room();
        addPlayer(room, "host", "th", "Host");
        addPlayer(room, "p2", "t2", "Bo");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.addPick(pick("p2", "touchdown", 100));

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        assertThat(restored.getPhase()).isEqualTo(Phase.OPEN);
        Round restoredRound = restored.getCurrentRound();
        assertThat(restoredRound.getId()).isEqualTo(round.getId());
        assertThat(restoredRound.getBet().id()).isEqualTo(BetId.of("drive-outcome"));
        assertThat(restoredRound.getClosesAt()).isEqualTo(round.getClosesAt());
        assertThat(restoredRound.getParticipants()).containsExactlyInAnyOrder(spieler("host"), spieler("p2"));
        assertThat(restoredRound.pickOf(spieler("p2")).outcomeId()).isEqualTo(OutcomeId.of("touchdown"));
        assertThat(restoredRound.pickOf(spieler("p2")).stake()).isEqualTo(Points.of(100));
    }

    @Test
    void geschlosseneRundeUeberstehtDenRundwegMitAllenAufgedecktenTipps() {
        Room room = new Room();
        addPlayer(room, "host", "th", "Host");
        addPlayer(room, "p2", "t2", "Bo");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.addPick(pick("host", "punt", 25));
        round.addPick(pick("p2", "touchdown", 100));
        round.setPhase(Phase.CLOSED);

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        assertThat(restored.getPhase()).isEqualTo(Phase.CLOSED);
        assertThat(restored.getCurrentRound().getPicks()).hasSize(2);
    }

    @Test
    void aufgeloesteRundeUeberstehtDenRundwegMitErgebnisPoolUndDeltas() {
        Room room = new Room();
        addPlayer(room, "host", "th", "Host");
        addPlayer(room, "p2", "t2", "Bo");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.addPick(pick("host", "touchdown", 100));
        round.addPick(pick("p2", "punt", 50));
        round.setPhase(Phase.CLOSED);
        round.setWinningOutcomeId(OutcomeId.of("touchdown"));
        round.setDeltas(Map.of(spieler("host"), PointsDelta.of(50), spieler("p2"), PointsDelta.of(-50)));
        round.setPool(Points.of(150));
        round.setAnnulled(false);
        round.setPhase(Phase.RESOLVED);

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        Round restoredRound = restored.getCurrentRound();
        assertThat(restoredRound.getPhase()).isEqualTo(Phase.RESOLVED);
        assertThat(restoredRound.getWinningOutcomeId()).isEqualTo(OutcomeId.of("touchdown"));
        assertThat(restoredRound.getPool()).isEqualTo(Points.of(150));
        assertThat(restoredRound.isAnnulled()).isFalse();
        assertThat(restoredRound.getDeltas()).containsEntry(spieler("host"), PointsDelta.of(50))
                .containsEntry(spieler("p2"), PointsDelta.of(-50));
    }

    @Test
    void vomHostAnnullierteRundeUeberstehtDenRundweg() {
        Room room = new Room();
        addPlayer(room, "host", "th", "Host");
        Round round = room.openBet(Bets.DRIVE_OUTCOME, NOW, Duration.ofSeconds(15));
        round.setPhase(Phase.CLOSED);
        round.setDeltas(Map.of());
        round.setPool(Points.ZERO);
        round.setAnnulled(true);
        round.setAnnulledByHost(true);
        round.setPhase(Phase.RESOLVED);

        Room restored = Room.fromSnapshot(room.toSnapshot(NOW.toEpochMilli()));

        Round restoredRound = restored.getCurrentRound();
        assertThat(restoredRound.isAnnulled()).isTrue();
        assertThat(restoredRound.isAnnulledByHost()).isTrue();
        assertThat(restoredRound.getPool()).isEqualTo(Points.ZERO);
    }

    @Test
    void unbekannteWetteImSnapshotVerwirftDieRundeAberNichtDieSpieler() {
        Room room = new Room();
        addPlayer(room, "host", "th", "Host");
        addPlayer(room, "p2", "t2", "Bo");
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
        assertThat(restored.byId(spieler("p2")).getName()).isEqualTo(PlayerName.of("Bo"));
    }
}
