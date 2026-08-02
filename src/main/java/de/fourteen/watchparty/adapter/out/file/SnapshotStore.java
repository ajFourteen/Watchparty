package de.fourteen.watchparty.adapter.out.file;

import de.fourteen.watchparty.application.port.out.SnapshotRepository;
import de.fourteen.watchparty.domain.model.RoomSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Schreibt und liest den {@link RoomSnapshot} auf die Platte (ADR-023).
 *
 * Analog zur Ausgangs-Queue in {@code ClientSession} (ADR-012): Der
 * Raum-Thread darf nicht auf Dateisystem-I/O warten (Invariante 2), deshalb
 * läuft das Schreiben auf einem eigenen Thread. {@link #save} wird
 * ausschließlich vom Raum-Thread aufgerufen — genau ein Erzeuger. Nur damit
 * ist die Verdichtung über eine einfache {@link ArrayBlockingQueue} der
 * Kapazität 1 korrekt: {@code poll()} gefolgt von {@code offer()} kann sich
 * nicht mit einem zweiten Erzeuger verschränken.
 *
 * {@code path == null} bedeutet Persistenz aus — die Voreinstellung für
 * lokale Entwicklung und Tests, siehe {@code watchparty.snapshot.path}.
 */
public class SnapshotStore implements SnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(SnapshotStore.class);

    private final Path path;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Thread writer;

    /**
     * Der wartende Stand mit einer laufenden Nummer. Die Nummer traegt die
     * Warteschlange und nicht ein Feld daneben, weil {@link #awaitWritten()}
     * sonst nicht sicher sagen koennte, <em>welcher</em> Stand geschrieben
     * wurde: Zwischen dem Leeren der Queue und dem Ablesen einer separaten
     * Nummer koennte laengst der naechste Stand eingereiht worden sein.
     */
    private record Queued(long seq, RoomSnapshot snapshot) {
    }

    private final ArrayBlockingQueue<Queued> pending = new ArrayBlockingQueue<>(1);

    /** Zuletzt eingereiht bzw. zuletzt fertig geschrieben — siehe {@link #awaitWritten()}. */
    private final AtomicLong queuedSeq = new AtomicLong();
    private final AtomicLong writtenSeq = new AtomicLong();

    public SnapshotStore(Path path) {
        this.path = path;
        if (path != null) {
            writer = new Thread(this::runLoop, "snapshot-writer");
            writer.setDaemon(true);
            writer.start();
        } else {
            writer = null;
        }
    }

    public boolean isEnabled() {
        return path != null;
    }

    /**
     * Reiht einen Snapshot zum Schreiben ein. Wer während eines laufenden
     * Schreibvorgangs nachlegt, überschreibt den wartenden — es kann sich
     * keine Queue aufstauen, egal wie oft der Raumzustand sich ändert.
     */
    @Override
    public void save(RoomSnapshot snapshot) {
        if (!isEnabled()) {
            return;
        }
        Queued queued = new Queued(queuedSeq.incrementAndGet(), snapshot);
        pending.poll();
        pending.offer(queued);
    }

    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            Queued queued;
            try {
                queued = pending.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // Waehrend des Schreibens kann schon der naechste Stand anstehen
            // -- dann gleich den neuesten nehmen statt zwei Schreibvorgaenge
            // hintereinander zu machen.
            Queued latest = queued;
            Queued next;
            while ((next = pending.poll()) != null) {
                latest = next;
            }
            writeToDisk(latest.snapshot());
            writtenSeq.set(latest.seq());
        }
    }

    private void writeToDisk(RoomSnapshot snapshot) {
        try {
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            byte[] json = mapper.writeValueAsBytes(snapshot);
            try (FileChannel channel = FileChannel.open(tmp,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(json));
                channel.force(true);
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Ein volles Dateisystem oder ein fehlendes Volume darf das
            // Spiel nicht anhalten (Invariante 2) -- geloggt und geschluckt.
            log.error("Snapshot konnte nicht geschrieben werden", e);
        }
    }

    /**
     * Liest den zuletzt geschriebenen Snapshot. {@code Optional.empty()} in
     * jedem Zweifelsfall (Datei fehlt, ist kaputt, oder ist aelter als
     * {@code ttl}) -- ein Snapshot, der den Start zerschiesst, ist der
     * schlimmste denkbare Ausgang.
     */
    @Override
    public Optional<RoomSnapshot> load(Instant now, Duration ttl) {
        if (!isEnabled() || !Files.exists(path)) {
            return Optional.empty();
        }
        RoomSnapshot snapshot;
        try {
            snapshot = mapper.readValue(path.toFile(), RoomSnapshot.class);
        } catch (IOException e) {
            log.error("Snapshot ist beschaedigt, Raum startet leer", e);
            quarantine();
            return Optional.empty();
        }
        if (snapshot.schemaVersion() != RoomSnapshot.SCHEMA_VERSION) {
            log.error("Snapshot mit unbekannter schemaVersion {} ignoriert, Raum startet leer",
                    snapshot.schemaVersion());
            quarantine();
            return Optional.empty();
        }
        if (Instant.ofEpochMilli(snapshot.savedAt()).plus(ttl).isBefore(now)) {
            log.info("Snapshot ist aelter als die Verfallszeit, Raum startet leer");
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    private void quarantine() {
        try {
            Files.move(path, path.resolveSibling(path.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Kaputter Snapshot konnte nicht beiseite gelegt werden", e);
        }
    }

    /**
     * Nur fuer Tests: blockiert (durch Polling), bis der Schreib-Thread alle
     * bis hierhin per {@link #save} eingereihten Snapshots geschrieben hat.
     *
     * Wartet auf die laufende Nummer, nicht auf "Queue leer und gerade nichts
     * am Schreiben". Die fruehere Fassung hatte dort eine Luecke: Der
     * Schreib-Thread nimmt den Stand mit {@code take()} aus der Queue, bevor
     * er sich als beschaeftigt markiert. Genau dazwischen sah diese Methode
     * eine leere Queue und einen unbeschaeftigten Schreiber und kehrte zurueck
     * -- der Test las dann den vorherigen Stand von der Platte und schlug
     * sporadisch fehl. Mit der Nummer gibt es dieses Fenster nicht mehr.
     */
    public void awaitWritten() {
        if (!isEnabled()) {
            return;
        }
        long target = queuedSeq.get();
        while (writtenSeq.get() < target) {
            Thread.onSpinWait();
        }
    }

    public void shutdown() {
        if (writer != null) {
            writer.interrupt();
        }
    }
}
