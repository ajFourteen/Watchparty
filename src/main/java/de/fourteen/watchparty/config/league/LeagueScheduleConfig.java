package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.adapter.out.feed.EspnScheduleFeed;
import de.fourteen.watchparty.application.league.ScheduleSyncJob;
import de.fourteen.watchparty.application.league.ScheduleSyncService;
import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.ScheduleFeed;
import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Verdrahtet den Spielplan-Abgleich (ADR-037): {@link EspnScheduleFeed},
 * {@link ScheduleSyncService} und den selbst nachplanenden {@link
 * ScheduleSyncJob} ueber den geteilten {@link Scheduler}-Port.
 *
 * {@code @ConditionalOnProperty} auf {@code watchparty.league.schedule.season-year}:
 * Fehlt die Saison, gibt es nichts abzugleichen — kein impliziter Standard,
 * der irgendwann die falsche Saison zieht. Braucht zusaetzlich {@link
 * GameRepository} aus {@link LeagueDatabaseConfig} (also indirekt
 * {@code watchparty.league.db.url}); fehlt die Datenbank trotz gesetzter
 * Saison, scheitert der Start mit einer eindeutigen Fehlermeldung statt
 * eines still falschen Verhaltens.
 */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.schedule", name = "season-year")
public class LeagueScheduleConfig {

    @Bean
    public ScheduleFeed scheduleFeed(@Value("${watchparty.league.feed.base-url:https://site.api.espn.com}") String baseUrl) {
        return new EspnScheduleFeed(baseUrl);
    }

    @Bean
    public ScheduleCommands scheduleCommands(ScheduleFeed scheduleFeed, GameRepository gameRepository) {
        return new ScheduleSyncService(scheduleFeed, gameRepository);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ScheduleSyncJob scheduleSyncJob(ScheduleCommands scheduleCommands, Scheduler scheduler,
            @Value("${watchparty.league.schedule.season-year}") int seasonYear,
            @Value("${watchparty.league.schedule.sync-interval-minutes:15}") long syncIntervalMinutes) {
        return new ScheduleSyncJob(scheduleCommands, scheduler, SeasonId.of(seasonYear),
                Duration.ofMinutes(syncIntervalMinutes));
    }
}
