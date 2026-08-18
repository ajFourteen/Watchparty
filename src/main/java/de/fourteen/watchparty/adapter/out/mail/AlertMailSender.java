package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.application.league.port.out.AlertSender;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Benachrichtigt den Betreiber per SMTP bei einem andauernden Feed-Ausfall
 * (docs/betrieb-tippspiel.md).
 *
 * {@link #feedUnreachable} ruft von {@code ScheduleSyncJob} aus auf, das auf
 * demselben geteilten Scheduler-Thread laeuft wie das Auto-Close bei den
 * Live-Wetten (CLAUDE.md, Invariante 2) — der darf nicht auf einen
 * SMTP-Versand warten. Deshalb ein eigener Thread, dieselbe Begruendung wie
 * der Schreib-Thread in {@code SnapshotStore}: {@link #feedUnreachable}
 * reiht nur ein und kehrt sofort zurueck.
 */
public class AlertMailSender implements AlertSender {

    private static final Logger log = LoggerFactory.getLogger(AlertMailSender.class);

    private final JavaMailSenderImpl delegate;
    private final String from;
    private final String alertEmail;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "alert-mail-sender");
        thread.setDaemon(true);
        return thread;
    });

    public AlertMailSender(String host, int port, String username, String password, String from, String alertEmail) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        this.delegate = sender;
        this.from = from;
        this.alertEmail = alertEmail;
    }

    @Override
    public void feedUnreachable(SeasonId season, int consecutiveFailedRuns) {
        executor.submit(() -> send(season, consecutiveFailedRuns));
    }

    private void send(SeasonId season, int consecutiveFailedRuns) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(alertEmail);
        message.setSubject("Tippspiel: ESPN-Feed nicht erreichbar");
        message.setText("""
                Der Spielplan-Abgleich fuer die Saison %d ist %d Mal in Folge fehlgeschlagen.

                Der zuletzt bekannte Stand bleibt unangetastet (Kriterium 11), aber der Feed \
                liefert seit einiger Zeit nichts Neues mehr. Bitte pruefen und, falls noetig, \
                betroffene Ergebnisse ueber den Handeintrag-Endpunkt von Hand setzen.
                """.formatted(season.year(), consecutiveFailedRuns));
        try {
            delegate.send(message);
        } catch (RuntimeException e) {
            log.error("Alarm-Mail fuer Feed-Ausfall (Saison {}) konnte nicht versendet werden", season, e);
        }
    }
}
