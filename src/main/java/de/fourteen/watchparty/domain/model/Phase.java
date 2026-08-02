package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Die vier Zustaende einer Runde nach ADR-020. {@code IDLE} existiert nur
 * implizit (keine aktuelle {@link Round}); {@code OPEN}, {@code CLOSED} und
 * {@code RESOLVED} sind Zustaende der jeweils aktuellen Runde.
 */
@ValueObject
public enum Phase {
    IDLE, OPEN, CLOSED, RESOLVED
}
