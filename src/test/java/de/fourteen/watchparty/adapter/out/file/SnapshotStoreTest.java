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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Schreiben und Lesen auf echtem Dateisystem (ADR-023), eine Datei je
 * Watchparty in einem gemeinsamen Verzeichnis (ADR-033). Deckt genau die
 * Faelle ab, die {@code SnapshotTest} bewusst ausspart: I/O, Verdichten
 * mehrerer Schreibvorgaenge, das Nebeneinander mehrerer Watchpartys und die
 * Sonderfaelle beim Laden (Datei fehlt, ist kaputt, ist abgelaufen).
 */
@AdapterTest
class SnapshotStoreTest {

    private static final Instant NOW = Instant.parse("2026-08-01T20:00:00Z");
    private static final Duration TTL = Duration.ofHours(6);

    private static RoomSnapshot minimalSnapshot(String code, long savedAt) {
        return new RoomSnapshot(RoomSnapshot.SCHEMA_VERSION, code, savedAt, "host", 3,
                List.of(new RoomSnapshot.PlayerSnapshot("host", "th", "Host", 1200, 0)), null);
    }

    @Test
    void deaktivierterStoreSchreibtNichtsUndLaedtNichts(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(null);
        assertThat(store.isEnabled()).isFalse();

        store.save(minimalSnapshot("AB3D", NOW.toEpochMilli()));
        assertThat(store.loadAll(NOW, TTL)).isEmpty();
    }

    @Test
    void schreibenUndLadenErgibtDenselbenStand(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(dir);

        store.save(minimalSnapshot("AB3D", NOW.toEpochMilli()));

        await().atMost(Duration.ofSeconds(2)).until(() -> Files.exists(dir.resolve("AB3D.json")));
        SnapshotStore reader = new SnapshotStore(dir);
        List<RoomSnapshot> loaded = reader.loadAll(NOW, TTL);

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).code()).isEqualTo("AB3D");
        assertThat(loaded.get(0).hostPlayerId()).isEqualTo("host");
        assertThat(loaded.get(0).nextRoundId()).isEqualTo(3);
        assertThat(loaded.get(0).players()).hasSize(1);
        assertThat(loaded.get(0).players().get(0).points()).isEqualTo(1200);
    }

    @Test
    void nurDerNeuesteAusstehendeSnapshotJeWatchpartyWirdGeschrieben(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(dir);

        for (int points = 0; points < 20; points++) {
            RoomSnapshot snapshot = new RoomSnapshot(RoomSnapshot.SCHEMA_VERSION, "AB3D", NOW.toEpochMilli(),
                    "host", 1,
                    List.of(new RoomSnapshot.PlayerSnapshot("host", "th", "Host", points, 0)), null);
            store.save(snapshot);
        }

        SnapshotStore reader = new SnapshotStore(dir);
        await().atMost(Duration.ofSeconds(2)).until(() -> reader.loadAll(NOW, TTL).stream()
                .filter(s -> s.code().equals("AB3D"))
                .anyMatch(s -> s.players().get(0).points() == 19));
    }

    @Test
    void mehrereWatchpartysWerdenUnabhaengigVoneinanderGeladen(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(dir);
        store.save(minimalSnapshot("AB3D", NOW.toEpochMilli()));
        store.save(minimalSnapshot("ZZ99", NOW.toEpochMilli()));
        store.awaitWritten();

        List<RoomSnapshot> loaded = new SnapshotStore(dir).loadAll(NOW, TTL);

        assertThat(loaded).extracting(RoomSnapshot::code).containsExactlyInAnyOrder("AB3D", "ZZ99");
    }

    @Test
    void deleteEntferntNurDieEigeneDatei(@TempDir Path dir) {
        SnapshotStore store = new SnapshotStore(dir);
        store.save(minimalSnapshot("AB3D", NOW.toEpochMilli()));
        store.save(minimalSnapshot("ZZ99", NOW.toEpochMilli()));
        store.awaitWritten();

        store.delete("AB3D");
        store.awaitWritten();

        assertThat(Files.exists(dir.resolve("AB3D.json"))).isFalse();
        assertThat(Files.exists(dir.resolve("ZZ99.json"))).isTrue();
        assertThat(new SnapshotStore(dir).loadAll(NOW, TTL))
                .extracting(RoomSnapshot::code)
                .containsExactly("ZZ99");
    }

    @Test
    void fehlendesVerzeichnisErgibtLeer(@TempDir Path dir) {
        Path nieAngelegt = dir.resolve("nie-angelegt");
        SnapshotStore store = new SnapshotStore(nieAngelegt);
        assertThat(store.loadAll(NOW, TTL)).isEmpty();

        // Der Schreib-Thread legt das Verzeichnis asynchron an (Invariante 2).
        // Ohne diese Wartemarke rennt JUnits @TempDir-Aufraeumen manchmal
        // gegen genau diese Anlage und scheitert mit
        // DirectoryNotEmptyException, obwohl der Test selbst laengst gruen ist.
        await().atMost(Duration.ofSeconds(2)).until(() -> Files.isDirectory(nieAngelegt));
    }

    @Test
    void kaputteDateiWirdBeiseiteGelegtUndErgibtLeer(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("AB3D.json");
        Files.writeString(file, "das ist kein json{{{");
        SnapshotStore store = new SnapshotStore(dir);

        assertThat(store.loadAll(NOW, TTL)).isEmpty();
        assertThat(Files.exists(file)).isFalse();
        assertThat(Files.exists(dir.resolve("AB3D.json.bad"))).isTrue();
    }

    @Test
    void abgelaufenerSnapshotErgibtLeerAberBleibtUnangetastet(@TempDir Path dir) {
        SnapshotStore writer = new SnapshotStore(dir);
        Instant savedAt = NOW.minus(TTL).minusSeconds(1);
        writer.save(minimalSnapshot("AB3D", savedAt.toEpochMilli()));
        writer.awaitWritten();

        SnapshotStore reader = new SnapshotStore(dir);
        assertThat(reader.loadAll(NOW, TTL)).isEmpty();
        assertThat(Files.exists(dir.resolve("AB3D.json"))).isTrue();
    }

    @Test
    void unbekannteSchemaVersionWirdBeiseiteGelegtUndErgibtLeer(@TempDir Path dir) {
        RoomSnapshot future = new RoomSnapshot(RoomSnapshot.SCHEMA_VERSION + 1, "AB3D", NOW.toEpochMilli(),
                "host", 1, List.of(), null);
        SnapshotStore writer = new SnapshotStore(dir);
        writer.save(future);
        writer.awaitWritten();

        SnapshotStore reader = new SnapshotStore(dir);
        assertThat(reader.loadAll(NOW, TTL)).isEmpty();
        assertThat(Files.exists(dir.resolve("AB3D.json.bad"))).isTrue();
    }
}
