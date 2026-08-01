package de.fourteenit.watchparty.room;

/**
 * Ein moeglicher Ausgang innerhalb einer {@link Bet}. {@code note} haelt
 * die Abgrenzung aus Anforderung 4.1 fest (z. B. dass der verschossene Field
 * Goal unter Turnover on Downs faellt) und muss in der Oberflaeche sichtbar
 * sein, damit es beim Aufloesen keinen Streit gibt.
 */
public record Outcome(String id, String label, String note) {
}
