package de.fourteen.watchparty.application.league.view;

import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.Prediction;
import de.fourteen.watchparty.domain.model.league.PredictionId;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PredictionView} ist die HIGH-kritische Sichtbarkeitsregel des
 * Tippspiels (Kriterium 19/20) — Mutation Score ≥ 99 %, deshalb hier jeder
 * Zweig einzeln: vor/genau am/nach dem Anstoss, eigener Tipp vorhanden/
 * fehlend, fremde Tipps vorhanden/fehlend, Endergebnis vorhanden/fehlend.
 */
@UnitTest
class PredictionViewTest {

    private static final EmailAddress ANNA = EmailAddress.of("anna@example.org");
    private static final EmailAddress BEN = EmailAddress.of("ben@example.org");
    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Instant KICKOFF = Instant.parse("2026-09-10T17:00:00Z");
    private static final GameId GAME_ID = GameId.of("1");

    private static final Map<EmailAddress, DisplayName> NAMEN = Map.of(
            ANNA, DisplayName.of("Anna"),
            BEN, DisplayName.of("Ben"));

    private static Game scheduled() {
        return Game.of(GAME_ID, MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.SCHEDULED, null, false);
    }

    private static DisplayName nameOf(EmailAddress email) {
        return NAMEN.get(email);
    }

    @Test
    @Anforderung("13.4-a")
    void bildetDieGrunddatenDesSpielsAb() {
        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.minusSeconds(1), scheduled(), List.of(), PredictionViewTest::nameOf);

        assertThat(view.gameId()).isEqualTo("1");
        assertThat(view.homeTeamName()).isEqualTo("Kansas City Chiefs");
        assertThat(view.awayTeamName()).isEqualTo("San Francisco 49ers");
        assertThat(view.kickoff()).isEqualTo(KICKOFF);
        assertThat(view.status()).isEqualTo("SCHEDULED");
    }

    @Test
    @Anforderung("13.4-e")
    void derEigeneTippIstVorDemAnstossSichtbar() {
        Prediction annasTipp = Prediction.of(PredictionId.of(ANNA, GAME_ID), GameScore.of(24, 17));

        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.minusSeconds(1), scheduled(),
                List.of(annasTipp), PredictionViewTest::nameOf);

        assertThat(view.ownPrediction()).isEqualTo(new PredictionView.ScoreView(24, 17));
    }

    @Test
    @Anforderung("13.4-e")
    void findetDenEigenenTippAuchWennEinFremderZuerstInDerListeSteht() {
        Prediction bensTipp = Prediction.of(PredictionId.of(BEN, GAME_ID), GameScore.of(20, 10));
        Prediction annasTipp = Prediction.of(PredictionId.of(ANNA, GAME_ID), GameScore.of(24, 17));

        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.minusSeconds(1), scheduled(),
                List.of(bensTipp, annasTipp), PredictionViewTest::nameOf);

        assertThat(view.ownPrediction()).isEqualTo(new PredictionView.ScoreView(24, 17));
    }

    @Test
    @Anforderung("13.4-e")
    void ohneEigenenTippIstOwnPredictionLeer() {
        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.minusSeconds(1), scheduled(),
                List.of(), PredictionViewTest::nameOf);

        assertThat(view.ownPrediction()).isNull();
    }

    @Test
    @Anforderung("13.4-d")
    void fremdeTippsSindVorDemAnstossNichtTeilDerAntwort() {
        Prediction bensTipp = Prediction.of(PredictionId.of(BEN, GAME_ID), GameScore.of(20, 10));

        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.minusSeconds(1), scheduled(),
                List.of(bensTipp), PredictionViewTest::nameOf);

        assertThat(view.otherPredictions()).isEmpty();
    }

    @Test
    @Anforderung("13.4-d")
    void genauAmAnstosszeitpunktGiltErBereitsAlsAngepfiffen() {
        Prediction bensTipp = Prediction.of(PredictionId.of(BEN, GAME_ID), GameScore.of(20, 10));

        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF, scheduled(), List.of(bensTipp), PredictionViewTest::nameOf);

        assertThat(view.otherPredictions()).hasSize(1);
    }

    @Test
    @Anforderung("13.4-d")
    void fremdeTippsSindNachDemAnstossMitNamenSichtbar() {
        Prediction bensTipp = Prediction.of(PredictionId.of(BEN, GAME_ID), GameScore.of(20, 10));

        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.plusSeconds(1), scheduled(),
                List.of(bensTipp), PredictionViewTest::nameOf);

        assertThat(view.otherPredictions())
                .containsExactly(new PredictionView.PredictionEntryView("Ben", new PredictionView.ScoreView(20, 10)));
    }

    @Test
    @Anforderung("13.4-d")
    void derEigeneTippTauchtNachDemAnstossNichtNochEinmalBeiDenFremdenAuf() {
        Prediction annasTipp = Prediction.of(PredictionId.of(ANNA, GAME_ID), GameScore.of(24, 17));
        Prediction bensTipp = Prediction.of(PredictionId.of(BEN, GAME_ID), GameScore.of(20, 10));

        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.plusSeconds(1), scheduled(),
                List.of(annasTipp, bensTipp), PredictionViewTest::nameOf);

        assertThat(view.otherPredictions()).extracting(PredictionView.PredictionEntryView::displayName).containsExactly("Ben");
    }

    @Test
    void einNochNichtBeendetesSpielHatKeinEndergebnisInDerAntwort() {
        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.plusSeconds(1), scheduled(), List.of(), PredictionViewTest::nameOf);

        assertThat(view.finalScore()).isNull();
    }

    @Test
    void einBeendetesSpielTraegtSeinEndergebnisInDerAntwort() {
        Game beendet = Game.of(GAME_ID, MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.FINAL, GameScore.of(24, 17), false);

        PredictionView.GameView view = PredictionView.game(ANNA, KICKOFF.plusSeconds(1), beendet, List.of(), PredictionViewTest::nameOf);

        assertThat(view.finalScore()).isEqualTo(new PredictionView.ScoreView(24, 17));
    }
}
