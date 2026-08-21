package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLink;

import java.util.ArrayList;
import java.util.List;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito): zeichnet jeden "Versand" auf, statt ihn auszufuehren. */
public class RecordingMailSender implements MailSender {

    private final List<LoginLink> gesendet = new ArrayList<>();
    private final List<ReportView.MatchdayReportView> gesendeteReports = new ArrayList<>();
    private final List<EmailAddress> gesendeteReportsEmpfaenger = new ArrayList<>();

    @Override
    public void sendLoginLink(LoginLink link) {
        gesendet.add(link);
    }

    @Override
    public void sendMatchdayReport(Account account, ReportView.MatchdayReportView report) {
        gesendeteReportsEmpfaenger.add(account.getEmail());
        gesendeteReports.add(report);
    }

    public List<LoginLink> gesendeteLinks() {
        return List.copyOf(gesendet);
    }

    public List<LoginLink> gesendeteLinksAn(EmailAddress email) {
        return gesendet.stream().filter(link -> link.getEmail().equals(email)).toList();
    }

    public int anzahlGesendet() {
        return gesendet.size();
    }

    public List<ReportView.MatchdayReportView> gesendeteReportsAn(EmailAddress email) {
        List<ReportView.MatchdayReportView> gefunden = new ArrayList<>();
        for (int i = 0; i < gesendeteReports.size(); i++) {
            if (gesendeteReportsEmpfaenger.get(i).equals(email)) {
                gefunden.add(gesendeteReports.get(i));
            }
        }
        return gefunden;
    }
}
