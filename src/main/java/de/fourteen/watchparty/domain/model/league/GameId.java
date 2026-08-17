package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/** Die Identität eines {@link Game} — die ID des Feeds selbst (ADR-037), kein eigenes Schema. */
@ValueObject
public record GameId(String value) {

    public GameId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Eine Spiel-ID ist nie leer");
        }
    }

    public static GameId of(String value) {
        return new GameId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
