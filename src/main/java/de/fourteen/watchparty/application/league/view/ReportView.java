package de.fourteen.watchparty.application.league.view;

import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.LeaguePoints;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.service.league.Scoring;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Die eigene Bilanz eines Spieltags (Kapitel 13.9, erster Schnitt aus
 * {@code docs/features/006-spieltags-report.md}): je gewertetem Spiel das
 * Endergebnis, der eigene Ergebnistipp — falls abgegeben — und die daraus
 * erreichten Wertungspunkte ({@link Scoring}), dazu die Summe ueber den
 * ganzen Spieltag.
 *
 * Rein lesend, aber anders als {@link PredictionView} ohne eigene
 * Sichtbarkeitsregel: Ein fremder Tipp kommt hier gar nicht erst hinein
 * (13.9-e), weil {@code ownPredictions} von der aufrufenden Seite
 * (PredictionService.matchdayReport) von vornherein nur mit den Tipps des
 * anfragenden Kontos befuellt wird — diese Projektion sieht andere Konten
 * nie.
 */
@Criticality(level = Criticality.Level.MEDIUM,
        requirements = { "13.9-a", "13.9-b", "13.9-c", "13.9-d", "13.9-e" })
public final class ReportView {

    private ReportView() {
    }

    /**
     * {@code finishedGames} traegt bereits nur die gewerteten (FINAL)
     * Spiele des Spieltags — die Auswahl trifft die Anwendungsschicht
     * (13.9-b), nicht diese Projektion.
     */
    public static MatchdayReportView matchday(Matchday matchday, List<Game> finishedGames,
            Map<GameId, GameScore> ownPredictions) {
        List<GameEntryView> entries = finishedGames.stream()
                .map(game -> entry(game, ownPredictions.get(game.getId())))
                .toList();
        int totalPoints = entries.stream().mapToInt(GameEntryView::points).sum();
        return new MatchdayReportView(matchday.week(), entries, totalPoints);
    }

    private static GameEntryView entry(Game game, @Nullable GameScore ownPrediction) {
        // Als FINAL markierte Spiele haben laut Game-Konstruktor immer ein Ergebnis.
        GameScore actual = Objects.requireNonNull(game.getScore());
        LeaguePoints points = ownPrediction == null ? LeaguePoints.NONE : Scoring.score(ownPrediction, actual);
        return new GameEntryView(
                game.getId().value(),
                game.getHomeTeam().name(),
                game.getAwayTeam().name(),
                score(actual),
                ownPrediction == null ? null : score(ownPrediction),
                points.value());
    }

    private static PredictionView.ScoreView score(GameScore score) {
        return new PredictionView.ScoreView(score.home(), score.away());
    }

    public record MatchdayReportView(int week, List<GameEntryView> games, int totalPoints) {
    }

    public record GameEntryView(
            String gameId,
            String homeTeamName,
            String awayTeamName,
            PredictionView.ScoreView finalScore,
            PredictionView.@Nullable ScoreView ownPrediction,
            int points) {
    }
}
