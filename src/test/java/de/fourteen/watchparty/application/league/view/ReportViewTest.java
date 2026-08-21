package de.fourteen.watchparty.application.league.view;

import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.Matchday;
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
 * {@link ReportView} als reine Projektion (13.9): Endergebnis, eigener
 * Tipp -- falls vorhanden -- und Wertungspunkte je gewertetem Spiel, dazu
 * die Spieltagssumme. Anders als {@code PredictionViewTest} ohne
 * Mutation-Score-Vorgabe, weil das Feature MEDIUM statt HIGH eingestuft
 * ist (docs/features/006-spieltags-report.md).
 */
@UnitTest
class ReportViewTest {

    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Instant KICKOFF = Instant.parse("2026-09-10T17:00:00Z");

    private static Game finalGame(String id, int homeScore, int awayScore) {
        return Game.of(GameId.of(id), MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.FINAL,
                GameScore.of(homeScore, awayScore), false);
    }

    @Test
    @Anforderung({ "13.9-a", "13.9-c" })
    void zeigtEndergebnisEigenenTippUndPunkte() {
        Game game = finalGame("1", 24, 17);

        ReportView.MatchdayReportView view = ReportView.matchday(MATCHDAY, List.of(game),
                Map.of(GameId.of("1"), GameScore.of(24, 17)));

        ReportView.GameEntryView entry = view.games().get(0);
        assertThat(entry.finalScore()).isEqualTo(new PredictionView.ScoreView(24, 17));
        assertThat(entry.ownPrediction()).isEqualTo(new PredictionView.ScoreView(24, 17));
        assertThat(entry.points()).isEqualTo(6);
    }

    @Test
    @Anforderung("13.9-c")
    void einGewertetesSpielOhneEigenenTippTraegtNullPunkteUndKeinenTipp() {
        Game game = finalGame("1", 24, 17);

        ReportView.MatchdayReportView view = ReportView.matchday(MATCHDAY, List.of(game), Map.of());

        ReportView.GameEntryView entry = view.games().get(0);
        assertThat(entry.ownPrediction()).isNull();
        assertThat(entry.points()).isEqualTo(0);
    }

    @Test
    void ohneUebergebeneSpieleBleibtDieBilanzLeer() {
        // Die eigentliche Filterung auf gewertete Spiele (13.9-b) sitzt in
        // PredictionService.matchdayReport, nicht hier -- diese Projektion
        // bekommt bereits nur FINAL-Spiele uebergeben (siehe SpieltagsBilanzScenarioTest).
        ReportView.MatchdayReportView view = ReportView.matchday(MATCHDAY, List.of(), Map.of());

        assertThat(view.games()).isEmpty();
    }

    @Test
    @Anforderung("13.9-d")
    void summiertDieWertungspunkteUeberAlleSpiele() {
        Game exact = finalGame("1", 24, 17);
        Game tendencyOnly = finalGame("2", 20, 10);

        ReportView.MatchdayReportView view = ReportView.matchday(MATCHDAY, List.of(exact, tendencyOnly), Map.of(
                GameId.of("1"), GameScore.of(24, 17),
                GameId.of("2"), GameScore.of(30, 10)));

        // Spiel 1 exakt (6), Spiel 2 nur die Tendenz (3): 9 insgesamt.
        assertThat(view.totalPoints()).isEqualTo(9);
    }

    @Test
    void traegtDieSpieltagsnummerAusDemMatchdayWeiter() {
        ReportView.MatchdayReportView view = ReportView.matchday(Matchday.of(SeasonId.of(2026), 7), List.of(), Map.of());

        assertThat(view.week()).isEqualTo(7);
    }
}
