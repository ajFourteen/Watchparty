package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.application.league.port.out.PredictionRepository;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Prediction;
import de.fourteen.watchparty.domain.model.league.PredictionId;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

/** {@link PredictionRepository} ueber {@link NamedParameterJdbcTemplate} (ADR-035). {@link #save} ist ein Upsert ueber (Konto, Spiel). */
public class PredictionRepositoryJdbc implements PredictionRepository {

    private static final RowMapper<Prediction> ROW_MAPPER = (rs, rowNum) -> Prediction.of(
            PredictionId.of(EmailAddress.of(rs.getString("account_email")), GameId.of(rs.getString("game_id"))),
            GameScore.of(rs.getInt("home_score"), rs.getInt("away_score")));

    private final NamedParameterJdbcTemplate jdbc;

    public PredictionRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Prediction prediction) {
        var params = new MapSqlParameterSource()
                .addValue("accountEmail", prediction.getId().accountEmail().value())
                .addValue("gameId", prediction.getId().gameId().value())
                .addValue("homeScore", prediction.getScore().home())
                .addValue("awayScore", prediction.getScore().away());
        jdbc.update("""
                INSERT INTO prediction (account_email, game_id, home_score, away_score)
                VALUES (:accountEmail, :gameId, :homeScore, :awayScore)
                ON CONFLICT (account_email, game_id) DO UPDATE SET
                    home_score = EXCLUDED.home_score,
                    away_score = EXCLUDED.away_score
                """, params);
    }

    @Override
    public List<Prediction> findByGame(GameId gameId) {
        return jdbc.query(
                "SELECT account_email, game_id, home_score, away_score FROM prediction WHERE game_id = :gameId",
                new MapSqlParameterSource("gameId", gameId.value()),
                ROW_MAPPER);
    }

    @Override
    public List<Prediction> findByAccount(EmailAddress account) {
        return jdbc.query(
                "SELECT account_email, game_id, home_score, away_score FROM prediction WHERE account_email = :accountEmail",
                new MapSqlParameterSource("accountEmail", account.value()),
                ROW_MAPPER);
    }
}
