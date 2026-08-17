package de.fourteen.watchparty.adapter.out.file;

import de.fourteen.watchparty.application.port.out.SnapshotRepository;
import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.domain.model.RoomSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Schreibt und liest die {@link RoomSnapshot}s aller Watchpartys auf die
 * Platte (ADR-023), eine Datei je Watchparty in einem gemeinsamen
 * Verzeichnis (ADR-033).
 *
 * Analog zur Ausgangs-Queue in {@code ClientSession} (ADR-012): Der
 * Raum-Thread darf nicht auf Dateisystem-I/O warten (Invariante 2), deshalb
 * laufen Schreiben <em>und</em> Loeschen auf einem eigenen Thread. {@link
 * #save}/{@link #delete} reihen nur ein — je Code gewinnt der jeweils
 * letzte Aufruf, ein Save loescht einen zuvor angestossenen Delete fuer
 * denselben Code aus der Warteschlange und umgekehrt, es kann sich also
 * keine Queue je Watchparty aufstauen.
 *
 * {@code directory == null} bedeutet Persistenz aus — die Voreinstellung
 * fuer lokale Entwicklung und Tests, siehe {@code watchparty.snapshot.path}.
 */
@Criticality(level = Criticality.Level.MEDIUM, requirements = { "1-d", "1-j" })
public class SnapshotStore implements SnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(SnapshotStore.class);

    private final @Nullable Path directory;
    private final ObjectMapper mapper = new ObjectMapper();
    private final @Nullable Thread writer;

    private final Map<String, RoomSnapshot> pendingSaves = new ConcurrentHashMap<>();
    private final Set<String> pendingDeletes = ConcurrentHashMap.newKeySet();

    /** Weckt den Schreib-Thread, sobald etwas ansteht. Traegt keine Daten, nur das Signal. */
    private final Semaphore wecker = new Semaphore(0);

    /** Zuletzt eingereiht bzw. zuletzt fertig verarbeitet — siehe {@link #awaitWritten()}. */
    private final AtomicLong queuedSeq = new AtomicLong();
    private final AtomicLong writtenSeq = new AtomicLong();

    public SnapshotStore(@Nullable Path directory) {
        this.directory = directory;
        if (directory != null) {
            // directory wird hier als Wert in die Closure aufgenommen, nicht
            // ueber das Feld gelesen: Der Schreib-Thread braucht keinen
            // weiteren Nullpruefung -- er existiert ja nur, weil directory
            // beim Start bereits nicht-null war.
            writer = new Thread(() -> runLoop(directory), "snapshot-writer");
            writer.setDaemon(true);
            writer.start();
        } else {
            writer = null;
        }
    }

    public boolean isEnabled() {
        return directory != null;
    }

    /**
     * Reiht einen Snapshot zum Schreiben ein. Wer waehrend eines laufenden
     * Schreibvorgangs fuer denselben Code nachlegt, ueberschreibt den
     * wartenden Stand — es kann sich keine Queue je Watchparty aufstauen,
     * egal wie oft ihr Zustand sich aendert.
     */
    @Override
    public void save(RoomSnapshot snapshot) {
        if (!isEnabled()) {
            return;
        }
        pendingDeletes.remove(snapshot.code());
        pendingSaves.put(snapshot.code(), snapshot);
        queuedSeq.incrementAndGet();
        wecker.release();
    }

    /** Reiht das Loeschen einer Watchparty-Datei ein (Anforderung 1-j). Nicht blockierend. */
    @Override
    public void delete(String code) {
        if (!isEnabled()) {
            return;
        }
        pendingSaves.remove(code);
        pendingDeletes.add(code);
        queuedSeq.incrementAndGet();
        wecker.release();
    }

    private void runLoop(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            log.error("Snapshot-Verzeichnis {} konnte nicht angelegt werden", directory, e);
        }
        while (!Thread.currentThread().isInterrupted()) {
            try {
                wecker.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // Seq VOR dem Verarbeiten gelesen: Alles bis zu diesem Stand war
            // beim Aufruf von save()/delete() bereits eingereiht und wird
            // unten mitverarbeitet -- eine sichere untere Schranke fuer das,
            // was writtenSeq gleich behaupten darf (siehe awaitWritten()).
            long seqAmStart = queuedSeq.get();
            for (String code : List.copyOf(pendingDeletes)) {
                pendingDeletes.remove(code);
                deleteFromDisk(directory, code);
            }
            for (String code : List.copyOf(pendingSaves.keySet())) {
                RoomSnapshot snapshot = pendingSaves.remove(code);
                if (snapshot != null) {
                    writeToDisk(directory, code, snapshot);
                }
            }
            writtenSeq.set(seqAmStart);
        }
    }

    private void writeToDisk(Path directory, String code, RoomSnapshot snapshot) {
        try {
            Path target = directory.resolve(code + ".json");
            Path tmp = directory.resolve(code + ".json.tmp");
            byte[] json = mapper.writeValueAsBytes(snapshot);
            try (FileChannel channel = FileChannel.open(tmp,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(json));
                channel.force(true);
            }
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Ein volles Dateisystem oder ein fehlendes Volume darf das
            // Spiel nicht anhalten (Invariante 2) -- geloggt und geschluckt.
            log.error("Snapshot fuer Watchparty {} konnte nicht geschrieben werden", code, e);
        }
    }

    private void deleteFromDisk(Path directory, String code) {
        try {
            Files.deleteIfExists(directory.resolve(code + ".json"));
        } catch (IOException e) {
            log.error("Snapshot fuer Watchparty {} konnte nicht geloescht werden", code, e);
        }
    }

    /**
     * Liest alle zuletzt geschriebenen, noch nicht abgelaufenen Staende aus
     * dem Verzeichnis. Ein einzelner kaputter oder abgelaufener Stand faellt
     * einfach weg, statt den Start der uebrigen Watchpartys zu gefaehrden —
     * ein Snapshot, der den Start zerschiesst, ist der schlimmste denkbare
     * Ausgang.
     */
    @Override
    public List<RoomSnapshot> loadAll(Instant now, Duration ttl) {
        Path currentDirectory = directory;
        if (currentDirectory == null || !Files.isDirectory(currentDirectory)) {
            return List.of();
        }
        List<RoomSnapshot> result = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(currentDirectory, "*.json")) {
            for (Path file : files) {
                readIfValid(file, now, ttl).ifPresent(result::add);
            }
        } catch (IOException e) {
            log.error("Snapshot-Verzeichnis {} konnte nicht gelesen werden", currentDirectory, e);
        }
        return result;
    }

    private Optional<RoomSnapshot> readIfValid(Path file, Instant now, Duration ttl) {
        RoomSnapshot snapshot;
        try {
            snapshot = mapper.readValue(file.toFile(), RoomSnapshot.class);
        } catch (IOException e) {
            log.error("Snapshot {} ist beschaedigt, wird uebersprungen", file.getFileName(), e);
            quarantine(file);
            return Optional.empty();
        }
        if (snapshot.schemaVersion() != RoomSnapshot.SCHEMA_VERSION) {
            log.error("Snapshot {} mit unbekannter schemaVersion {} ignoriert",
                    file.getFileName(), snapshot.schemaVersion());
            quarantine(file);
            return Optional.empty();
        }
        if (Instant.ofEpochMilli(snapshot.savedAt()).plus(ttl).isBefore(now)) {
            log.info("Snapshot {} ist aelter als die Verfallszeit, wird uebersprungen", file.getFileName());
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    private void quarantine(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Kaputter Snapshot {} konnte nicht beiseite gelegt werden", file.getFileName(), e);
        }
    }

    /**
     * Nur fuer Tests: blockiert (durch Polling), bis der Schreib-Thread alle
     * bis hierhin per {@link #save}/{@link #delete} eingereihten Auftraege
     * abgearbeitet hat.
     *
     * Wartet auf die laufende Nummer, nicht auf "Warteschlangen leer und
     * gerade nichts in Arbeit". Eine fruehere Fassung hatte dort eine
     * Luecke: Der Schreib-Thread nimmt einen Auftrag aus der Warteschlange,
     * bevor er sich als beschaeftigt markiert. Genau dazwischen saehe diese
     * Methode leere Warteschlangen und einen unbeschaeftigten Schreiber und
     * kehrte zurueck -- ein Test laese dann den vorherigen Stand von der
     * Platte und schluege sporadisch fehl. Mit der Nummer gibt es dieses
     * Fenster nicht mehr.
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
