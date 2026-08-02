package de.fourteen.watchparty.domain.model;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Der eine Raum, komplett im Arbeitsspeicher (ADR-004).
 *
 * <b>Aggregate Root.</b> {@link Player} und {@link Round} sind Entities
 * darin und nur ueber ihn erreichbar; ihre Mutatoren sind paket-privat.
 * Nach aussen gibt es keine Setter, sondern Uebergaenge, die die Absicht
 * benennen — {@code openBet}, {@code closeCurrentRound},
 * {@code resolveCurrentRound}. Wer den Zustand aendern will, sagt <em>was</em>
 * passiert, nicht <em>welches Feld</em> sich aendert.
 *
 * Kein Zugriff von aussen ausser ueber den {@code RoomActor}; alle Methoden
 * laufen auf dem Raum-Thread und sind deshalb ohne Synchronisierung
 * geschrieben (ADR-009, Invariante 1).
 */
public class Room {

    /** Startguthaben (Anforderung 3.1). Wert wird am Spielgefuehl kalibriert. */
    public static final Points STARTING_POINTS = Points.of(1000);

    /** Einfuegereihenfolge zaehlt: der erste Joiner wird Host (ADR-016). */
    private final Map<PlayerId, Player> playersById = new LinkedHashMap<>();
    private final Map<Token, PlayerId> playerIdByToken = new LinkedHashMap<>();

    /** {@code null}, solange kein Spieler beigetreten ist. */
    private @Nullable PlayerId hostPlayerId;

    /** {@code null} in Phase IDLE -- noch keine Runde eroeffnet. */
    private @Nullable Round currentRound;
    private RoundId nextRoundId = RoundId.of(1);

    // --- Teilnehmer -----------------------------------------------------------

    public Player addPlayer(PlayerId id, Token token, PlayerName name) {
        Player player = new Player(id, token, name, STARTING_POINTS);
        playersById.put(id, player);
        playerIdByToken.put(token, id);
        if (hostPlayerId == null) {
            hostPlayerId = id;
        }
        return player;
    }

    /**
     * Reconnect (ADR-014): dasselbe Konto, neue Verbindung. Der
     * Verpasste-Runden-Zaehler beginnt von vorn (Anforderung 8.1).
     *
     * {@code null}, wenn der Token unbekannt ist — dann ist es kein
     * Reconnect, sondern ein neuer Spieler.
     */
    public @Nullable Player rejoin(@Nullable Token token, PlayerName name) {
        Player player = byToken(token);
        if (player == null) {
            return null;
        }
        player.setName(name);
        player.setConnected(true);
        player.resetMissedRounds();
        return player;
    }

    public void markDisconnected(PlayerId playerId) {
        Player player = playersById.get(playerId);
        if (player != null) {
            player.setConnected(false);
        }
    }

    public @Nullable Player byToken(@Nullable Token token) {
        if (token == null) {
            return null;
        }
        PlayerId id = playerIdByToken.get(token);
        return id == null ? null : playersById.get(id);
    }

    public @Nullable Player byId(@Nullable PlayerId id) {
        return id == null ? null : playersById.get(id);
    }

    public Collection<Player> players() {
        return List.copyOf(playersById.values());
    }

    /** {@code null}, solange kein Spieler beigetreten ist. */
    public @Nullable PlayerId getHostPlayerId() {
        return hostPlayerId;
    }

    public boolean isHost(@Nullable PlayerId playerId) {
        return hostPlayerId != null && hostPlayerId.equals(playerId);
    }

    /**
     * Uebergibt die Host-Rolle an den am fruehesten beigetretenen verbundenen
     * Spieler (ADR-021). Verlieren wirkt immer sofort, sonst waere der Raum
     * mitten im offenen Fenster steuerlos. Zurueckholen — ein frueher
     * beigetretener Spieler kommt zurueck und soll die Rolle reklamieren —
     * wirkt nur, wenn {@code allowPickup} gilt, also in IDLE oder RESOLVED;
     * sonst rutschen dem Vertreter die Steuerknoepfe mitten in der Runde weg.
     */
    public void reassignHostIfNeeded(boolean allowPickup) {
        Player host = hostPlayerId == null ? null : playersById.get(hostPlayerId);
        boolean hostLost = host == null || !host.isConnected();
        if (!hostLost && !allowPickup) {
            return;
        }
        hostPlayerId = playersById.values().stream()
                .filter(Player::isConnected)
                .map(Player::getId)
                .findFirst()
                .orElse(null);
    }

    // --- Zustandsautomat der aktuellen Runde (ADR-020) ------------------------

    public Phase getPhase() {
        return currentRound == null ? Phase.IDLE : currentRound.getPhase();
    }

    /** {@code null} in Phase IDLE. */
    public @Nullable Round getCurrentRound() {
        return currentRound;
    }

