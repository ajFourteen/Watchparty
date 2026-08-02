package de.fourteen.watchparty.domain.model;

/**
 * Die Wett-Parameter aus Anforderung 3.1, an einer Stelle im Code statt
 * verstreut. Value Object.
 *
 * Die konkreten Werte gelten als vorlaeufig, bis sie an einem echten
 * Spielabend gegen das Spielgefuehl geprueft sind (docs/probelauf.md).
 */
public record Params(Points minStake, Points penalty) {

    public static final Params DEFAULT = new Params(Points.of(25), Points.of(25));

    public Params {
        if (minStake == null || penalty == null) {
            throw new IllegalArgumentException("Mindesteinsatz und Strafe muessen gesetzt sein");
        }
    }
}
