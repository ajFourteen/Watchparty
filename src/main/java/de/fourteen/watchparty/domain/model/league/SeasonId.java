package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Die Identität einer Saison: das Jahr, in dem sie beginnt (ADR-034,
 * "eine Liga gehört zu genau einer Saison"). Kein Kalenderjahr im Sinn
 * eines Zeitraums — eine NFL-Saison beginnt im September und endet im
 * Folgejahr, benannt wird sie trotzdem nach dem Startjahr.
 */
@ValueObject
public record SeasonId(int year) {

    public SeasonId {
        if (year < 1920) {
            throw new IllegalArgumentException("Eine Saison beginnt nicht vor 1920: " + year);
        }
    }

    public static SeasonId of(int year) {
        return new SeasonId(year);
    }

    @Override
    public String toString() {
        return String.valueOf(year);
    }
}