    /**
     * Die Uebergaenge unten setzen voraus, dass eine Runde laeuft -- der
     * Aufrufer (der Anwendungsring) muss das vorher pruefen, etwa ueber
     * {@code getPhase()}. Diese Methode macht die Vorbedingung explizit statt
     * sie stillschweigend vorauszusetzen: Ein Verstoss ist ein Programmfehler
     * und meldet sich klar, statt irgendwo spaeter eine NullPointerException
     * ohne Kontext zu werfen.
     */
    private Round requireCurrentRound() {
        Round round = currentRound;
        if (round == null) {
            throw new IllegalStateException("Kein Aufruf ohne laufende Runde erlaubt -- der Aufrufer muss das vorher pruefen");
        }
        return round;
    }

    /**
     * Oeffnet eine neue Runde. Der Teilnehmerkreis wird jetzt eingefroren
     * (Anforderung 8.1): dabei, wer verbunden ist, plus wer getrennt ist aber
     * noch keine zwei Runden am Stueck verpasst hat — ab der dritten
     * verpassten Runde pausiert ein getrennter Spieler und zahlt nicht mehr.
     */
    public Round openBet(Bet bet, Instant now, Duration window) {
        Set<PlayerId> participants = new LinkedHashSet<>();
        for (Player player : playersById.values()) {
            if (!player.isPaused()) {
                participants.add(player.getId());
            }
        }
        currentRound = new Round(nextRoundId, bet, now.plus(window), participants);
        nextRoundId = nextRoundId.next();
        return currentRound;
    }

    // --- Uebergaenge der aktuellen Runde --------------------------------------
    //
    // Diese Methoden gibt es, weil die Mutatoren von Round paket-privat sind
    // und bleiben sollen: Sie sind die Aggregatgrenze. Der Anwendungsring
    // spricht hier die Absicht aus, das Aggregat fuehrt sie aus.

    /** Das Wettfenster ist zu — manuell oder per Auto-Close (ADR-010, ADR-020). */
    public void closeCurrentRound() {
        requireCurrentRound().setPhase(Phase.CLOSED);
    }

    /** Nimmt einen Tipp in die laufende Runde auf. Die Pruefung macht der Aufrufer. */
    public void addPick(Pick pick) {
        requireCurrentRound().addPick(pick);
    }

    /**
     * Anforderung 8.6: Der Host dreht die Runde zurueck. Billig, weil Punkte
     * erst beim Aufloesen verrechnet werden (ADR-020) — vor RESOLVED gibt es
     * nichts zurueckzurechnen, die Nullsumme kann hier nicht kaputtgehen.
     */
    public void annulCurrentRound() {
        Round round = requireCurrentRound();
        round.setDeltas(Map.of());
        round.setPool(Points.ZERO);
        round.setAnnulled(true);
        round.setAnnulledByHost(true);
        round.setPhase(Phase.RESOLVED);
    }

    /**
     * Verbucht das Ergebnis der Abrechnung und schaltet nach RESOLVED. Die
     * Rechnung selbst steckt in {@code Settlement}; hier wird sie angewandt
     * und festgehalten.
     */
    public void resolveCurrentRound(OutcomeId winningOutcomeId, Map<PlayerId, PointsDelta> deltas,
            Points pool, boolean annulled) {
        for (Map.Entry<PlayerId, PointsDelta> entry : deltas.entrySet()) {
            Player player = playersById.get(entry.getKey());
            if (player != null) {
                player.credit(entry.getValue());
            }
        }
        Round round = requireCurrentRound();
        round.setWinningOutcomeId(winningOutcomeId);
        round.setDeltas(deltas);
        round.setPool(pool);
        round.setAnnulled(annulled);
        round.setPhase(Phase.RESOLVED);
    }

    /**
     * Anforderung 8.1: Nur <em>getrennte</em> Nicht-Tipper zaehlen fuer die
     * Pause; wer verbunden ist und nicht tippt, zahlt jede Runde ohne Pause.
     */
    public void countMissedRounds(Set<PlayerId> nonPickers) {
        for (PlayerId playerId : nonPickers) {
            Player player = playersById.get(playerId);
            if (player != null && !player.isConnected()) {
                player.incrementMissedRounds();
            }
        }
    }

    // --- Snapshot (ADR-023) ---------------------------------------------------

