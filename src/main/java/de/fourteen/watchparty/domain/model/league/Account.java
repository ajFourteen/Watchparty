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
 * Traegt neben der reinen Datenhaltung zwei benannte Uebergaenge fuer den
 * Mailversand des Spieltags-Reports (13.9-n, ADR-041): {@link
 * #optIntoReportMail} und {@link #optOutOfReportMail}. Der {@link
 * #reportMailToken} entsteht mit dem Konto und bleibt ueber seine gesamte
 * Lebensdauer stabil -- ein Abmeldelink aus einer alten Mail muss auch nach
 * spaeterem erneuten Bestellen noch wirken.
 */
@AggregateRoot
public class Account {

    @Identity
    private final EmailAddress email;
    private final DisplayName displayName;
    private final Instant createdAt;
    private boolean reportMailOptIn;
    private final ReportMailToken reportMailToken;

    private Account(EmailAddress email, DisplayName displayName, Instant createdAt, boolean reportMailOptIn,
            ReportMailToken reportMailToken) {
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.reportMailOptIn = reportMailOptIn;
        this.reportMailToken = reportMailToken;
    }

    /** Legt ein neues Konto an oder baut ein bestehendes aus seinen gespeicherten Werten wieder auf. */
    public static Account of(EmailAddress email, DisplayName displayName, Instant createdAt, boolean reportMailOptIn,
            ReportMailToken reportMailToken) {
        return new Account(email, displayName, createdAt, reportMailOptIn, reportMailToken);
    }

    public void optIntoReportMail() {
        this.reportMailOptIn = true;
    }

    public void optOutOfReportMail() {
        this.reportMailOptIn = false;
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

    public boolean isReportMailOptIn() {
        return reportMailOptIn;
    }

    public ReportMailToken getReportMailToken() {
        return reportMailToken;
    }
}
