package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLink;

import java.util.ArrayList;
import java.util.List;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito): zeichnet jeden "Versand" auf, statt ihn auszufuehren. */
public class RecordingMailSender implements MailSender {

    private final List<LoginLink> gesendet = new ArrayList<>();

    @Override
    public void sendLoginLink(LoginLink link) {
        gesendet.add(link);
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
}
