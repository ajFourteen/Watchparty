package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Ein Anteil am Pool. Value Object.
 *
 * Anforderung 7 trennt ausdruecklich zwei Dinge: die <em>echten Punkte</em>
 * im Pool und die <em>Anteile</em>, nach denen er verteilt wird. Wer 10
 * setzt, zahlt 10 Punkte ein, zaehlt beim Verteilen aber mit dem
 * Mindesteinsatz als Anteil (7.1). Beides in {@code int} auszudruecken hat
 * genau eine Konsequenz: Man kann sie versehentlich addieren.
 *
 * Deshalb ein eigener Typ. Er hat bewusst keine Umrechnung nach
 * {@link Points} — die Bruecke ist die Verteilung selbst, nicht eine
 * Konvertierung.
 */
@ValueObject
public record Share(int value) {

    public Share {
        if (value < 0) {
            throw new IllegalArgumentException("Ein Anteil ist nie negativ, war: " + value);
        }
    }

    public static Share of(int value) {
        return new Share(value);
    }

    /** Der Anteil eines Tipps: {@code max(Einsatz, Mindesteinsatz)} (Anforderung 7.1). */
    public static Share forStake(Points stake, Points minStake) {
        return new Share(Math.max(stake.value(), minStake.value()));
    }

    public Share plus(Share other) {
        return new Share(value + other.value);
    }

    public boolean isZero() {
        return value == 0;
    }

    @Override
    public String toString() {
        return value + " Anteile";
    }
}
