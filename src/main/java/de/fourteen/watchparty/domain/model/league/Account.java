package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;

/**
 * Das Konto eines Tippers. Aggregate Root.
 *
 * Traegt in dieser Stufe nur Datenhaltung: {@link #register} legt ein neues
 * Konto an, {@link #of} baut ein bestehendes aus der Datenbank wieder auf.
 * Der Anmeldefluss (Magic Link, ADR-036) und das Loeschen (Kriterium 7)
 * kommen mit ihren eigenen Kommandos und benannten Uebergaengen erst in
 * Stufe 3 — bis dahin ist ein {@code Account} unveraenderlich.
 */
@AggregateRoot
public class Account {

    @Identity
    private final AccountId id;
    private final EmailAddress email;
    private final DisplayName displayName;
    private final Instant createdAt;

    private Account(AccountId id, EmailAddress email, DisplayName displayName, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    /** Legt ein neues Konto an, mit frisch vergebener {@link AccountId}. */
    public static Account register(EmailAddress email, DisplayName displayName, Instant createdAt) {
        return new Account(AccountId.newId(), email, displayName, createdAt);
    }

    /** Baut ein bestehendes Konto aus seinen gespeicherten Werten wieder auf. */
    public static Account of(AccountId id, EmailAddress email, DisplayName displayName, Instant createdAt) {
        return new Account(id, email, displayName, createdAt);
    }

    public AccountId getId() {
        return id;
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
}
