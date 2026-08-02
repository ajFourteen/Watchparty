package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Die Identitaet einer {@link Round}. Value Object.
 *
 * Monoton steigend, damit ein verspaeteter Auto-Close-Task nicht die
 * naechste, schon wieder offene Runde schliesst (ADR-010). Genau dafuer
 * braucht es Gleichheit ueber den Wert — und die bringt ein Record mit.
 */
@ValueObject
public record RoundId(long value) {

    public RoundId {
        if (value < 1) {
            throw new IllegalArgumentException("Runden werden ab 1 gezaehlt, war: " + value);
        }
    }

    public static RoundId of(long value) {
        return new RoundId(value);
    }

    public RoundId next() {
        return new RoundId(value + 1);
    }

    @Override
    public String toString() {
        return "Runde " + value;
    }
}