    /**
     * Reines Kopieren von Feldern, kein I/O — die Stelle, an der ein
     * {@code SnapshotStore} ansetzt. Laeuft auf dem Raum-Thread wie jede
     * andere Lesung von {@code Room} (Invariante 1).
     *
     * Die Value Objects werden hier auf einfache Typen abgewickelt: Das
     * Dateiformat soll sich nicht aendern, nur weil das Modell praeziser
     * geworden ist — genau die Entkopplung, die ADR-023 wollte.
     */
    public RoomSnapshot toSnapshot(long savedAt) {
        List<RoomSnapshot.PlayerSnapshot> playerSnapshots = new ArrayList<>();
        for (Player player : playersById.values()) {
            playerSnapshots.add(new RoomSnapshot.PlayerSnapshot(
                    player.getId().value(),
                    player.getToken().value(),
                    player.getName().value(),
                    player.getPoints().value(),
                    player.getMissedRounds()));
        }

        RoomSnapshot.RoundSnapshot roundSnapshot = currentRound == null ? null : toSnapshot(currentRound);

        return new RoomSnapshot(RoomSnapshot.SCHEMA_VERSION, savedAt,
                hostPlayerId == null ? null : hostPlayerId.value(),
                nextRoundId.value(), playerSnapshots, roundSnapshot);
    }

    private static RoomSnapshot.RoundSnapshot toSnapshot(Round round) {
        List<RoomSnapshot.PickSnapshot> pickSnapshots = new ArrayList<>();
        for (Pick pick : round.picksInOrder()) {
            pickSnapshots.add(new RoomSnapshot.PickSnapshot(
                    pick.playerId().value(), pick.outcomeId().value(), pick.stake().value()));
        }

        Map<String, Integer> deltas = null;
        if (round.getDeltas() != null) {
            deltas = new LinkedHashMap<>();
            for (Map.Entry<PlayerId, PointsDelta> entry : round.getDeltas().entrySet()) {
                deltas.put(entry.getKey().value(), entry.getValue().value());
            }
        }

        List<String> participants = round.getParticipants().stream().map(PlayerId::value).toList();

        return new RoomSnapshot.RoundSnapshot(
                round.getId().value(),
                round.getBet().id().value(),
                round.getClosesAt().toEpochMilli(),
                round.getPhase().name(),
                participants,
                pickSnapshots,
                round.getWinningOutcomeId() == null ? null : round.getWinningOutcomeId().value(),
                deltas,
                round.getPool().value(),
                round.isAnnulled(),
                round.isAnnulledByHost());
    }

    /**
     * Baut einen {@code Room} aus einem zuvor geschriebenen Snapshot wieder
     * auf. Wer sich meldet, wird verbunden — beim Laden ist deshalb jeder
     * {@link Player} zunaechst getrennt, alles andere waere gelogen. Eine
     * Runde, deren {@code betId} es im aktuellen Katalog (ADR-017) nicht mehr
     * gibt, wird verworfen statt eine unbekannte Wette wiederzubeleben;
     * Spieler und Punkte bleiben davon unberuehrt.
     */
    public static Room fromSnapshot(RoomSnapshot snapshot) {
        Room room = new Room();
        for (RoomSnapshot.PlayerSnapshot ps : snapshot.players()) {
            PlayerId id = PlayerId.of(ps.id());
            Token token = Token.of(ps.token());
            Player player = new Player(id, token, PlayerName.of(ps.name()), Points.of(ps.points()));
            player.setConnected(false);
            for (int i = 0; i < ps.missedRounds(); i++) {
                player.incrementMissedRounds();
            }
            room.playersById.put(id, player);
            room.playerIdByToken.put(token, id);
        }
        room.hostPlayerId = snapshot.hostPlayerId() == null ? null : PlayerId.of(snapshot.hostPlayerId());
        room.nextRoundId = RoundId.of(snapshot.nextRoundId());

        RoomSnapshot.RoundSnapshot rs = snapshot.round();
        if (rs != null) {
            Bet bet = Bets.byId(BetId.ofNullable(rs.betId()));
            if (bet != null) {
                room.currentRound = fromSnapshot(rs, bet);
            }
        }
        return room;
    }

    private static Round fromSnapshot(RoomSnapshot.RoundSnapshot rs, Bet bet) {
        Set<PlayerId> participants = new LinkedHashSet<>();
        for (String participant : rs.participants()) {
            participants.add(PlayerId.of(participant));
        }

        Round round = new Round(RoundId.of(rs.id()), bet, Instant.ofEpochMilli(rs.closesAt()), participants);
        round.setPhase(Phase.valueOf(rs.phase()));
        for (RoomSnapshot.PickSnapshot ps : rs.picks()) {
            round.addPick(new Pick(PlayerId.of(ps.playerId()), OutcomeId.of(ps.outcomeId()), Points.of(ps.stake())));
        }
        round.setWinningOutcomeId(OutcomeId.ofNullable(rs.winningOutcomeId()));
        if (rs.deltas() != null) {
            Map<PlayerId, PointsDelta> deltas = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : rs.deltas().entrySet()) {
                deltas.put(PlayerId.of(entry.getKey()), PointsDelta.of(entry.getValue()));
            }
            round.setDeltas(deltas);
        }
        round.setPool(Points.of(rs.pool()));
        round.setAnnulled(rs.annulled());
        round.setAnnulledByHost(rs.annulledByHost());
        return round;
    }
}
