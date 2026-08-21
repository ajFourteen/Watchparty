package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.LoginLink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schreibt den Anmeldelink strukturiert ins Log statt ihn zu versenden.
 *
 * Fällt ein, solange kein echter Mailversand konfiguriert ist
 * (`watchparty.league.mail.smtp.username` fehlt, siehe {@code
 * LeagueLoginConfig}) — fürs lokale Entwickeln und für einen Betrieb ohne
 * IONOS-Zugangsdaten. Der Port {@link MailSender} ist davon unberührt:
 * {@link SmtpMailSender} tauscht nur diese Klasse aus, nichts, was von ihr
 * abhängt.
 */
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    private final String baseUrl;

    public LoggingMailSender(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendLoginLink(LoginLink link) {
        log.info("Anmeldelink fuer {} (verfaellt {}): {}", link.getEmail(), link.getExpiresAt(),
                LoginLinkUrl.of(baseUrl, link));
    }

    @Override
    public void sendMatchdayReport(Account account, ReportView.MatchdayReportView report) {
        log.info("Spieltags-Report fuer {} (Spieltag {}, {} Punkte), Abmeldelink: {}\n{}",
                account.getEmail(), report.week(), report.totalPoints(),
                ReportMailUnsubscribeUrl.of(baseUrl, account), ReportMailBody.of(report));
    }
}
