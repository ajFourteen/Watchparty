package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Wer ein {@link GameScore} fuer sich entscheidet (13.5-b). Value Object.
 *
 * Eigener Begriff fuer das Tippspiel (ADR-038), nicht zu verwechseln mit
 * einem Ausgang ({@code Outcome}) der Live-Wetten.
 */
@ValueObject
public enum Tendency {
    HEIM, GAST, UNENTSCHIEDEN
}
