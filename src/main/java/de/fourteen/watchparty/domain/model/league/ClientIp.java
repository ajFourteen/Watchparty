package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Die Absender-Adresse einer Anmeldeanfrage, fuer das Rate Limit je IP
 * (Kriterium 4). Value Object statt eines rohen {@code String}, damit sie
 * sich nicht mit einer {@link EmailAddress} vertauschen laesst — beide sind
 * an derselben Stelle im Anmeldefluss im Umlauf.
 *
 * Bewusst ohne Formatpruefung (IPv4/IPv6): Woher der Wert stammt (ein
 * HTTP-Adapter, der die tatsaechliche Absenderadresse traegt), entscheidet
 * nicht diese Stufe.
 */
@ValueObject
public record ClientIp(String value) {

    public ClientIp {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Eine Absender-Adresse ist nie leer");
        }
    }

    public static ClientIp of(String value) {
        return new ClientIp(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
