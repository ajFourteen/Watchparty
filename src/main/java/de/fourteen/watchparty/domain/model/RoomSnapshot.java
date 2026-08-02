package de.fourteen.watchparty.domain.model;

import de.fourteen.watchparty.domain.service.Bets;

import java.util.List;
import java.util.Map;

/**
 * Ein Abzug des Raumzustands zu einem Zeitpunkt, gedacht zum Überstehen
 * eines Neustarts innerhalb desselben Spielabends (ADR-023) — nicht zum
 * Überstehen mehrerer Abende, dafür sorgt {@code savedAt} zusammen mit der
 * Verfallszeit beim Laden.
 *
 * Bewusst ein eigenes Datenmodell statt {@link Room} direkt zu
 * serialisieren: Das hält die Datei unabhängig von internen Umbauten an
 * {@code Room}/{@code Round} und macht {@code schemaVersion} zu einer
 * echten Kompatibilitätsgrenze.
 */
public record RoomSnapshot(
        int schemaVersion,
        long savedAt,
        String hostPlayerId,
        long nextRoundId,
        List<PlayerSnapshot> players,
        RoundSnapshot round) {

    public static final int SCHEMA_VERSION = 1;

    public record PlayerSnapshot(String id, String token, String name, int points, int missedRounds) {
    }

    /**
     * {@code betId} statt der ganzen {@link Bet}: {@link Bets} bleibt die
     * einzige Quelle für den Katalog (ADR-017). Fehlt die ID beim Laden im
     * aktuellen Katalog, wird die Runde verworfen statt eine unbekannte
     * Wette wiederzubeleben.
     */
    public record RoundSnapshot(
            long id,
            String betId,
            long closesAt,
            String phase,
            List<String> participants,
            List<Pick> picks,
            String winningOutcomeId,
            Map<String, Integer> deltas,
            int pool,
            boolean annulled,
            boolean annulledByHost) {
    }
}
