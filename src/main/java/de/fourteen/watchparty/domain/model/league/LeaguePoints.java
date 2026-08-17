package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Wertungspunkte eines Ergebnistipps (13.5, ADR-038). Value Object.
 *
 * Ausdruecklich nicht {@link de.fourteen.watchparty.domain.model.Points}:
 * Eine Liga zahlt keinen Pool aus, die Zahl bedeutet etwas anderes
 * (ADR-025/ADR-038) — beide Typen bleiben deshalb unverwechselbar getrennt.
 */
@ValueObject
public record LeaguePoints(int value) implements Comparable<LeaguePoints> {

    public static final LeaguePoints NONE = new LeaguePoints(0);
    public static final LeaguePoints TENDENCY_ONLY = new LeaguePoints(3);
    public static final LeaguePoints TENDENCY_AND_BUCKET = new LeaguePoints(5);
    public static final LeaguePoints EXACT = new LeaguePoints(6);

    public LeaguePoints {
        if (value < 0) {
            throw new IllegalArgumentException("Wertungspunkte sind nie negativ (13.5-d), waren: " + value);
        }
    }

    @Override
    public int compareTo(LeaguePoints other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return value + " Wertungspunkte";
    }
}
