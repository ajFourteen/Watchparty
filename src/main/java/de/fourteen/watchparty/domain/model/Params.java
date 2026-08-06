package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Die Wett-Parameter aus Anforderung 3.1, an einer Stelle im Code statt
 * verstreut (3.1-a) -- auch das Startguthaben, urspruenglich als eigene
 * Konstante in {@code Room} verstreut, bis das beim Nachruesten der
 * Feature-Abdeckung auffiel (docs/offene-entscheidungen.md). Value Object.
 *
 * Die konkreten Werte gelten als vorlaeufig, bis sie an einem echten
 * Spielabend gegen das Spielgefuehl geprueft sind (docs/probelauf.md).
 */
@ValueObject
public record Params(Points startingPoints, Points minStake, Points penalty) {

    public static final Params DEFAULT = new Params(Points.of(1000), Points.of(25), Points.of(25));

    public Params {
        if (startingPoints == null || minStake == null || penalty == null) {
            throw new IllegalArgumentException("Startguthaben, Mindesteinsatz und Strafe muessen gesetzt sein");
        }
    }
}
