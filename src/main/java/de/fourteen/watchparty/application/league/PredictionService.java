package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.PredictionRepository;
import de.fourteen.watchparty.application.league.view.PredictionView;
import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.LeaguePoints;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.Prediction;
import de.fourteen.watchparty.domain.model.league.PredictionId;
import de.fourteen.watchparty.domain.service.league.Scoring;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Setzt {@link PredictionCommands} um (Kapitel 13.4). Die eigentliche
 * Sichtbarkeitsregel steckt in {@link PredictionView}; hier wird nur
 * beschafft und zusammengefuehrt.
 */
public class PredictionService implements PredictionCommands {

    private final Clock clock;
    private final GameRepository games;
    private final PredictionRepository predictions;
    private final AccountRepository accounts;

    public PredictionService(Clock clock, GameRepository games, PredictionRepository predictions,
            AccountRepository accounts) {
        this.clock = clock;
        this.games = games;
        this.predictions = predictions;
        this.accounts = accounts;
    }

    @Override
    public PredictionView.MatchdayView viewMatchday(EmailAddress requester, Matchday matchday) {
        Instant now = clock.instant();
        List<PredictionView.GameView> gameViews = new ArrayList<>();
        for (Game game : games.findByMatchday(matchday)) {
            gameViews.add(PredictionView.game(requester, now, game, predictions.findByGame(game.getId()), this::displayNameOf));
        }
        return new PredictionView.MatchdayView(matchday.week(), gameViews);
    }

    @Override
    public void submitPrediction(EmailAddress account, GameId gameId, GameScore score) {
        Game game = games.findById(gameId).orElseThrow(() -> new NoSuchElementException("Unbekanntes Spiel: " + gameId));
        if (!clock.instant().isBefore(game.getKickoff())) {
            throw new IllegalStateException("Das Spiel hat bereits angestossen, ein Tipp ist nicht mehr moeglich");
        }
        predictions.save(Prediction.of(PredictionId.of(account, gameId), score));
    }

    @Override
    public LeaguePoints totalPoints(EmailAddress account) {
        int total = 0;
        for (Prediction prediction : predictions.findByAccount(account)) {
            GameId gameId = prediction.getId().gameId();
            Game game = games.findById(gameId).orElseThrow(() -> new NoSuchElementException("Unbekanntes Spiel: " + gameId));
            if (game.getStatus() != GameStatus.FINAL) {
                continue;
            }
            // Als FINAL markierte Spiele haben laut Game-Konstruktor immer ein Ergebnis.
            GameScore actual = Objects.requireNonNull(game.getScore());
            total += Scoring.score(prediction.getScore(), actual).value();
        }
        return new LeaguePoints(total);
    }

    @Override
    public ReportView.MatchdayReportView matchdayReport(EmailAddress account, Matchday matchday) {
        List<Game> finishedGames = games.findByMatchday(matchday).stream()
                .filter(g -> g.getStatus() == GameStatus.FINAL)
                .toList();
        // Ausschliesslich die eigenen Ergebnistipps -- ein fremder Tipp wird
        // hier gar nicht erst geladen (13.9-e), nicht nur nachtraeglich ausgefiltert.
        Map<GameId, GameScore> ownPredictions = predictions.findByAccount(account).stream()
                .collect(Collectors.toMap(p -> p.getId().gameId(), Prediction::getScore));
        return ReportView.matchday(matchday, finishedGames, ownPredictions);
    }

    private DisplayName displayNameOf(EmailAddress email) {
        return accounts.findByEmail(email).map(Account::getDisplayName).orElse(DisplayName.of("?"));
    }
}
