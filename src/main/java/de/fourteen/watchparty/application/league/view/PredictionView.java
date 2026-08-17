package de.fourteen.watchparty.application.league.view;

import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Prediction;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Die Projektion vom Spielplan- und Tipp-Zustand auf die Antwort ans Konto,
 * das fragt — dieselbe Rolle wie {@code RoomView} fuer die Live-Wetten,
 * hier fuer Kriterium 19/20 statt Invariante 4.
 *
 * Rein lesend und ohne eigenen Zustand: was ein Konto sehen darf, haengt
 * allein von den uebergebenen Werten ab, insbesondere {@code now} gegen
 * {@link Game#getKickoff()}. Vor dem Anstoss verlaesst kein fremder Tipp
 * diese Methode — nicht verschleiert, sondern schlicht nicht Teil des
 * zurueckgegebenen Objekts. Genau das macht den Unterschied zu einer
 * Oberflaeche, die etwas nur nicht anzeigt: Wer die Antwort selbst mitliest,
 * erfaehrt nichts (Kriterium 19, HIGH).
 */
@Criticality(level = Criticality.Level.HIGH, requirements = { "13.4-d", "13.4-e" })
public final class PredictionView {

    private PredictionView() {
    }

    /**
     * Baut die Ansicht auf ein einzelnes Spiel fuer das anfragende Konto.
     * {@code predictionsForThisGame} traegt ausschliesslich Tipps zu genau
     * diesem Spiel — die Zuordnung passiert in der Anwendungsschicht, nicht
     * hier, damit diese Methode keine Spiel-ID-Vergleiche zusaetzlich zur
     * eigentlichen Sichtbarkeitsregel braucht.
     */
    public static GameView game(EmailAddress requester, Instant now, Game game,
            List<Prediction> predictionsForThisGame, Function<EmailAddress, DisplayName> displayNameOf) {
        boolean angepfiffen = !now.isBefore(game.getKickoff());

        @Nullable ScoreView ownPrediction = predictionsForThisGame.stream()
                .filter(p -> p.getId().accountEmail().equals(requester))
                .findFirst()
                .map(p -> score(p.getScore()))
                .orElse(null);

        List<PredictionEntryView> otherPredictions = angepfiffen
                ? predictionsForThisGame.stream()
                        .filter(p -> !p.getId().accountEmail().equals(requester))
                        .map(p -> new PredictionEntryView(
                                displayNameOf.apply(p.getId().accountEmail()).value(), score(p.getScore())))
                        .toList()
                : List.of();

        return new GameView(
                game.getId().value(),
                game.getHomeTeam().name(),
                game.getAwayTeam().name(),
                game.getKickoff(),
                game.getStatus().name(),
                game.getScore() == null ? null : score(game.getScore()),
                ownPrediction,
                otherPredictions);
    }

    private static ScoreView score(GameScore score) {
        return new ScoreView(score.home(), score.away());
    }

    public record MatchdayView(int week, List<GameView> games) {
    }

    public record GameView(
            String gameId,
            String homeTeamName,
            String awayTeamName,
            Instant kickoff,
            String status,
            @Nullable ScoreView finalScore,
            @Nullable ScoreView ownPrediction,
            List<PredictionEntryView> otherPredictions) {
    }

    public record ScoreView(int home, int away) {
    }

    public record PredictionEntryView(String displayName, ScoreView score) {
    }
}
