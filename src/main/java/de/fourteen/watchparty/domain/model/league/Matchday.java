package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Saison und Spieltagsnummer zusammen: die Anzeige- und Tippeinheit
 * (Feature-Dokument, "Abgabeschluss ist der Anstoß des jeweiligen Spiels,
 * nicht der Spieltag als Ganzes — ein Spieltag ist die Anzeige- und
 * Tippeinheit, kein Termin"). Auf die Regular Season beschränkt (1..18) —
 * die erste Saison hat keine Playoffs.
 */
@ValueObject
public record Matchday(SeasonId season, int week) {

    public static final int REGULAR_SEASON_WEEKS = 18;

    public Matchday {
        if (week < 1 || week > REGULAR_SEASON_WEEKS) {
            throw new IllegalArgumentException(
                    "Ein Spieltag liegt zwischen 1 und " + REGULAR_SEASON_WEEKS + ", war: " + week);
        }
    }

    public static Matchday of(SeasonId season, int week) {
        return new Matchday(season, week);
    }
}
