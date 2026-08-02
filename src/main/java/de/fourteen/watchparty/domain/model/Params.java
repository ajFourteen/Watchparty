package de.fourteen.watchparty.domain.model;

/**
 * Die Wett-Parameter aus Anforderung 3.1, an einer Stelle im Code statt
 * verstreut.
 */
public record Params(int minStake, int penalty) {

    public static final Params DEFAULT = new Params(25, 25);
}
