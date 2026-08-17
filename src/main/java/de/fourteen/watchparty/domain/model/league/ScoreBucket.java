package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

import de.fourteen.watchparty.criticality.Criticality;

/**
 * Die vier Abstands-Eimer eines {@link GameScore} (13.5-c, ADR-038). Value
 * Object.
 *
 * Acht ist die groesste Differenz, die ein einzelner Drive noch ausgleicht
 * (Touchdown plus Two-Point) — deshalb diese Grenzen und keine glatten
 * Zehner. Bei einem Unentschieden (Abstand 0) fallen Tendenz und Abstand
 * zusammen, deshalb ein eigener Eimer statt einer Sonderbehandlung von
 * {@code EIN_SCORE}. Die Eimergrenzen sind die wahrscheinlichste Stelle
 * fuer Off-by-one, deshalb Mutation Score ≥ 99 % nach ADR-038.
 */
@ValueObject
@Criticality(level = Criticality.Level.HIGH, requirements = { "13.5-c" })
public enum ScoreBucket {
    UNENTSCHIEDEN, EIN_SCORE, ZWEI_SCORE, DREI_PLUS_SCORE;

    public static ScoreBucket of(int margin) {
        if (margin < 0) {
            throw new IllegalArgumentException("Ein Abstand ist nie negativ, war: " + margin);
        }
        if (margin == 0) {
            return UNENTSCHIEDEN;
        }
        if (margin <= 8) {
            return EIN_SCORE;
        }
        if (margin <= 16) {
            return ZWEI_SCORE;
        }
        return DREI_PLUS_SCORE;
    }
}
