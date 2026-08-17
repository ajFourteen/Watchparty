package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/** Das Kürzel einer Mannschaft (z. B. "KC"), wie es der Feed liefert (ADR-037). Value Object. */
@ValueObject
public record TeamId(String value) {

    public TeamId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ein Mannschafts-Kürzel ist nie leer");
        }
    }

    public static TeamId of(String value) {
        return new TeamId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
