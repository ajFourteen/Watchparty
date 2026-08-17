package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

import de.fourteen.watchparty.criticality.Criticality;

/**
 * Ein Ergebnis im American Football: Punkte der Heim- und der
 * Gastmannschaft. Value Object.
 *
 * Traegt sowohl einen abgegebenen Ergebnistipp als auch ein Endergebnis —
 * beide haben dieselbe Form, zwei nicht-negative ganze Zahlen (13.4-a). Die
 * Wertung ({@link de.fourteen.watchparty.domain.service.league.Scoring})
 * braucht von beiden nur {@link #tendency()} und {@link #margin()} —
 * Mutation Score ≥ 99 % nach ADR-038 gilt fuer beide Methoden.
 */
@ValueObject
@Criticality(level = Criticality.Level.HIGH, requirements = { "13.5-b", "13.5-c" })
public record GameScore(int home, int away) {

    public GameScore {
        if (home < 0 || away < 0) {
            throw new IllegalArgumentException("Ein Ergebnis ist nie negativ, war: " + home + ":" + away);
        }
    }

    public static GameScore of(int home, int away) {
        return new GameScore(home, away);
    }

    public Tendency tendency() {
        if (home > away) {
            return Tendency.HEIM;
        }
        if (away > home) {
            return Tendency.GAST;
        }
        return Tendency.UNENTSCHIEDEN;
    }

    public int margin() {
        return Math.abs(home - away);
    }

    @Override
    public String toString() {
        return home + ":" + away;
    }
}
