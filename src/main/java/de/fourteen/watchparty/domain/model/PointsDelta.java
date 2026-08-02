package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Collection;

/**
 * Die Veraenderung eines Kontostands bei der Abrechnung — Gewinn oder
 * Verlust. Value Object.
 *
 * Eigener Typ neben {@link Points}, weil er das Gegenteil aussagt: Punkte
 * sind nie negativ, ein Delta darf und muss es sein. Ohne die Trennung
 * muesste {@code Points} negative Werte zulassen und verlöre genau die
 * Bedingung, die Invariante 5 traegt.
 */
@ValueObject
public record PointsDelta(int value) {

    public static final PointsDelta NONE = new PointsDelta(0);

    public static PointsDelta of(int value) {
        return new PointsDelta(value);
    }

    /** Zufluss: eine Auszahlung oder ein zurueckgegebener Einsatz. */
    public static PointsDelta gain(Points points) {
        return new PointsDelta(points.value());
    }

    /** Abfluss: ein Einsatz, der in den Pool wandert, oder eine Strafe. */
    public static PointsDelta loss(Points points) {
        return new PointsDelta(-points.value());
    }

    public PointsDelta plus(PointsDelta other) {
        return new PointsDelta(value + other.value);
    }

    public boolean isZero() {
        return value == 0;
    }

    /**
     * Invariante 5 in einer Zeile: Die Summe aller Deltas einer Runde ist
     * exakt null, Punkte entstehen und verschwinden nie.
     */
    public static boolean sumIsZero(Collection<PointsDelta> deltas) {
        return deltas.stream().mapToInt(PointsDelta::value).sum() == 0;
    }

    @Override
    public String toString() {
        return (value > 0 ? "+" : "") + value;
    }
}
