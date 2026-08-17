package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

/** Der Name einer Liga, wie ihn der Verwalter beim Anlegen vergibt. Value Object. */
@ValueObject
public record LeagueName(String value) {

    public static final int MAX_LENGTH = 40;

    public LeagueName {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Liganame muss 1 bis " + MAX_LENGTH + " Zeichen haben");
        }
        value = value.trim();
    }

    public static LeagueName of(String value) {
        return new LeagueName(value);
    }

    public static boolean isValid(@Nullable String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && trimmed.length() <= MAX_LENGTH;
    }

    @Override
    public String toString() {
        return value;
    }
}
