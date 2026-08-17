package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** {@link GameRepository} ueber {@link NamedParameterJdbcTemplate} (ADR-035). {@link #save} ist ein Upsert ueber die Spiel-ID. */
public class GameRepositoryJdbc implements GameRepository {

    private static final RowMapper<Game> ROW_MAPPER = (rs, rowNum) -> Game.of(
            GameId.of(rs.getString("id")),
            Matchday.of(SeasonId.of(rs.getInt("season_year")), rs.getInt("week")),
            Team.of(TeamId.of(rs.getString("home_team_id")), rs.getString("home_team_name")),
            Team.of(TeamId.of(rs.getString("away_team_id")), rs.getString("away_team_name")),
            rs.getTimestamp("kickoff").toInstant(),
            GameStatus.valueOf(rs.getString("status")),
            scoreFrom(rs),
            rs.getBoolean("manual_override"));

    private final NamedParameterJdbcTemplate jdbc;

    public GameRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static @Nullable GameScore scoreFrom(ResultSet rs) throws SQLException {
        int home = rs.getInt("home_score");
        if (rs.wasNull()) {
            return null;
        }
        int away = rs.getInt("away_score");
        return GameScore.of(home, away);
    }

    @Override
    public void save(Game game) {
        GameScore score = game.getScore();
        var params = new MapSqlParameterSource()
                .addValue("id", game.getId().value())
                .addValue("seasonYear", game.getMatchday().season().year())
                .addValue("week", game.getMatchday().week())
                .addValue("homeTeamId", game.getHomeTeam().id().value())
                .addValue("homeTeamName", game.getHomeTeam().name())
                .addValue("awayTeamId", game.getAwayTeam().id().value())
                .addValue("awayTeamName", game.getAwayTeam().name())
                .addValue("kickoff", Timestamp.from(game.getKickoff()))
                .addValue("status", game.getStatus().name())
                .addValue("homeScore", score == null ? null : score.home())
                .addValue("awayScore", score == null ? null : score.away())
                .addValue("manualOverride", game.isManualOverride());
        jdbc.update("""
                INSERT INTO game (id, season_year, week, home_team_id, home_team_name,
                    away_team_id, away_team_name, kickoff, status, home_score, away_score, manual_override)
                VALUES (:id, :seasonYear, :week, :homeTeamId, :homeTeamName,
                    :awayTeamId, :awayTeamName, :kickoff, :status, :homeScore, :awayScore, :manualOverride)
                ON CONFLICT (id) DO UPDATE SET
                    kickoff = EXCLUDED.kickoff,
                    status = EXCLUDED.status,
                    home_score = EXCLUDED.home_score,
                    away_score = EXCLUDED.away_score,
                    manual_override = EXCLUDED.manual_override
                """, params);
    }

    @Override
    public Optional<Game> findById(GameId id) {
        List<Game> found = jdbc.query(
                "SELECT * FROM game WHERE id = :id",
                new MapSqlParameterSource("id", id.value()),
                ROW_MAPPER);
        return found.stream().findFirst();
    }

    @Override
    public List<Game> findByMatchday(Matchday matchday) {
        var params = new MapSqlParameterSource()
                .addValue("seasonYear", matchday.season().year())
                .addValue("week", matchday.week());
        return jdbc.query(
                "SELECT * FROM game WHERE season_year = :seasonYear AND week = :week",
                params, ROW_MAPPER);
    }

    @Override
    public List<Game> findBySeason(SeasonId season) {
        return jdbc.query(
                "SELECT * FROM game WHERE season_year = :seasonYear",
                new MapSqlParameterSource("seasonYear", season.year()),
                ROW_MAPPER);
    }
}
