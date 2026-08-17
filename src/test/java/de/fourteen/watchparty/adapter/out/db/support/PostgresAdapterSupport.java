package de.fourteen.watchparty.adapter.out.db.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

/**
 * Gemeinsame Grundlage fuer alle {@code adapter/out/db}-Tests: ein
 * einziger, beim Klassenladen einmalig gestarteter Postgres-Container fuer
 * den gesamten {@code adapterTest}-Lauf statt einem je Testklasse
 * (10-Minuten-Budget, docs/teststrategie.md Abschnitt 10). Migriert einmal,
 * danach vor jedem Test geleert -- kein {@code Thread.sleep}, kein
 * Testcontainer je Methode.
 *
 * Derselbe Container laeuft bewusst fuer die gesamte JVM weiter (kein
 * {@code stop()}): Testcontainers' Ryuk-Reaper raeumt ihn beim Ende des
 * Testlaufs auf.
 */
public abstract class PostgresAdapterSupport {

    private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");

    protected static final DataSource DATA_SOURCE;
    protected static final NamedParameterJdbcTemplate JDBC;

    static {
        CONTAINER.start();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(CONTAINER.getJdbcUrl());
        hikariConfig.setUsername(CONTAINER.getUsername());
        hikariConfig.setPassword(CONTAINER.getPassword());
        DATA_SOURCE = new HikariDataSource(hikariConfig);

        Flyway.configure()
                .dataSource(DATA_SOURCE)
                .locations("classpath:db/league/migration")
                .load()
                .migrate();

        JDBC = new NamedParameterJdbcTemplate(DATA_SOURCE);
    }

    /** Wird mit jeder neuen Tabelle aus einer spaeteren Stufe ergaenzt. */
    @BeforeEach
    void leereTabellen() {
        JDBC.getJdbcTemplate().execute("TRUNCATE TABLE account, login_link, account_session CASCADE");
    }
}
