package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.port.in.ReportMailCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.application.league.port.out.MatchdayCompletionListener;
import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;

import java.util.NoSuchElementException;

/**
 * Setzt {@link ReportMailCommands} um und haengt als {@link
 * MatchdayCompletionListener} an {@code ScheduleSyncService} (13.9-n/o/p,
 * ADR-041). Der Mailinhalt selbst ist unveraendert {@link
 * PredictionCommands#matchdayReport} -- dieselbe Berechnung wie die Seite
 * zum Abruf (13.9-a), hier nur an jedes opted-in Konto verschickt statt auf
 * Anfrage zurueckgegeben.
 */
public class ReportMailService implements ReportMailCommands, MatchdayCompletionListener {

    private final AccountRepository accounts;
    private final PredictionCommands predictions;
    private final MailSender mailSender;

    public ReportMailService(AccountRepository accounts, PredictionCommands predictions, MailSender mailSender) {
        this.accounts = accounts;
        this.predictions = predictions;
        this.mailSender = mailSender;
    }

    @Override
    public void optIn(EmailAddress email) {
        Account account = accounts.findByEmail(email).orElseThrow(() -> new NoSuchElementException("Unbekanntes Konto: " + email));
        account.optIntoReportMail();
        accounts.save(account);
    }

    @Override
    public void optOut(EmailAddress email) {
        Account account = accounts.findByEmail(email).orElseThrow(() -> new NoSuchElementException("Unbekanntes Konto: " + email));
        account.optOutOfReportMail();
        accounts.save(account);
    }

    @Override
    public void unsubscribe(ReportMailToken token) {
        accounts.findByReportMailToken(token).ifPresent(account -> {
            account.optOutOfReportMail();
            accounts.save(account);
        });
    }

    @Override
    public void onMatchdayCompleted(Matchday matchday) {
        for (Account account : accounts.findOptedIntoReportMail()) {
            ReportView.MatchdayReportView report = predictions.matchdayReport(account.getEmail(), matchday);
            mailSender.sendMatchdayReport(account, report);
        }
    }
}
