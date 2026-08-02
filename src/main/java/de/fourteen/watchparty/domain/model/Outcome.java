package de.fourteen.watchparty.domain.model;

/**
 * Ein moeglicher Ausgang innerhalb einer {@link Bet}. Value Object.
 *
 * {@code note} haelt die Abgrenzung aus Anforderung 4.1 fest (z. B. dass der
 * verschossene Field Goal unter Turnover on Downs faellt) und muss in der
 * Oberflaeche sichtbar sein, damit es beim Aufloesen keinen Streit gibt.
 */
public record Outcome(OutcomeId id, String label, String note) {

    public static Outcome of(String id, String label, String note) {
        return new Outcome(OutcomeId.of(id), label, note);
    }
}
