package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Der Stand eines Spiels. Value Object (Enum).
 *
 * {@link #CANCELLED} deckt sowohl ein abgesagtes als auch ein aus anderem
 * Grund nicht gewertetes Spiel ab (Kriterium 13) — für die Wertung ist der
 * Unterschied ohne Bedeutung, beide bringen niemandem Punkte.
 */
@ValueObject
public enum GameStatus {
    SCHEDULED,
    FINAL,
    CANCELLED
}
