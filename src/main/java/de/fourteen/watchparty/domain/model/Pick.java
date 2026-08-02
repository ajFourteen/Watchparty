package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Ein abgegebener Tipp: ein Spieler, ein Ausgang, ein Einsatz. Value Object.
 * Ein Spieler hat pro Runde hoechstens einen Pick (Anforderung 6).
 *
 * Heisst bewusst nicht {@code Bet} — das ist die Wette selbst, also die
 * Frage, auf die hier getippt wird (ADR-022).
 */
@ValueObject
public record Pick(PlayerId playerId, OutcomeId outcomeId, Points stake) {

    public Pick {
        if (playerId == null || outcomeId == null || stake == null) {
            throw new IllegalArgumentException("Ein Tipp braucht Spieler, Ausgang und Einsatz");
        }
    }

    public boolean isOn(OutcomeId outcome) {
        return outcomeId.equals(outcome);
    }

    /** Der Anteil dieses Tipps am Pool (Anforderung 7.1). */
    public Share share(Params params) {
        return Share.forStake(stake, params.minStake());
    }
}
