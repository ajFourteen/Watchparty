package de.fourteen.watchparty.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Ein moeglicher Ausgang innerhalb einer {@link Bet}. Value Object.
 *
 * {@code note} haelt die Abgrenzung aus Anforderung 4.1 fest (z. B. dass der
 * verschossene Field Goal unter Turnover on Downs faellt) und muss in der
 * Oberflaeche sichtbar sein, damit es beim Aufloesen keinen Streit gibt.
 * {@code null}, wo der Ausgang fuer sich spricht (ADR-026).
 */
public record Outcome(OutcomeId id, String label, @Nullable String note) {

    public static Outcome of(String id, String label, @Nullable String note) {
        return new Outcome(OutcomeId.of(id), label, note);
    }
}
