package de.fourteenit.watchparty.room;

/**
 * Ein abgegebener Tipp: ein Ausgang, ein Einsatz. Ein Spieler hat pro Runde
 * hoechstens einen Bet (Anforderung 6).
 */
public record Bet(String playerId, String outcomeId, int stake) {
}
