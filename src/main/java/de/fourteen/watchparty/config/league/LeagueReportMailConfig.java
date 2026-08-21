package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.application.league.ReportMailService;
import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.MailSender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verdrahtet den Mailversand des Spieltags-Reports (13.9-n/o/p, ADR-041):
 * ein einziger {@link ReportMailService}-Bean, der sowohl {@code
 * ReportMailCommands} (Opt-in/Opt-out/Abmeldung, gebraucht von {@code
 * ReportMailController}) als auch {@code MatchdayCompletionListener}
 * (gebraucht von {@code LeagueScheduleConfig}) bedient — Spring loest beide
 * Injektionspunkte ueber denselben Bean auf, keine zwei Beans fuer eine
 * Verantwortlichkeit.
 *
 * Dieselbe Bedingung wie {@link LeagueDatabaseConfig}: Der Mailversand
 * haengt an Konten und Tipps, es gibt keinen Grund, ihn unabhaengig von der
 * Datenbank abzuschalten (Kriterium 37).
 */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
public class LeagueReportMailConfig {

    @Bean
    public ReportMailService reportMailService(AccountRepository accountRepository,
            PredictionCommands predictionCommands, MailSender mailSender) {
        return new ReportMailService(accountRepository, predictionCommands, mailSender);
    }
}
