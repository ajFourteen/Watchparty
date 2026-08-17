package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Eine Mannschaft, wie sie an einem Spiel beteiligt ist: Kürzel und
 * Anzeigename. Bewusst am {@link Game} mitgeführt statt in einer eigenen
 * Tabelle normalisiert — der Feed liefert beides ohnehin zu jedem Spiel neu,
 * und ein eigener Mannschafts-Stamm wäre Pflege ohne Gegenwert (ADR-037).
 */
@ValueObject
public record Team(TeamId id, String name) {

    public Team {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ein Mannschaftsname ist nie leer");
        }
    }

    public static Team of(TeamId id, String name) {
        return new Team(id, name);
    }
}
