package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.adapter.out.file.SnapshotStore;
import de.fourteen.watchparty.domain.model.RoomSnapshot;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der Snapshot-Round-Trip auf der Adapter-Ebene (docs/teststrategie.md,
 * Abschnitt 2.3): Der Fall wird am Port-Datentyp ({@link RoomSnapshot})
 * konstruiert, nicht ueber die Domaene erzeugt. Pilotszenario aus Phase 1 der
 * Teststrategie-Umsetzung, Grundlage fuer den vollstaendig gefuellten Raum
 * aus Phase 3.4.
 */
public class SnapshotStufen extends DeutscheStufe<SnapshotStufen> {

    private static final Instant JETZT = Instant.parse("2026-08-01T20:00:00Z");

    private Path verzeichnis;
    private RoomSnapshot geschrieben;
    private List<RoomSnapshot> geladen = List.of();

    public SnapshotStufen einRaumSnapshotMitEinerAbgeschlossenenRunde(Path verzeichnis) {
        this.verzeichnis = verzeichnis;
        geschrieben = new RoomSnapshot(
                RoomSnapshot.SCHEMA_VERSION,
                "AB3D",
                JETZT.toEpochMilli(),
                "host",
                3,
                List.of(
                        new RoomSnapshot.PlayerSnapshot("host", "token-host", "Host", 1200, 0),
                        new RoomSnapshot.PlayerSnapshot("anna", "token-anna", "Anna", 975, 1)),
                new RoomSnapshot.RoundSnapshot(
                        2,
                        "drive-outcome",
                        JETZT.plusSeconds(15).toEpochMilli(),
                        "RESOLVED",
                        List.of("host", "anna"),
                        List.of(new RoomSnapshot.PickSnapshot("host", "touchdown", 25)),
                        "touchdown",
                        Map.of("host", 25, "anna", -25),
                        25,
                        false,
                        false));
        return this;
    }

    public SnapshotStufen wirdGeschriebenUndWiederGeladen() {
        SnapshotStore writer = new SnapshotStore(verzeichnis);
        writer.save(geschrieben);
        writer.awaitWritten();

        SnapshotStore reader = new SnapshotStore(verzeichnis);
        geladen = reader.loadAll(JETZT, Duration.ofHours(6));
        return this;
    }

    public SnapshotStufen ergibtWiederExaktDenselbenStand() {
        assertThat(geladen).containsExactly(geschrieben);
        return this;
    }
}
