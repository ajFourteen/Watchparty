package de.fourteen.watchparty.room;

/**
 * Ein abgegebener Tipp: ein Ausgang, ein Einsatz. Ein Spieler hat pro Runde
 * hoechstens einen Pick (Anforderung 6).
 *
 * Heisst bewusst nicht {@code Bet} — das ist die Wette selbst, also die
 * Frage, auf die hier getippt wird (ADR-022).
 */
public record Pick(String playerId, String outcomeId, int stake) {
}
