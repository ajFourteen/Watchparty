package de.fourteen.watchparty.application.port.out;

import de.fourteen.watchparty.domain.model.RoomSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ausgangs-Port fuer den Snapshot aus ADR-023. Spricht {@link RoomSnapshot} —
 * ein Datenmodell der Domaene, nicht des Adapters. Deshalb zeigt die
 * Abhaengigkeit vom Datei-Adapter nach innen und nicht umgekehrt.
 *
 * Seit ADR-033 haelt eine Instanz mehrere Watchpartys, deshalb {@link
 * #loadAll} statt eines einzelnen Stands und {@link #delete} fuers
 * Aufraeumen nach Ablauf (Anforderung 1-j).
 *
 * {@link #save} muss sofort zurueckkehren (Invariante 2): Der Raum-Thread
 * wartet nie auf Dateisystem-I/O.
 */
public interface SnapshotRepository {

    /** Reiht einen Stand zum Schreiben ein. Nicht blockierend. */
    void save(RoomSnapshot snapshot);

    /**
     * Liest alle zuletzt geschriebenen, noch nicht abgelaufenen Staende.
     * Ein einzelner kaputter oder abgelaufener Stand faellt einfach weg,
     * statt den Start der uebrigen Watchpartys zu gefaehrden — ein Snapshot,
     * der den Start zerschiesst, ist der schlimmste denkbare Ausgang.
     */
    List<RoomSnapshot> loadAll(Instant now, Duration ttl);

    /** Entfernt den Stand einer Watchparty von der Platte (Anforderung 1-j). Nicht blockierend. */
    void delete(String code);
}
