package de.fourteen.watchparty.adapter.out.feed;

import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapping gegen eine aufgezeichnete ESPN-Antwort (ADR-037,
 * docs/teststrategie.md 2.3), nie gegen das echte Netz — ein Formatwechsel
 * bei ESPN bricht diese Aufzeichnung, nicht die Produktion, unbemerkt.
 */
@AdapterTest
class EspnScheduleFeedTest {

    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);

    private static String aufgezeichneteAntwort() {
        try (InputStream in = EspnScheduleFeedTest.class.getResourceAsStream("/feed/espn-scoreboard-sample.json")) {
            if (in == null) {
                throw new IllegalStateException("Testressource fehlt: feed/espn-scoreboard-sample.json");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Game> find(List<Game> games, String id) {
        return games.stream().filter(g -> g.getId().equals(GameId.of(id))).findFirst();
    }

    @Test
    void liestEinNochNichtBegonnenesSpiel() {
        List<Game> games = new EspnScheduleFeed("https://example.invalid").parse(aufgezeichneteAntwort(), MATCHDAY);

        Optional<Game> game = find(games, "401872656");
        assertThat(game).isPresent();
        assertThat(game.get().getMatchday()).isEqualTo(MATCHDAY);
        assertThat(game.get().getHomeTeam()).isEqualTo(Team.of(TeamId.of("KC"), "Kansas City Chiefs"));
        assertThat(game.get().getAwayTeam()).isEqualTo(Team.of(TeamId.of("DEN"), "Denver Broncos"));
        assertThat(game.get().getKickoff()).isEqualTo(Instant.parse("2026-09-10T00:20:00Z"));
        assertThat(game.get().getStatus()).isEqualTo(GameStatus.SCHEDULED);
        assertThat(game.get().getScore()).isNull();
    }

    @Test
    void liestEinBeendetesSpielMitErgebnis() {
        List<Game> games = new EspnScheduleFeed("https://example.invalid").parse(aufgezeichneteAntwort(), MATCHDAY);

        Optional<Game> game = find(games, "401872657");
        assertThat(game).isPresent();
        assertThat(game.get().getStatus()).isEqualTo(GameStatus.FINAL);
        assertThat(game.get().getScore()).isEqualTo(GameScore.of(24, 17));
    }

    @Test
    void liestEinAbgesagtesSpielOhneErgebnis() {
        List<Game> games = new EspnScheduleFeed("https://example.invalid").parse(aufgezeichneteAntwort(), MATCHDAY);

        Optional<Game> game = find(games, "401872658");
        assertThat(game).isPresent();
        assertThat(game.get().getStatus()).isEqualTo(GameStatus.CANCELLED);
        assertThat(game.get().getScore()).isNull();
    }

    @Test
    void ueberspringtEinenUnvollstaendigenEintragOhneDieUebrigenZuGefaehrden() {
        List<Game> games = new EspnScheduleFeed("https://example.invalid").parse(aufgezeichneteAntwort(), MATCHDAY);

        assertThat(find(games, "401872659")).isEmpty();
        assertThat(games).hasSize(3);
    }
}
