package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

/** Die Identität einer Liga. Value Object, UUID-basiert — anders als {@link LeagueCode} nie vorzulesen. */
@ValueObject
public record LeagueId(UUID value) {

    public LeagueId {
        if (value == null) {
            throw new IllegalArgumentException("Eine Liga-ID ist nie null");
        }
    }

    public static LeagueId newId() {
        return new LeagueId(UUID.randomUUID());
    }

    public static LeagueId of(UUID value) {
        return new LeagueId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
