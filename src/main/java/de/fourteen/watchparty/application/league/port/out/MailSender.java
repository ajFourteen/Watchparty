package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.LoginLink;

/**
 * Ausgangs-Port fuer den Versand des Anmeldelinks (ADR-036). Spricht {@link
 * LoginLink} — ein Datenmodell der Domaene, nicht des Adapters.
 *
 * Anders als {@link de.fourteen.watchparty.application.port.out.SnapshotRepository}
 * ohne Nicht-blockierend-Zusicherung: Das Tippspiel laeuft auf
 * Request-Threads (CLAUDE.md, "Was mit den harten Invarianten passiert").
 * Ein Ausfall des Mailversands darf trotzdem keine laufende Watchparty
 * beruehren (Kriterium 37) — das ist bereits strukturell erfuellt, weil
 * dieser Port ausschliesslich vom Ligacode aufgerufen wird.
 */
public interface MailSender {

    void sendLoginLink(LoginLink link);

    /** Der Spieltags-Report per Mail (13.9-o, ADR-041) -- derselbe Inhalt wie die eigene Bilanz zum Abruf (13.9-a). */
    void sendMatchdayReport(Account account, ReportView.MatchdayReportView report);
}
