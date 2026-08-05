package de.fourteen.watchparty.domain.model;

import de.fourteen.watchparty.criticality.Criticality;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Eine Runde: eine geoeffnete Wette von OPEN bis RESOLVED (ADR-020).
 * <b>Entity</b> innerhalb des Aggregats {@link Room}, Identitaet ueber
 * {@link RoundId}.
 *
 * Die ID ist monoton steigend, damit ein verspaeteter Auto-Close-Task nicht
 * versehentlich die naechste, schon wieder offene Runde schliesst (ADR-010).
 *
 * Nur ueber das Aggregat erreichbar: Die Mutatoren sind paket-privat, und
 * genau das ist die Aggregatgrenze. Wer eine Runde weiterschalten will, sagt
 * es {@link Room}. Nur vom Raum-Thread beruehrt, daher ohne jede
 * Synchronisierung (Invariante 1).
 */
@Entity
@Criticality(level = Criticality.Level.MEDIUM,
        requirements = { "5-a", "5-b", "5-c", "5-d", "8.6", "8.6-a", "8.6-b", "9-a", "9-b", "9-c" })
public class Round {

    @Identity
    private final RoundId id;
    private final Bet bet;
    private final Instant closesAt;

    /** Beim Oeffnen eingefroren (Anforderung 8.1): nur diese Spieler koennen bestraft werden. */
    private final Set<PlayerId> participants;

    private final Map<PlayerId, Pick> picks = new LinkedHashMap<>();

    private Phase phase = Phase.OPEN;

    /** Erst ab RESOLVED gesetzt (ADR-020) -- vorher gibt es noch kein Ergebnis. */
    private @Nullable OutcomeId winningOutcomeId;

    /** Erst ab RESOLVED gesetzt, aus demselben Grund wie {@link #winningOutcomeId}. */
    private @Nullable Map<PlayerId, PointsDelta> deltas;

    private Points pool = Points.ZERO;
    private boolean annulled;

    /**
     * Trennt die beiden Gruende fuers Annullieren: niemand hat getippt
     * (Anforderung 8.4) oder der Host hat abgebrochen (8.6). Fuer die Punkte
     * macht es keinen Unterschied — beide Male passiert nichts —, aber die
     * Oberflaeche darf den Spielern nicht den falschen Grund nennen.
     */
    private boolean annulledByHost;

    Round(RoundId id, Bet bet, Instant closesAt, Set<PlayerId> participants) {
        this.id = id;
        this.bet = bet;
        this.closesAt = closesAt;
        this.participants = new LinkedHashSet<>(participants);
    }

    public RoundId getId() {
        return id;
    }

    public Bet getBet() {
        return bet;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    /** ADR-011: allein dieser Vergleich entscheidet, nicht ob der Timer schon gefeuert hat. */
    public boolean isOpenAt(Instant now) {
        return phase == Phase.OPEN && now.isBefore(closesAt);
    }

    public Set<PlayerId> getParticipants() {
        return Set.copyOf(participants);
    }

    public Map<PlayerId, Pick> getPicks() {
        return Map.copyOf(picks);
    }

    /** In Einfuegereihenfolge — die Abrechnung braucht sie stabil (Groesste-Reste-Verfahren, 7.2). */
    public java.util.List<Pick> picksInOrder() {
        return java.util.List.copyOf(picks.values());
    }

    /** Abfrage, kein Uebergang — darf deshalb nach aussen, anders als die Mutatoren. */
    public boolean hasPick(PlayerId playerId) {
        return picks.containsKey(playerId);
    }

    /** {@code null}, wenn dieser Spieler in dieser Runde noch nicht getippt hat. */
    public @Nullable Pick pickOf(PlayerId playerId) {
        return picks.get(playerId);
    }

    /**
     * Teilnehmer, die nicht getippt haben (Anforderung 8.1) — nur sie zahlen
     * die Strafe. Der Teilnehmerkreis ist beim Oeffnen eingefroren, wer
     * spaeter dazukommt, ist nicht dabei.
     */
    public Set<PlayerId> nonPickers() {
        Set<PlayerId> ohneTipp = new LinkedHashSet<>(participants);
        ohneTipp.removeAll(picks.keySet());
        return ohneTipp;
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

    /** {@code null} vor RESOLVED. */
    public @Nullable OutcomeId getWinningOutcomeId() {
        return winningOutcomeId;
    }

    void setWinningOutcomeId(@Nullable OutcomeId winningOutcomeId) {
        this.winningOutcomeId = winningOutcomeId;
    }

    /** {@code null} vor RESOLVED. */
    public @Nullable Map<PlayerId, PointsDelta> getDeltas() {
        Map<PlayerId, PointsDelta> current = deltas;
        return current == null ? null : Map.copyOf(current);
    }

    void setDeltas(Map<PlayerId, PointsDelta> deltas) {
        this.deltas = deltas;
    }

    public Points getPool() {
        return pool;
    }

    void setPool(Points pool) {
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
