package de.fourteen.watchparty.domain.model;

/**
 * Die vier Zustaende einer Runde nach ADR-020. {@code IDLE} existiert nur
 * implizit (keine aktuelle {@link Round}); {@code OPEN}, {@code CLOSED} und
 * {@code RESOLVED} sind Zustaende der jeweils aktuellen Runde.
 */
public enum Phase {
    IDLE, OPEN, CLOSED, RESOLVED
}
