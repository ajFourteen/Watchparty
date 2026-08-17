package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.league.FakeAccountRepository;
import de.fourteen.watchparty.application.league.FakeGameRepository;
import de.fourteen.watchparty.application.league.FakePredictionRepository;
import de.fourteen.watchparty.application.league.PredictionService;
import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.view.PredictionView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port-to-Port-Stufe (docs/teststrategie.md, Abschnitt 2.2) fuers Tippen
 * (Kapitel 13.4): Eingang ist {@link PredictionService} als Umsetzung von
 * {@link PredictionCommands}, Ausgaenge sind handgeschriebene Test Doubles
 * (ADR-025).
 */
public class TippenStufen extends DeutscheStufe<TippenStufen> {

    private static final Instant START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);

    private final FakeClock clock = new FakeClock(START);
    private final FakeGameRepository games = new FakeGameRepository();
    private final FakePredictionRepository predictions = new FakePredictionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final PredictionCommands tippen = new PredictionService(clock, games, predictions, accounts);

    private Exception letzterFehler;
    private PredictionView.MatchdayView letzteAnsicht;

    public TippenStufen einSpielMitAnstossIn(String gameId, Duration abstand) {
        games.save(Game.of(GameId.of(gameId), MATCHDAY, HOME, AWAY, clock.instant().plus(abstand),
                GameStatus.SCHEDULED, null, false));
        return self();
    }

    public TippenStufen einKontoMitNamenExistiertFuer(String name, String email) {
        accounts.save(Account.of(EmailAddress.of(email), DisplayName.of(name), clock.instant()));
        return self();
    }

    public TippenStufen tipptFuerDasSpiel(String email, String gameId, int heim, int gast) {
        letzterFehler = null;
        try {
            tippen.submitPrediction(EmailAddress.of(email), GameId.of(gameId), GameScore.of(heim, gast));
        } catch (RuntimeException e) {
            letzterFehler = e;
        }
        return self();
    }

    public TippenStufen vergehtDieZeitUm(Duration dauer) {
        clock.advance(dauer);
        return self();
    }

    public TippenStufen ruftDenSpieltagAbAls(String email) {
        letzteAnsicht = tippen.viewMatchday(EmailAddress.of(email), MATCHDAY);
        return self();
    }

    public TippenStufen istDerTippAbgelehntWorden() {
        assertThat(letzterFehler).as("der Tipp haette abgelehnt werden sollen").isNotNull();
        return self();
    }

    public TippenStufen istDerTippAngenommenWorden() {
        assertThat(letzterFehler).as("der Tipp haette angenommen werden sollen, war aber: " + letzterFehler).isNull();
        return self();
    }

    public TippenStufen zeigtFuerDasSpielDenEigenenTipp(String gameId, int heim, int gast) {
        assertThat(spielAus(gameId).ownPrediction()).isEqualTo(new PredictionView.ScoreView(heim, gast));
        return self();
    }

    public TippenStufen zeigtFuerDasSpielKeineFremdenTipps(String gameId) {
        assertThat(spielAus(gameId).otherPredictions()).isEmpty();
        return self();
    }

    public TippenStufen zeigtFuerDasSpielDenFremdenTippVon(String gameId, String erwarteterName, int heim, int gast) {
        assertThat(spielAus(gameId).otherPredictions())
                .contains(new PredictionView.PredictionEntryView(erwarteterName, new PredictionView.ScoreView(heim, gast)));
        return self();
    }

    private PredictionView.GameView spielAus(String gameId) {
        assertThat(letzteAnsicht).as("es haette bereits ein Spieltag abgerufen worden sein sollen").isNotNull();
        return letzteAnsicht.games().stream().filter(g -> g.gameId().equals(gameId)).findFirst().orElseThrow();
    }
}
