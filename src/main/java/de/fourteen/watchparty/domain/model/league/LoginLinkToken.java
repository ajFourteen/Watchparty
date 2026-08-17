package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Der geheime Teil eines Anmeldelinks (ADR-036). Value Object.
 *
 * 32 zufaellige Byte, Base64url ohne Padding: lang genug, um nicht zu
 * erraten zu sein, und URL-sicher, weil er als Pfadsegment im Link steht.
 */
@ValueObject
public record LoginLinkToken(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public LoginLinkToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ein Anmeldelink-Token ist nie leer");
        }
    }

    public static LoginLinkToken generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new LoginLinkToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public static LoginLinkToken of(String value) {
        return new LoginLinkToken(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
