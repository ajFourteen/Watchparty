package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

/**
 * Der Anzeigename eines Kontos, wie er in Ranglisten steht. Value Object.
 *
 * Dieselbe Regel wie {@link de.fourteen.watchparty.domain.model.PlayerName}
 * (1 bis 20 Zeichen), aber bewusst eigenstaendig implementiert statt
 * importiert — ein Anzeigename ist kein Spielername (Feature-Dokument,
 * Abschnitt "Umgesetzt in"), und die Liga kennt ohnehin keinen
 * Live-Wetten-Typ ({@code ArchitectureTest},
 * {@code ligaUndRaumcodeKennenEinanderNicht}).
 */
@ValueObject
public record DisplayName(String value) {

    public static final int MAX_LENGTH = 20;

    public DisplayName {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Anzeigename muss 1 bis " + MAX_LENGTH + " Zeichen haben");
        }
        value = value.trim();
    }

    public static DisplayName of(String value) {
        return new DisplayName(value);
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
