package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Duration;
import java.time.Instant;

/**
 * Eine angemeldete Sitzung (Kriterium 5): haelt 90 Tage, damit niemand sich
 * innerhalb einer Saison woechentlich neu anmelden muss (ADR-036). Entity,
 * Identitaet ueber den {@link SessionToken}.
 */
@Entity
public class AccountSession {

    @Identity
    private final SessionToken token;
    private final EmailAddress accountEmail;
    private final Instant createdAt;
    private final Instant expiresAt;

    private AccountSession(SessionToken token, EmailAddress accountEmail, Instant createdAt, Instant expiresAt) {
        this.token = token;
        this.accountEmail = accountEmail;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** Beginnt eine neue Sitzung fuer das Konto, gueltig fuer {@code validity} ab {@code now}. */
    public static AccountSession start(EmailAddress accountEmail, Instant now, Duration validity) {
        return new AccountSession(SessionToken.generate(), accountEmail, now, now.plus(validity));
    }

    /** Baut eine bestehende Sitzung aus ihren gespeicherten Werten wieder auf. */
    public static AccountSession of(SessionToken token, EmailAddress accountEmail, Instant createdAt, Instant expiresAt) {
        return new AccountSession(token, accountEmail, createdAt, expiresAt);
    }

    public boolean isValid(Instant now) {
        return now.isBefore(expiresAt);
    }

    public SessionToken getToken() {
        return token;
    }

    public EmailAddress getAccountEmail() {
        return accountEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
