package de.fourteen.watchparty.domain.model;

import java.util.List;

/**
 * Eine Wette ist fachlich eine Frage mit einer festen Liste moeglicher
 * Ausgaenge (ADR-017). Value Object — zwei Wetten mit gleicher ID und
 * gleichen Ausgaengen sind dieselbe Wette.
 *
 * Der Drive-Ausgang ist nur die erste Instanz davon, kein eingebauter
 * Sonderfall — weitere Wetten sind ein neuer Datensatz, kein Umbau der
 * Wett-Engine.
 *
 * {@code note} traegt die Regel, nach der aufgeloest wird, wenn sie sich
 * nicht schon aus den Ausgaengen ergibt (etwa die Yard-Schwellen beim Big
 * Play). Sie muss in der Oberflaeche sichtbar sein, damit es beim Aufloesen
 * keinen Streit gibt; {@code null}, wo die Frage fuer sich spricht.
 */
public record Bet(BetId id, String question, String note, List<Outcome> outcomes) {

    public Bet {
        outcomes = List.copyOf(outcomes);
    }

    /**
     * Gehoert dieser Ausgang zu dieser Wette? Die Frage stellte frueher der
     * RoomActor mit einem Stream ueber die Ausgaenge -- zweimal, beim Tippen
     * und beim Aufloesen. Sie gehoert zur Wette.
     */
    public boolean hasOutcome(OutcomeId outcomeId) {
        return outcomeId != null && outcomes.stream().anyMatch(outcome -> outcome.id().equals(outcomeId));
    }
}
