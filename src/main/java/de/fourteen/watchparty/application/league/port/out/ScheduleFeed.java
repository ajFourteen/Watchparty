package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.Matchday;

import java.util.List;

/**
 * Ausgangs-Port zum Spielplan-Feed (ADR-037). Liefert den aktuellen Stand
 * eines Spieltags in den eigenen Typen des Domänenmodells — kein Aufrufer
 * kennt ESPN oder ein anderes konkretes Format, nur diesen Port.
 *
 * Wirft eine unchecked Exception bei Ausfall oder kaputter Antwort, statt
 * leer zurueckzugeben: Der Aufrufer (Kriterium 11) muss den Unterschied
 * zwischen "kein Spiel an diesem Spieltag" und "der Feed ist gerade nicht
 * erreichbar" kennen, um den letzten bekannten Stand unangetastet zu lassen.
 */
public interface ScheduleFeed {

    List<Game> fetchMatchday(Matchday matchday);
}
