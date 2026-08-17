package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;

/**
 * Das Konto eines Tippers. Aggregate Root.
 *
 * Die E-Mail-Adresse selbst ist die Identitaet — kein zusaetzliches,
 * zufaellig erzeugtes {@code AccountId}: Ein Konto ohne diese Indirektion
 * ist ein Feld und ein Index weniger, und die Adresse ist ohnehin bereits
 * eindeutig (Kriterium 1). Trifft die Zusage, so wenig personenbezogene
 * Daten wie moeglich zu halten (Rueckfrage vom 2026-08-17).
 *
 * Traegt in dieser Stufe nur Datenhaltung. Der Anmeldefluss (Magic Link,
 * ADR-036) und das Loeschen (Kriterium 7) kommen mit ihren eigenen
 * Kommandos und benannten Uebergaengen erst in Stufe 3 — bis dahin ist ein
 * {@code Account} unveraenderlich.
 */
@AggregateRoot
public class Account {

    @Identity
    private final EmailAddress email;
    private final DisplayName displayName;
    private final Instant createdAt;

    private Account(EmailAddress email, DisplayName displayName, Instant createdAt) {
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    /** Legt ein neues Konto an oder baut ein bestehendes aus seinen gespeicherten Werten wieder auf. */
    public static Account of(EmailAddress email, DisplayName displayName, Instant createdAt) {
        return new Account(email, displayName, createdAt);
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
