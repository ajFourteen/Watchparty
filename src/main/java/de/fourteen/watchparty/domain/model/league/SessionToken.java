package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Der geheime Teil einer Anmeldung (Kriterium 5). Value Object.
 *
 * 32 zufaellige Byte, Base64url ohne Padding — dieselbe Bauweise wie {@link
 * LoginLinkToken}, aber ein eigener Typ: Ein Sitzungs-Token und ein
 * Anmeldelink-Token duerfen sich nie versehentlich vertauschen lassen,
 * genau wie {@code PlayerId} und {@code RoundId} auf der Live-Wetten-Seite.
 */
@ValueObject
public record SessionToken(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public SessionToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ein Sitzungs-Token ist nie leer");
        }
    }

    public static SessionToken generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new SessionToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public static SessionToken of(String value) {
        return new SessionToken(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
