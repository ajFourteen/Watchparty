package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class GameTest {

    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Instant KICKOFF = Instant.parse("2026-09-10T17:00:00Z");

    private static Game scheduled() {
        return Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.SCHEDULED, null, false);
    }

    @Test
    void traegtDieBeimAnlegenUebergebenenWerte() {
        Game game = scheduled();

        assertThat(game.getId()).isEqualTo(GameId.of("1"));
        assertThat(game.getMatchday()).isEqualTo(MATCHDAY);
        assertThat(game.getHomeTeam()).isEqualTo(HOME);
        assertThat(game.getAwayTeam()).isEqualTo(AWAY);
        assertThat(game.getKickoff()).isEqualTo(KICKOFF);
        assertThat(game.getStatus()).isEqualTo(GameStatus.SCHEDULED);
        assertThat(game.getScore()).isNull();
        assertThat(game.isManualOverride()).isFalse();
    }

    @Test
    void einNochNichtBeendetesSpielHatKeinErgebnis() {
        assertThatThrownBy(() -> Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF,
                GameStatus.SCHEDULED, GameScore.of(10, 7), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void einBeendetesSpielBrauchtEinErgebnis() {
        assertThatThrownBy(() -> Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF,
                GameStatus.FINAL, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Anforderung("13.3-c")
    void mergeFromFeedUebernimmtEineVerlegteAnstosszeit() {
        Game game = scheduled();
        Instant neuerAnstoss = KICKOFF.plusSeconds(3600);
        Game feedGame = Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, neuerAnstoss, GameStatus.SCHEDULED, null, false);

        game.mergeFromFeed(feedGame);

        assertThat(game.getKickoff()).isEqualTo(neuerAnstoss);
    }

    @Test
    @Anforderung("13.3-e")
    void mergeFromFeedUebernimmtEinNeuesEndergebnis() {
        Game game = scheduled();
        Game feedGame = Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF,
                GameStatus.FINAL, GameScore.of(24, 17), false);

        game.mergeFromFeed(feedGame);

        assertThat(game.getStatus()).isEqualTo(GameStatus.FINAL);
        assertThat(game.getScore()).isEqualTo(GameScore.of(24, 17));
    }

    @Test
    @Anforderung("13.3-f")
    void mergeFromFeedUebernimmtEineAbsage() {
        Game game = scheduled();
        Game feedGame = Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.CANCELLED, null, false);

        game.mergeFromFeed(feedGame);

        assertThat(game.getStatus()).isEqualTo(GameStatus.CANCELLED);
        assertThat(game.getScore()).isNull();
    }

    @Test
    @Anforderung("13.3-g")
    void einHandeintragUeberschreibtDenFeed() {
        Game game = scheduled();

        game.applyManualResult(GameScore.of(30, 20));

        assertThat(game.getStatus()).isEqualTo(GameStatus.FINAL);
        assertThat(game.getScore()).isEqualTo(GameScore.of(30, 20));
        assertThat(game.isManualOverride()).isTrue();
    }

    @Test
    @Anforderung("13.3-g")
    void derFeedKannEinenHandeintragNichtZuruecknehmen() {
        Game game = scheduled();
        game.applyManualResult(GameScore.of(30, 20));

        Game widersprechendesFeedErgebnis = Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF,
                GameStatus.FINAL, GameScore.of(24, 17), false);
        game.mergeFromFeed(widersprechendesFeedErgebnis);

        assertThat(game.getScore()).isEqualTo(GameScore.of(30, 20));
        assertThat(game.isManualOverride()).isTrue();
    }

    @Test
    @Anforderung("13.3-g")
    void einHandeintragLaesstDieAnstosszeitDesFeedsUnberuehrtNichtStehen() {
        // Die Anstosszeit uebernimmt weiterhin der Feed (Kriterium 10 gilt unabhaengig vom Handeintrag).
        Game game = scheduled();
        game.applyManualResult(GameScore.of(30, 20));

        Instant neuerAnstoss = KICKOFF.plusSeconds(3600);
        Game feedGame = Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, neuerAnstoss, GameStatus.SCHEDULED, null, false);
        game.mergeFromFeed(feedGame);

        assertThat(game.getKickoff()).isEqualTo(neuerAnstoss);
    }
}
