package de.fourteen.watchparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * DataSource- und Flyway-Autoconfiguration sind bewusst ausgeschaltet: Die
 * Liga-Datenbank wird von Hand in {@code config.league} verdrahtet (Stil wie
 * {@code RoomConfig}/{@code SnapshotConfig}), damit Verbindungsaufbau und
 * Migration derselben expliziten Kontrolle unterliegen wie der Rest der
 * Anwendung -- und damit die Live-Wetten ohne jede Datenbank-Konfiguration
 * weiter starten (ADR-034, Kriterium 37).
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class })
public class WatchpartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatchpartyApplication.class, args);
    }
}
