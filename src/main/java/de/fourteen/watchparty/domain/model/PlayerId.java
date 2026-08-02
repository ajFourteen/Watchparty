package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Die Identitaet eines {@link Player}. Value Object.
 *
 * Eigener Typ statt {@code String}, weil im Umlauf mehrere Zeichenketten
 * sind, die sich zum Verwechseln aehneln: Spieler-ID, Token, Sitzungs-ID,
 * Ausgangs-ID. Vertauscht man zwei davon, kompiliert es jetzt nicht mehr.
 */
@ValueObject
public record PlayerId(String value) {

    public PlayerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Eine Spieler-ID ist nie leer");
        }
    }

    public static PlayerId of(String value) {
        return new PlayerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
