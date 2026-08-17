package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.adapter.out.db.support.PostgresAdapterSupport;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rundlauf gegen echtes Postgres (ADR-035, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was {@link de.fourteen.watchparty.application.league.port.out.GameRepository}
 * ausdrueckt?
 */
@AdapterTest
class GameRepositoryJdbcTest extends PostgresAdapterSupport {

    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Instant KICKOFF = Instant.parse("2026-09-10T17:00:00Z");

    private final GameRepositoryJdbc repository = new GameRepositoryJdbc(JDBC);

    @Test
    void einGeplantesSpielOhneErgebnisIstUeberDieIdWiederAuffindbar() {
        Game game = Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.SCHEDULED, null, false);

        repository.save(game);

        Optional<Game> gefunden = repository.findById(GameId.of("1"));
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getMatchday()).isEqualTo(MATCHDAY);
        assertThat(gefunden.get().getHomeTeam()).isEqualTo(HOME);
        assertThat(gefunden.get().getAwayTeam()).isEqualTo(AWAY);
        assertThat(gefunden.get().getKickoff()).isEqualTo(KICKOFF);
        assertThat(gefunden.get().getStatus()).isEqualTo(GameStatus.SCHEDULED);
        assertThat(gefunden.get().getScore()).isNull();
        assertThat(gefunden.get().isManualOverride()).isFalse();
    }

    @Test
    void einBeendetesSpielTraegtSeinErgebnisUndDenHandeintragVermerk() {
        Game game = Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF,
                GameStatus.FINAL, GameScore.of(24, 17), true);

        repository.save(game);

        Optional<Game> gefunden = repository.findById(GameId.of("1"));
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getStatus()).isEqualTo(GameStatus.FINAL);
        assertThat(gefunden.get().getScore()).isEqualTo(GameScore.of(24, 17));
        assertThat(gefunden.get().isManualOverride()).isTrue();
    }

    @Test
    void unbekannteIdLiefertLeer() {
        assertThat(repository.findById(GameId.of("unbekannt"))).isEmpty();
    }

    @Test
    void speichernMitDerselbenIdAktualisiertStattZuDuplizieren() {
        repository.save(Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.SCHEDULED, null, false));
        repository.save(Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF,
                GameStatus.FINAL, GameScore.of(24, 17), false));

        Optional<Game> gefunden = repository.findById(GameId.of("1"));
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getStatus()).isEqualTo(GameStatus.FINAL);
        assertThat(gefunden.get().getScore()).isEqualTo(GameScore.of(24, 17));
    }

    @Test
    void findByMatchdayLiefertNurSpieleDesGefragtenSpieltags() {
        repository.save(Game.of(GameId.of("1"), MATCHDAY, HOME, AWAY, KICKOFF, GameStatus.SCHEDULED, null, false));
        Matchday andererSpieltag = Matchday.of(SeasonId.of(2026), 2);
        repository.save(Game.of(GameId.of("2"), andererSpieltag, HOME, AWAY, KICKOFF, GameStatus.SCHEDULED, null, false));

        List<Game> gefunden = repository.findByMatchday(MATCHDAY);

        assertThat(gefunden).extracting(g -> g.getId().value()).containsExactly("1");
    }
}
