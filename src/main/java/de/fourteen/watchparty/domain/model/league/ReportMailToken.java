package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Der geheime Teil des Ein-Klick-Abmeldelinks fuer den Spieltags-Report per
 * Mail (13.9-p, ADR-041). Value Object.
 *
 * 32 zufaellige Byte, Base64url ohne Padding -- dieselbe Bauweise wie
 * {@link LoginLinkToken} und {@link SessionToken}, aber ein eigener Typ.
 * Anders als beide bleibt dieser Token dauerhaft gueltig statt einmal
 * verwendbar oder zeitlich befristet: Ein Abmeldelink in einer alten Mail
 * muss auch nach spaeterem erneuten Bestellen noch wirken (ADR-041).
 */
@ValueObject
public record ReportMailToken(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public ReportMailToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ein Abmeldelink-Token ist nie leer");
        }
    }

    public static ReportMailToken generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new ReportMailToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public static ReportMailToken of(String value) {
        return new ReportMailToken(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
