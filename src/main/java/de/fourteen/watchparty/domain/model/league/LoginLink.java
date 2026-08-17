package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Duration;
import java.time.Instant;

/**
 * Ein angeforderter Anmeldelink (ADR-036). Entity, Identitaet ueber den
 * {@link LoginLinkToken} selbst.
 *
 * Traegt den zum Anfragezeitpunkt mitgeschickten {@link DisplayName}: Ein
 * neues Konto braucht beim ersten Anmelden bereits einen Namen (Kriterium
 * 6), und der Anmeldelink selbst transportiert nur die E-Mail-Adresse
 * (Kriterium 1) — deshalb fragt das Anmeldeformular E-Mail und Anzeigename
 * in einem Schritt ab, und der Name reist mit dem Link. Existiert das Konto
 * bereits, wird der mitgeschickte Name beim Einloesen verworfen — ein
 * bestehender Name aendert sich nur ueber ein eigenes, ausdrueckliches
 * Kommando, nie beiher durchs Anmelden.
 */
@Entity
public class LoginLink {

    @Identity
    private final LoginLinkToken token;
    private final EmailAddress email;
    private final DisplayName displayName;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean used;

    private LoginLink(LoginLinkToken token, EmailAddress email, DisplayName displayName,
            Instant createdAt, Instant expiresAt, boolean used) {
        this.token = token;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    /** Stellt einen neuen Link aus, gueltig fuer {@code validity} ab {@code now} (Kriterium 2: 15 Minuten). */
    public static LoginLink issue(EmailAddress email, DisplayName displayName, Instant now, Duration validity) {
        return new LoginLink(LoginLinkToken.generate(), email, displayName, now, now.plus(validity), false);
    }

    /** Baut einen bestehenden Link aus seinen gespeicherten Werten wieder auf. */
    public static LoginLink of(LoginLinkToken token, EmailAddress email, DisplayName displayName,
            Instant createdAt, Instant expiresAt, boolean used) {
        return new LoginLink(token, email, displayName, createdAt, expiresAt, used);
    }

    /** Weder verbraucht noch abgelaufen (Kriterium 2). */
    public boolean isValid(Instant now) {
        return !used && now.isBefore(expiresAt);
    }

    /** Markiert den Link als verbraucht — danach meldet er niemanden mehr an. */
    public void redeem(Instant now) {
        if (!isValid(now)) {
            throw new IllegalStateException("Anmeldelink ist verbraucht oder abgelaufen");
        }
        used = true;
    }

    public LoginLinkToken getToken() {
        return token;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public DisplayName getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }
}
