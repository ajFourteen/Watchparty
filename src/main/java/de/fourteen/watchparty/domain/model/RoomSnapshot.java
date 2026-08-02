package de.fourteen.watchparty.domain.model;

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
 *
 * <b>Bewusst ohne Value Objects</b>, obwohl es im Domänenring liegt: Hier
 * stehen einfache Typen für das Dateiformat, nicht für das Modell. Genau
 * deshalb konnte das Modell auf {@link PlayerId}, {@link Points} und
 * Geschwister umgestellt werden, ohne dass sich ein Byte auf der Platte
 * ändert oder die {@code schemaVersion} steigen müsste. Die Umrechnung
 * steht in {@code Room.toSnapshot}/{@code fromSnapshot}.
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

    /** Entspricht Feld für Feld dem früher direkt serialisierten {@code Pick}. */
    public record PickSnapshot(String playerId, String outcomeId, int stake) {
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
            List<PickSnapshot> picks,
            String winningOutcomeId,
            Map<String, Integer> deltas,
            int pool,
            boolean annulled,
            boolean annulledByHost) {
    }
}
