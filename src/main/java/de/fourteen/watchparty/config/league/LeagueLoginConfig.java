package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.adapter.out.db.AccountSessionRepositoryJdbc;
import de.fourteen.watchparty.adapter.out.db.LoginLinkRepositoryJdbc;
import de.fourteen.watchparty.adapter.out.mail.LoggingMailSender;
import de.fourteen.watchparty.adapter.out.ratelimit.InMemoryRateLimiter;
import de.fourteen.watchparty.application.league.LoginService;
import de.fourteen.watchparty.application.league.port.in.LoginCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.AccountSessionRepository;
import de.fourteen.watchparty.application.league.port.out.LoginLinkRepository;
import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.application.league.port.out.RateLimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Clock;
import java.time.Duration;

/**
 * Verdrahtet die Anmeldung (ADR-036): Repositories, Rate Limit, Mailversand
 * und {@link LoginService} als Umsetzung von {@link LoginCommands}.
 *
 * Dieselbe Bedingung wie {@link LeagueDatabaseConfig}: Ohne
 * {@code watchparty.league.db.url} entsteht hier kein einziger Bean — die
 * Anmeldung haengt an der Datenbank, es gibt keinen Grund, sie unabhaengig
 * davon abzuschalten (Kriterium 37).
 */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
public class LeagueLoginConfig {

    @Bean
    public LoginLinkRepository loginLinkRepository(NamedParameterJdbcTemplate leagueJdbcTemplate) {
        return new LoginLinkRepositoryJdbc(leagueJdbcTemplate);
    }

    @Bean
    public AccountSessionRepository accountSessionRepository(NamedParameterJdbcTemplate leagueJdbcTemplate) {
        return new AccountSessionRepositoryJdbc(leagueJdbcTemplate);
    }

    @Bean
    public MailSender mailSender(@Value("${watchparty.league.login.base-url:http://localhost:5173}") String baseUrl) {
        return new LoggingMailSender(baseUrl);
    }

    @Bean
    public RateLimiter rateLimiter(
            @Value("${watchparty.league.login.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${watchparty.league.login.rate-limit.window-minutes:15}") long windowMinutes) {
        return new InMemoryRateLimiter(maxAttempts, Duration.ofMinutes(windowMinutes));
    }

    @Bean
    public LoginCommands loginCommands(Clock clock, AccountRepository accountRepository,
            LoginLinkRepository loginLinkRepository, AccountSessionRepository accountSessionRepository,
            MailSender mailSender, RateLimiter rateLimiter) {
        return new LoginService(clock, accountRepository, loginLinkRepository, accountSessionRepository,
                mailSender, rateLimiter);
    }
}
