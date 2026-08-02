package de.fourteen.watchparty.application.port.out;

import de.fourteen.watchparty.domain.model.RoomSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Ausgangs-Port fuer den Snapshot aus ADR-023. Spricht {@link RoomSnapshot} —
 * ein Datenmodell der Domaene, nicht des Adapters. Deshalb zeigt die
 * Abhaengigkeit vom Datei-Adapter nach innen und nicht umgekehrt.
 *
 * {@link #save} muss sofort zurueckkehren (Invariante 2): Der Raum-Thread
 * wartet nie auf Dateisystem-I/O.
 */
public interface SnapshotRepository {

    /** Reiht einen Stand zum Schreiben ein. Nicht blockierend. */
    void save(RoomSnapshot snapshot);

    /**
     * Liest den zuletzt geschriebenen Stand. {@code Optional.empty()} in jedem
     * Zweifelsfall — ein Snapshot, der den Start zerschiesst, ist der
     * schlimmste denkbare Ausgang.
     */
    Optional<RoomSnapshot> load(Instant now, Duration ttl);
}
