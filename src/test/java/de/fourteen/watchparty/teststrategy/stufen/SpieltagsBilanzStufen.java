package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.league.FakeAccountRepository;
import de.fourteen.watchparty.application.league.FakeGameRepository;
import de.fourteen.watchparty.application.league.FakePredictionRepository;
import de.fourteen.watchparty.application.league.PredictionService;
import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.view.PredictionView;
import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port-to-Port-Stufe (docs/teststrategie.md, Abschnitt 2.2) fuer die eigene
 * Spieltags-Bilanz (Kapitel 13.9, Feature 006, Schnitt 1): Eingang ist
 * {@link PredictionService} als Umsetzung von {@link PredictionCommands},
 * derselbe Dienst wie fuer das Tippen selbst -- die Bilanz liest nur, was
 * dort schon steht.
 */
public class SpieltagsBilanzStufen extends DeutscheStufe<SpieltagsBilanzStufen> {

    private static final Instant START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);

    private final FakeClock clock = new FakeClock(START);
    private final FakeGameRepository games = new FakeGameRepository();
    private final FakePredictionRepository predictions = new FakePredictionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final PredictionCommands tippen = new PredictionService(clock, games, predictions, accounts);

    private ReportView.MatchdayReportView letzteBilanz;

    public SpieltagsBilanzStufen einSpielMitAnstossIn(String gameId, Duration abstand) {
        games.save(Game.of(GameId.of(gameId), MATCHDAY, HOME, AWAY, clock.instant().plus(abstand),
                GameStatus.SCHEDULED, null, false));
        return self();
    }

    /** Simuliert den Nachfuehr-Job (ADR-037): das Spiel geht von SCHEDULED auf FINAL mit dem genannten Endergebnis ueber. */
    public SpieltagsBilanzStufen dasSpielEndetMit(String gameId, int heim, int gast) {
        Game bestehend = games.findById(GameId.of(gameId)).orElseThrow();
        games.save(Game.of(bestehend.getId(), bestehend.getMatchday(), bestehend.getHomeTeam(), bestehend.getAwayTeam(),
                bestehend.getKickoff(), GameStatus.FINAL, GameScore.of(heim, gast), false));
        return self();
    }

    public SpieltagsBilanzStufen einKontoMitNamenExistiertFuer(String name, String email) {
        accounts.save(Account.of(EmailAddress.of(email), DisplayName.of(name), clock.instant(), false, ReportMailToken.generate()));
        return self();
    }

    public SpieltagsBilanzStufen tipptFuerDasSpiel(String email, String gameId, int heim, int gast) {
        tippen.submitPrediction(EmailAddress.of(email), GameId.of(gameId), GameScore.of(heim, gast));
        return self();
    }

    public SpieltagsBilanzStufen ruftDieBilanzAbFuer(String email) {
        letzteBilanz = tippen.matchdayReport(EmailAddress.of(email), MATCHDAY);
        return self();
    }

    public SpieltagsBilanzStufen zeigtFuerDasSpielDasEndergebnis(String gameId, int heim, int gast) {
        assertThat(eintrag(gameId).finalScore()).isEqualTo(new PredictionView.ScoreView(heim, gast));
        return self();
    }

    public SpieltagsBilanzStufen zeigtFuerDasSpielDenEigenenTipp(String gameId, int heim, int gast) {
        assertThat(eintrag(gameId).ownPrediction()).isEqualTo(new PredictionView.ScoreView(heim, gast));
        return self();
    }

    public SpieltagsBilanzStufen zeigtFuerDasSpielKeinenEigenenTipp(String gameId) {
        assertThat(eintrag(gameId).ownPrediction()).isNull();
        return self();
    }

    public SpieltagsBilanzStufen zeigtFuerDasSpielDiePunkte(String gameId, int punkte) {
        assertThat(eintrag(gameId).points()).isEqualTo(punkte);
        return self();
    }

    public SpieltagsBilanzStufen enthaeltDasSpielNicht(String gameId) {
        assertThat(letzteBilanz.games()).noneMatch(g -> g.gameId().equals(gameId));
        return self();
    }

    public SpieltagsBilanzStufen zeigtDieSpieltagssumme(int punkte) {
        assertThat(letzteBilanz.totalPoints()).isEqualTo(punkte);
        return self();
    }

    private ReportView.GameEntryView eintrag(String gameId) {
        assertThat(letzteBilanz).as("es haette bereits eine Bilanz abgerufen worden sein sollen").isNotNull();
        return letzteBilanz.games().stream().filter(g -> g.gameId().equals(gameId)).findFirst().orElseThrow();
    }
}
