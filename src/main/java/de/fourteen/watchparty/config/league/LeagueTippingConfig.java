package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.application.league.PredictionService;
import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.PredictionRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Verdrahtet {@link PredictionService} als Umsetzung von {@link
 * PredictionCommands} (Kapitel 13.4). Dieselbe Bedingung wie {@link
 * LeagueDatabaseConfig}.
 */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
public class LeagueTippingConfig {

    @Bean
    public PredictionCommands predictionCommands(Clock clock, GameRepository gameRepository,
            PredictionRepository predictionRepository, AccountRepository accountRepository) {
        return new PredictionService(clock, gameRepository, predictionRepository, accountRepository);
    }
}
