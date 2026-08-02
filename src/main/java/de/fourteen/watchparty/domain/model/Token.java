package de.fourteen.watchparty.domain.model;

/**
 * Das Wiedererkennungsmerkmal eines Spielers ueber Verbindungsabbrueche
 * hinweg (ADR-014). Value Object.
 *
 * Liegt im localStorage des Handys und ist damit das Einzige, was einen
 * zurueckkehrenden Spieler mit seinem Konto verbindet. Eigener Typ, damit er
 * nicht versehentlich als {@link PlayerId} durchgereicht wird — die beiden
 * sind gleich aufgebaut und duerfen nie verwechselt werden.
 */
public record Token(String value) {

    public Token {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ein Token ist nie leer");
        }
    }

    public static Token of(String value) {
        return new Token(value);
    }

    /** Fuer Eingaben von aussen, die auch fehlen duerfen: der erste Beitritt hat noch keinen. */
    public static Token ofNullable(String value) {
        return value == null || value.isBlank() ? null : new Token(value);
    }

    @Override
    public String toString() {
        // Nicht der Wert: Ein Token im Log waere ein fremdes Konto.
        return "Token(...)";
    }
}
