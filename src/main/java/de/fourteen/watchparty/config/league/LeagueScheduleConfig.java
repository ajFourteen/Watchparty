package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.adapter.out.feed.EspnScheduleFeed;
import de.fourteen.watchparty.application.league.ScheduleSyncService;
import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.ScheduleFeed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verdrahtet den Spielplan-Abgleich (ADR-037): {@link EspnScheduleFeed} und
 * {@link ScheduleSyncService}.
 *
 * Kein selbst nachplanender Job mehr (ADR-037-Nachtrag vom 2026-08-18): ESPN
 * blockiert Zugriffe aus Fly.ios IP-Bereich mit 403 (Akamai). Ein taeglicher
 * GitHub-Actions-Workflow (.github/workflows/schedule-relay.yml) ruft den
 * Feed stattdessen von dort ab und liefert die rohe Antwort an
 * {@code ScheduleController}s Relay-Endpunkt, der sie ueber {@link
 * ScheduleCommands#ingestRelayedFeed} einspeist — dieselbe Abgleich- und
 * Merge-Logik wie zuvor, nur ohne die eigene, blockierte Netzwerkverbindung.
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
}
