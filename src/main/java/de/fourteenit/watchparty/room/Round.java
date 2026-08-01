package de.fourteenit.watchparty.room;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Eine Runde: ein geoeffneter Markt von OPEN bis RESOLVED (ADR-020). Jede
 * Runde traegt eine eigene, monoton steigende ID, damit ein verspaeteter
 * Auto-Close-Task nicht versehentlich die naechste, schon wieder offene
 * Runde schliesst (ADR-010).
 *
 * Nur vom {@link RoomActor} auf dem Raum-Thread beruehrt, daher ohne jede
 * Synchronisierung (Invariante 1).
 */
public class Round {

    private final long id;
    private final Market market;
    private final Instant closesAt;

    /** Beim Oeffnen eingefroren (Anforderung 8.1): nur diese Spieler koennen bestraft werden. */
    private final Set<String> participants;

    private final Map<String, Bet> bets = new LinkedHashMap<>();

    private Phase phase = Phase.OPEN;
    private String winningOutcomeId;
    private Map<String, Integer> deltas;
    private int pool;
    private boolean annulled;

    Round(long id, Market market, Instant closesAt, Set<String> participants) {
        this.id = id;
        this.market = market;
        this.closesAt = closesAt;
        this.participants = new LinkedHashSet<>(participants);
    }

    public long getId() {
        return id;
    }

    public Market getMarket() {
        return market;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public Map<String, Bet> getBets() {
        return bets;
    }

    boolean hasBet(String playerId) {
        return bets.containsKey(playerId);
    }

    void addBet(Bet bet) {
        bets.put(bet.playerId(), bet);
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
}
