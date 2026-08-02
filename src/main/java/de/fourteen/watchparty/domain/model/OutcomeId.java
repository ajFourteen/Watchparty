package de.fourteen.watchparty.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Die Identitaet eines {@link Outcome} innerhalb einer Wette. Value Object.
 *
 * Taucht an zwei Stellen auf, die sich nicht vertun duerfen: im Tipp eines
 * Spielers und im tatsaechlichen Ausgang, den der Host aufloest.
 */
public record OutcomeId(String value) {

    public OutcomeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Eine Ausgangs-ID ist nie leer");
        }
    }

    public static OutcomeId of(String value) {
        return new OutcomeId(value);
    }

    public static @Nullable OutcomeId ofNullable(@Nullable String value) {
        return value == null || value.isBlank() ? null : new OutcomeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
