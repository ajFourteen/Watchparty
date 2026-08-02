package de.fourteen.watchparty.room;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Bindet den {@link SnapshotStore} produktiv an einen Pfad (ADR-023).
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
