package de.fourteenit.watchparty.room;

import java.util.List;

/**
 * Ein Markt ist fachlich eine Frage mit einer festen Liste moeglicher
 * Ausgaenge (ADR-017). Der Drive-Ausgang ist nur die erste Instanz davon,
 * kein eingebauter Sonderfall — weitere Maerkte sind spaeter ein neuer
 * Datensatz, kein Umbau der Wett-Engine.
 */
public record Market(String id, String question, List<Outcome> outcomes) {
}
