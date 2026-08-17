package de.fourteen.watchparty.config;

import de.fourteen.watchparty.adapter.out.file.SnapshotStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Bindet den {@link SnapshotStore} produktiv an ein Verzeichnis (ADR-023,
 * seit ADR-033 ein Verzeichnis statt einer einzelnen Datei -- eine Datei je
 * Watchparty).
 *
 * {@code watchparty.snapshot.path} leer oder ungesetzt bedeutet Persistenz
 * aus -- das ist die Voreinstellung fuer lokale Entwicklung und Tests und
 * gleichzeitig der Notausschalter, falls der Snapshot am Spielabend Aerger
 * macht.
 */
@Configuration
public class SnapshotConfig {

    @Bean(destroyMethod = "shutdown")
    public SnapshotStore snapshotStore(@Value("${watchparty.snapshot.path:}") String path) {
        return new SnapshotStore(path == null || path.isBlank() ? null : Path.of(path));
    }
}
