package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.application.league.LeagueService;
import de.fourteen.watchparty.application.league.port.in.LeagueCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.LeagueRepository;
import de.fourteen.watchparty.application.league.port.out.PredictionRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Verdrahtet {@link LeagueService} als Umsetzung von {@link LeagueCommands}
 * (Kapitel 13.6). Dieselbe Bedingung wie {@link LeagueDatabaseConfig}.
 */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
public class LeagueMembershipConfig {

    @Bean
    public LeagueCommands leagueCommands(Clock clock, LeagueRepository leagueRepository,
            GameRepository gameRepository, PredictionRepository predictionRepository,
            AccountRepository accountRepository) {
        return new LeagueService(clock, leagueRepository, gameRepository, predictionRepository, accountRepository);
    }
}
