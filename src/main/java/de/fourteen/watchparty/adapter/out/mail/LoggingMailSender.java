package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.domain.model.league.LoginLink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schreibt den Anmeldelink strukturiert ins Log statt ihn zu versenden.
 *
 * Bewusste Zwischenstufe (Rueckfrage vom 2026-08-17): Ein echter
 * Mailversand-Anbieter braucht ein Konto, Zugangsdaten und einen Vertrag —
 * das ist eine betriebliche Entscheidung fuer Stufe 8, kein Adapter, der
 * sich am Schreibtisch bauen laesst. Der Port {@link MailSender} ist davon
 * unberuehrt: Ein spaeterer Adapter (z. B. SMTP) tauscht nur diese Klasse
 * aus, nichts, was von ihr abhaengt.
 */
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    private final String baseUrl;

    public LoggingMailSender(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendLoginLink(LoginLink link) {
        String url = baseUrl + "/login/" + link.getToken().value();
        log.info("Anmeldelink fuer {} (verfaellt {}): {}", link.getEmail(), link.getExpiresAt(), url);
    }
}
