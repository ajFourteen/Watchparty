package de.fourteen.watchparty.adapter.out.file;

import de.fourteen.watchparty.domain.model.RoomSnapshot;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Schreiben und Lesen auf echtem Dateisystem (ADR-023). Deckt genau die
 * Faelle ab, die {@code SnapshotTest} bewusst ausspart: I/O, Verdichten
 * mehrerer Schreibvorgaenge und die Sonderfaelle beim Laden aus Abschnitt 6
 * des Plans (Datei fehlt, ist kaputt, ist abgelaufen).
 */
@AdapterTest
class SnapshotStoreTest {

    private static final Instant NOW = Instant.parse("2026-08-01T20:00:00Z");
    private static final Duration TTL = Duration.ofHours(6);

    private static RoomSnapshot minimalSnapshot(long savedAt) {
        return new RoomSnapshot(RoomSnapshot.SCHEMA_VERSION, savedAt, "host", 3,
                List.of(new RoomSnapshot.PlayerSnapshot("host", "th", "Host", 1200, 0)), null);
    }

    @Test
    void deaktivierterStoreSchreibtNichtsUndLaedtNichts(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(null);
        assertThat(store.isEnabled()).isFalse();

        store.save(minimalSnapshot(NOW.toEpochMilli()));
        assertThat(store.load(NOW, TTL)).isEmpty();
    }

    @Test
    void schreibenUndLadenErgibtDenselbenStand(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(dir.resolve("room.json"));

        store.save(minimalSnapshot(NOW.toEpochMilli()));

        await().atMost(Duration.ofSeconds(2)).until(() -> Files.exists(dir.resolve("room.json")));
        SnapshotStore reader = new SnapshotStore(dir.resolve("room.json"));
        Optional<RoomSnapshot> loaded = reader.load(NOW, TTL);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().hostPlayerId()).isEqualTo("host");
        assertThat(loaded.get().nextRoundId()).isEqualTo(3);
        assertThat(loaded.get().players()).hasSize(1);
        assertThat(loaded.get().players().get(0).points()).isEqualTo(1200);
    }

    @Test
    void nurDerNeuesteAusstehendeSnapshotWirdGeschrieben(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(dir.resolve("room.json"));

        for (int points = 0; points < 20; points++) {
            RoomSnapshot snapshot = new RoomSnapshot(RoomSnapshot.SCHEMA_VERSION, NOW.toEpochMilli(),
                    "host", 1,
                    List.of(new RoomSnapshot.PlayerSnapshot("host", "th", "Host", points, 0)), null);
            store.save(snapshot);
        }

        SnapshotStore reader = new SnapshotStore(dir.resolve("room.json"));
        await().atMost(Duration.ofSeconds(2))
                .until(() -> reader.load(NOW, TTL).map(s -> s.players().get(0).points()).orElse(-1) == 19);
    }

    @Test
    void fehlendeDateiErgibtLeer(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(dir.resolve("nie-geschrieben.json"));
        assertThat(store.load(NOW, TTL)).isEmpty();
    }

    @Test
    void kaputteDateiWirdBeiseiteGelegtUndErgibtLeer(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("room.json");
        Files.writeString(file, "das ist kein json{{{");
        SnapshotStore store = new SnapshotStore(file);

        assertThat(store.load(NOW, TTL)).isEmpty();
        assertThat(Files.exists(file)).isFalse();
        assertThat(Files.exists(dir.resolve("room.json.bad"))).isTrue();
    }

    @Test
    void abgelaufenerSnapshotErgibtLeerAberBleibtUnangetastet(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("room.json");
        SnapshotStore writer = new SnapshotStore(file);
        Instant savedAt = NOW.minus(TTL).minusSeconds(1);
        writer.save(minimalSnapshot(savedAt.toEpochMilli()));
        await().atMost(Duration.ofSeconds(2)).until(() -> Files.exists(file));

        SnapshotStore reader = new SnapshotStore(file);
        assertThat(reader.load(NOW, TTL)).isEmpty();
        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void unbekannteSchemaVersionWirdBeiseiteGelegtUndErgibtLeer(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("room.json");
        RoomSnapshot future = new RoomSnapshot(RoomSnapshot.SCHEMA_VERSION + 1, NOW.toEpochMilli(),
                "host", 1, List.of(), null);
        SnapshotStore writer = new SnapshotStore(file);
        writer.save(future);
        await().atMost(Duration.ofSeconds(2)).until(() -> Files.exists(file));

        SnapshotStore reader = new SnapshotStore(file);
        assertThat(reader.load(NOW, TTL)).isEmpty();
        assertThat(Files.exists(dir.resolve("room.json.bad"))).isTrue();
    }
}
