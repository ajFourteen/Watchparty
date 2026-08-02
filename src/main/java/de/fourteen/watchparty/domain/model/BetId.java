package de.fourteen.watchparty.domain.model;

/** Die Identitaet einer {@link Bet} im Katalog (ADR-017). Value Object. */
public record BetId(String value) {

    public BetId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Eine Wetten-ID ist nie leer");
        }
    }

    public static BetId of(String value) {
        return new BetId(value);
    }

    /** Fuer Eingaben von aussen: ohne Angabe gilt die Standard-Wette. */
    public static BetId ofNullable(String value) {
        return value == null || value.isBlank() ? null : new BetId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
