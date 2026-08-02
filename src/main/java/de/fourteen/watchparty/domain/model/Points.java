package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Punkte — die Waehrung des Spiels (Anforderung 3). Value Object.
 *
 * Deckt jede Rolle ab, in der echte Punkte auftreten: Kontostand, Einsatz,
 * Pool und Strafe. Das sind Rollen derselben Groesse, keine verschiedenen
 * Groessen — sie tragen deshalb denselben Typ und werden ueber den
 * Feldnamen unterschieden. Anders als {@link Share}: Anteile sind eine
 * andere Einheit und duerfen mit Punkten gar nicht erst verwechselbar sein
 * (Anforderung 7).
 *
 * <b>Nie negativ.</b> Invariante 5 sagt, dass kein Konto negativ wird; hier
 * ist das kein Kommentar, sondern eine Bedingung am Typ. Eine Subtraktion,
 * die darunter fiele, wirft — wer kappen will, sagt das mit {@link #min}.
 * Ganzzahlig, nie Fliesskomma.
 */
@ValueObject
public record Points(int value) implements Comparable<Points> {

    public static final Points ZERO = new Points(0);

    public Points {
        if (value < 0) {
            throw new IllegalArgumentException("Punkte sind nie negativ (Invariante 5), waren: " + value);
        }
    }

    public static Points of(int value) {
        return new Points(value);
    }

    public Points plus(Points other) {
        return new Points(value + other.value);
    }

    /** Wirft, wenn das Ergebnis negativ waere — dann stimmt die Rechnung nicht. */
    public Points minus(Points other) {
        return new Points(value - other.value);
    }

    /** Das Kappen aus Anforderung 8.1: eingesammelt wird {@code min(Strafe, Kontostand)}. */
    public Points min(Points other) {
        return value <= other.value ? this : other;
    }

    public boolean isLessThan(Points other) {
        return value < other.value;
    }

    public boolean isZero() {
        return value == 0;
    }

    /** Verbucht eine Veraenderung. Wirft, wenn das Konto darunter negativ wuerde. */
    public Points apply(PointsDelta delta) {
        return new Points(value + delta.value());
    }

    @Override
    public int compareTo(Points other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return value + " Punkte";
    }
}
