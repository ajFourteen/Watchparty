package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;

import java.util.List;
import java.util.Optional;

/**
 * Ausgangs-Port fuer Konten. Spricht {@link Account} — ein Datenmodell der
 * Domaene, nicht des Adapters. Deshalb zeigt die Abhaengigkeit vom
 * Datenbank-Adapter nach innen und nicht umgekehrt (ADR-024).
 *
 * Anders als {@link de.fourteen.watchparty.application.port.out.SnapshotRepository}
 * traegt {@link #save} keine Nicht-blockierend-Zusicherung: Das Tippspiel
 * laeuft auf Request-Threads, nicht auf dem Raum-Thread (CLAUDE.md, "Was mit
 * den harten Invarianten passiert").
 */
public interface AccountRepository {

    void save(Account account);

    Optional<Account> findByEmail(EmailAddress email);

    /** Kriterium 7: Adresse und Anzeigename sind danach fort. */
    void delete(EmailAddress email);

    /** Alle Konten mit aktivem Mailversand-Opt-in (13.9-n) -- die Empfaenger eines Spieltags-Reports. */
    List<Account> findOptedIntoReportMail();

    /** Fuer den Abmeldelink (13.9-p): der Token steht im Link, nicht die E-Mail-Adresse. */
    Optional<Account> findByReportMailToken(ReportMailToken token);
}
