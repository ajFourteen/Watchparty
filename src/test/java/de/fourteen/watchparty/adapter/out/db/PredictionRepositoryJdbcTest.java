package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.adapter.out.db.support.PostgresAdapterSupport;
import de.fourteen.watchparty.domain.model.league.Account;
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
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rundlauf gegen echtes Postgres (ADR-035, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was {@link de.fourteen.watchparty.application.league.port.out.PredictionRepository}
 * ausdrueckt?
 */
@AdapterTest
class PredictionRepositoryJdbcTest extends PostgresAdapterSupport {

    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final EmailAddress ANNA = EmailAddress.of("anna@example.org");
    private static final GameId GAME_ID = GameId.of("1");

    private final AccountRepositoryJdbc accounts = new AccountRepositoryJdbc(JDBC);
    private final GameRepositoryJdbc games = new GameRepositoryJdbc(JDBC);
    private final PredictionRepositoryJdbc repository = new PredictionRepositoryJdbc(JDBC);

    private void legeKontoUndSpielAn() {
        accounts.save(Account.of(ANNA, DisplayName.of("Anna"), NOW));
        games.save(Game.of(GAME_ID, MATCHDAY, HOME, AWAY, NOW.plusSeconds(3600), GameStatus.SCHEDULED, null, false));
    }

    @Test
    void gespeicherterTippIstUeberDasSpielWiederAuffindbar() {
        legeKontoUndSpielAn();
        Prediction prediction = Prediction.of(PredictionId.of(ANNA, GAME_ID), GameScore.of(24, 17));

        repository.save(prediction);

        List<Prediction> gefunden = repository.findByGame(GAME_ID);
        assertThat(gefunden).hasSize(1);
        assertThat(gefunden.get(0).getId()).isEqualTo(prediction.getId());
        assertThat(gefunden.get(0).getScore()).isEqualTo(GameScore.of(24, 17));
    }

    @Test
    void unbekanntesSpielLiefertEineLeereListe() {
        assertThat(repository.findByGame(GameId.of("unbekannt"))).isEmpty();
    }

    @Test
    void speichernMitDerselbenIdAktualisiertStattZuDuplizieren() {
        legeKontoUndSpielAn();
        repository.save(Prediction.of(PredictionId.of(ANNA, GAME_ID), GameScore.of(24, 17)));

        repository.save(Prediction.of(PredictionId.of(ANNA, GAME_ID), GameScore.of(30, 20)));

        List<Prediction> gefunden = repository.findByGame(GAME_ID);
        assertThat(gefunden).hasSize(1);
        assertThat(gefunden.get(0).getScore()).isEqualTo(GameScore.of(30, 20));
    }
}
