package de.fourteen.watchparty.config.league;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.fourteen.watchparty.adapter.out.db.AccountRepositoryJdbc;
import de.fourteen.watchparty.adapter.out.db.GameRepositoryJdbc;
import de.fourteen.watchparty.adapter.out.db.PredictionRepositoryJdbc;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.PredictionRepository;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Verdrahtet die Postgres-Anbindung des Tippspiels (ADR-035): eigene,
 * manuelle Beans statt Spring-Boot-Autoconfiguration (die fuer DataSource
 * und Flyway in {@code WatchpartyApplication} ausgeschaltet ist), im selben
 * Stil wie {@code RoomConfig}/{@code SnapshotConfig}.
 *
 * {@code @ConditionalOnProperty} auf {@code watchparty.league.db.url}: Fehlt
 * die Eigenschaft, entsteht in dieser Klasse kein einziger Bean — das
 * Tippspiel bleibt dann funktionslos, aber die Anwendung startet trotzdem
 * (Kriterium 37, "der Ausfall der Datenbank haelt keine Watchparty an"). Das
 * ist zugleich die Voreinstellung fuer lokale Entwicklung und Tests der
 * Live-Wetten, die keine Datenbank kennen.
 */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
public class LeagueDatabaseConfig {

    @Bean(destroyMethod = "close")
    public DataSource leagueDataSource(
            @Value("${watchparty.league.db.url}") String url,
            @Value("${watchparty.league.db.username:}") String username,
            @Value("${watchparty.league.db.password:}") String password) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        if (!username.isBlank()) {
            hikariConfig.setUsername(username);
        }
        if (!password.isBlank()) {
            hikariConfig.setPassword(password);
        }
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/league/migration")
                .load()
                .migrate();

        return dataSource;
    }

    @Bean
    public NamedParameterJdbcTemplate leagueJdbcTemplate(DataSource leagueDataSource) {
        return new NamedParameterJdbcTemplate(leagueDataSource);
    }

    @Bean
    public AccountRepository accountRepository(NamedParameterJdbcTemplate leagueJdbcTemplate) {
        return new AccountRepositoryJdbc(leagueJdbcTemplate);
    }

    @Bean
    public GameRepository gameRepository(NamedParameterJdbcTemplate leagueJdbcTemplate) {
        return new GameRepositoryJdbc(leagueJdbcTemplate);
    }

    @Bean
    public PredictionRepository predictionRepository(NamedParameterJdbcTemplate leagueJdbcTemplate) {
        return new PredictionRepositoryJdbc(leagueJdbcTemplate);
    }
}
