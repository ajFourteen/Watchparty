package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;

/**
 * Die Mitgliedschaft eines Kontos in einer {@link League}. Entity innerhalb
 * des Aggregats, Identitaet ueber die E-Mail-Adresse — ein Konto ist einer
 * Liga nie zweimal beigetreten.
 */
@Entity
public class Membership {

    @Identity
    private final EmailAddress accountEmail;
    private final Instant joinedAt;

    private Membership(EmailAddress accountEmail, Instant joinedAt) {
        this.accountEmail = accountEmail;
        this.joinedAt = joinedAt;
    }

    public static Membership of(EmailAddress accountEmail, Instant joinedAt) {
        return new Membership(accountEmail, joinedAt);
    }

    public EmailAddress getAccountEmail() {
        return accountEmail;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
