package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.adapter.out.mail.LoggingMailSender;
import de.fourteen.watchparty.adapter.out.mail.SmtpMailSender;
import de.fourteen.watchparty.application.league.port.out.MailSender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verdrahtet den Mailversand des Anmeldelinks (ADR-036). Zwei Beans um
 * dieselbe Bedingung herum: {@link SmtpMailSender} entsteht nur, wenn
 * Strato-Zugangsdaten gesetzt sind ({@code watchparty.league.mail.smtp.username}
 * als Signal, dass echt versendet werden soll); {@link LoggingMailSender}
 * springt sonst ein — {@code @ConditionalOnMissingBean} statt einer zweiten,
 * gespiegelten Bedingung, damit die beiden nie gleichzeitig oder nie greifen
 * koennen.
 *
 * Dieselbe Bedingung wie {@link LeagueDatabaseConfig} auf Klassenebene: Ohne
 * Datenbank gibt es auch keinen {@code LoginCommands}-Bean, der den
 * Mailversand ueberhaupt braucht (Kriterium 37).
 */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
public class LeagueMailConfig {

    @Bean
    @ConditionalOnProperty(prefix = "watchparty.league.mail.smtp", name = "username")
    public MailSender smtpMailSender(
            @Value("${watchparty.league.mail.smtp.host:smtp.strato.de}") String host,
            @Value("${watchparty.league.mail.smtp.port:587}") int port,
            @Value("${watchparty.league.mail.smtp.username}") String username,
            @Value("${watchparty.league.mail.smtp.password}") String password,
            @Value("${watchparty.league.mail.from:noreply@fourteen-it.de}") String from,
            @Value("${watchparty.league.login.base-url:http://localhost:5173}") String baseUrl) {
        return new SmtpMailSender(host, port, username, password, from, baseUrl);
    }

    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    public MailSender loggingMailSender(
            @Value("${watchparty.league.login.base-url:http://localhost:5173}") String baseUrl) {
        return new LoggingMailSender(baseUrl);
    }
}
