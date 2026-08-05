package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.port.out.SnapshotRepository;
import de.fourteen.watchparty.domain.model.RoomSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Das {@link SnapshotRepository} fuer Tests, die mit Persistenz nichts zu tun
 * haben: nimmt jeden Stand entgegen und wirft ihn weg, laedt nie etwas.
 *
 * Entspricht dem frueheren {@code new SnapshotStore(null)} — nur ohne den
 * Datei-Adapter aus dem Anwendungsring heraus anfassen zu muessen.
 */
public class NoSnapshots implements SnapshotRepository {

    @Override
    public void save(RoomSnapshot snapshot) {
    }

    @Override
    public Optional<RoomSnapshot> load(Instant now, Duration ttl) {
        return Optional.empty();
    }
}
