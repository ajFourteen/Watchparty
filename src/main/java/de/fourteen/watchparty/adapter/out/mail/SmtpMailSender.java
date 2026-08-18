package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.domain.model.league.LoginLink;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Versendet den Anmeldelink per SMTP (ADR-036, Rueckfrage vom 2026-08-18:
 * Strato als Anbieter). {@link JavaMailSenderImpl} wird hier von Hand
 * gebaut statt ueber Spring Boots {@code MailSenderAutoConfiguration} —
 * derselbe Stil wie {@code HikariDataSource} fuer die Datenbank
 * ({@code LeagueDatabaseConfig}): explizite Kontrolle statt impliziter
 * Konfiguration ueber {@code spring.mail.*}.
 *
 * Klartext statt HTML: der Anmeldelink ist der einzige Inhalt, eine
 * HTML-Mail waere fuer eine einzelne Zeile mehr Zeremonie als Gewinn und
 * faellt Spamfiltern eher auf als eine schlichte Textmail.
 */
public class SmtpMailSender implements MailSender {

    private final JavaMailSenderImpl delegate;
    private final String from;
    private final String baseUrl;

    public SmtpMailSender(String host, int port, String username, String password, String from, String baseUrl) {
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
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendLoginLink(LoginLink link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(link.getEmail().value());
        message.setSubject("Dein Anmeldelink fürs Tippspiel");
        message.setText("""
                Hallo,

                mit diesem Link meldest du dich fürs Tippspiel an. Er gilt einmal und 15 Minuten:

                %s

                Hast du das nicht angefordert, kannst du diese Mail einfach ignorieren.
                """.formatted(LoginLinkUrl.of(baseUrl, link)));
        delegate.send(message);
    }
}
