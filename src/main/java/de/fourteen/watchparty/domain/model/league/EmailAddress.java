package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Die E-Mail-Adresse eines Kontos. Value Object.
 *
 * Normalisiert auf Kleinschreibung (Feature-Dokument, Abschnitt "Umgesetzt
 * in"): {@code Anna@Example.org} und {@code anna@example.org} sind dasselbe
 * Konto, nicht zwei. Die Pruefung ist bewusst schlicht — ein einziges
 * {@code @} mit Text davor und danach, keine Zeichen aus dem Umlauf — statt
 * eines RFC-vollstaendigen Musters, dessen einziger Zweck ohnehin nur die
 * Zustellung des Anmeldelinks ist (ADR-036), nicht die Adresse selbst.
 */
@ValueObject
public record EmailAddress(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public EmailAddress {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Keine gueltige E-Mail-Adresse: " + value);
        }
        value = value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    public static boolean isValid(@Nullable String value) {
        if (value == null) {
            return false;
        }
        return FORMAT.matcher(value.trim()).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
