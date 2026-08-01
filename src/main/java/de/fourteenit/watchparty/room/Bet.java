package de.fourteenit.watchparty.room;

import java.util.List;

/**
 * Eine Wette ist fachlich eine Frage mit einer festen Liste moeglicher
 * Ausgaenge (ADR-017). Der Drive-Ausgang ist nur die erste Instanz davon,
 * kein eingebauter Sonderfall — weitere Wetten sind ein neuer Datensatz,
 * kein Umbau der Wett-Engine.
 *
 * {@code note} traegt die Regel, nach der aufgeloest wird, wenn sie sich
 * nicht schon aus den Ausgaengen ergibt (etwa die Yard-Schwellen beim Big
 * Play). Sie muss in der Oberflaeche sichtbar sein, damit es beim Aufloesen
 * keinen Streit gibt; {@code null}, wo die Frage fuer sich spricht.
 */
public record Bet(String id, String question, String note, List<Outcome> outcomes) {
}
