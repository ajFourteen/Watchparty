package de.fourteenit.watchparty.room;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Eine Runde: eine geoeffnete Wette von OPEN bis RESOLVED (ADR-020). Jede
 * Runde traegt eine eigene, monoton steigende ID, damit ein verspaeteter
 * Auto-Close-Task nicht versehentlich die naechste, schon wieder offene
 * Runde schliesst (ADR-010).
 *
 * Nur vom {@link RoomActor} auf dem Raum-Thread beruehrt, daher ohne jede
 * Synchronisierung (Invariante 1).
 */
public class Round {

    private final long id;
    private final Bet bet;
    private final Instant closesAt;

    /** Beim Oeffnen eingefroren (Anforderung 8.1): nur diese Spieler koennen bestraft werden. */
    private final Set<String> participants;

    private final Map<String, Pick> picks = new LinkedHashMap<>();

    private Phase phase = Phase.OPEN;
    private String winningOutcomeId;
    private Map<String, Integer> deltas;
    private int pool;
    private boolean annulled;

    /**
     * Trennt die beiden Gruende fuers Annullieren: niemand hat getippt
     * (Anforderung 8.4) oder der Host hat abgebrochen (8.6). Fuer die Punkte
     * macht es keinen Unterschied — beide Male passiert nichts —, aber die
     * Oberflaeche darf den Spielern nicht den falschen Grund nennen.
     */
    private boolean annulledByHost;

    Round(long id, Bet bet, Instant closesAt, Set<String> participants) {
        this.id = id;
        this.bet = bet;
        this.closesAt = closesAt;
        this.participants = new LinkedHashSet<>(participants);
    }

    public long getId() {
        return id;
    }

    public Bet getBet() {
        return bet;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public Map<String, Pick> getPicks() {
        return picks;
    }

    boolean hasPick(String playerId) {
        return picks.containsKey(playerId);
    }

    void addPick(Pick pick) {
        picks.put(pick.playerId(), pick);
    }

    public Phase getPhase() {
        return phase;
    }

    void setPhase(Phase phase) {
        this.phase = phase;
    }

    public String getWinningOutcomeId() {
        return winningOutcomeId;
    }

    void setWinningOutcomeId(String winningOutcomeId) {
        this.winningOutcomeId = winningOutcomeId;
    }

    public Map<String, Integer> getDeltas() {
        return deltas;
    }

    void setDeltas(Map<String, Integer> deltas) {
        this.deltas = deltas;
    }

    public int getPool() {
        return pool;
    }

    void setPool(int pool) {
        this.pool = pool;
    }

    public boolean isAnnulled() {
        return annulled;
    }

    void setAnnulled(boolean annulled) {
        this.annulled = annulled;
    }

    public boolean isAnnulledByHost() {
        return annulledByHost;
    }

    void setAnnulledByHost(boolean annulledByHost) {
        this.annulledByHost = annulledByHost;
    }
}
