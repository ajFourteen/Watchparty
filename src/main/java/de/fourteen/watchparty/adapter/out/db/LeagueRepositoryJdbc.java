package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.application.league.port.out.LeagueRepository;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.LeagueName;
import de.fourteen.watchparty.domain.model.league.Membership;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link LeagueRepository} ueber {@link NamedParameterJdbcTemplate} (ADR-035).
 * {@link #save} schreibt die Ligazeile per Upsert und die Mitgliedschaften
 * als vollstaendigen Ersatz (loeschen, neu einfuegen) — bei der zu
 * erwartenden Groesse (eine Handvoll Mitspieler) einfacher richtig zu
 * halten als eine Differenzbildung, im selben Stil wie die uebrigen
 * Repositories dieses Projekts ohne eigene Transaktionsverwaltung.
 */
public class LeagueRepositoryJdbc implements LeagueRepository {

    private static final RowMapper<LeagueRow> LEAGUE_ROW_MAPPER = (rs, rowNum) -> new LeagueRow(
            LeagueId.of(UUID.fromString(rs.getString("id"))),
            SeasonId.of(rs.getInt("season_year")),
            LeagueCode.of(rs.getString("code")),
            LeagueName.of(rs.getString("name")),
            EmailAddress.of(rs.getString("manager_email")));

    private static final RowMapper<Membership> MEMBERSHIP_ROW_MAPPER = (rs, rowNum) -> Membership.of(
            EmailAddress.of(rs.getString("account_email")),
            rs.getTimestamp("joined_at").toInstant());

    private record LeagueRow(LeagueId id, SeasonId season, LeagueCode code, LeagueName name, EmailAddress managerEmail) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public LeagueRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(League league) {
        var params = new MapSqlParameterSource()
                .addValue("id", league.getId().value())
                .addValue("seasonYear", league.getSeason().year())
                .addValue("code", league.getCode().value())
                .addValue("name", league.getName().value())
                .addValue("managerEmail", league.getManagerEmail().value());
        jdbc.update("""
                INSERT INTO league (id, season_year, code, name, manager_email)
                VALUES (:id, :seasonYear, :code, :name, :managerEmail)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
                """, params);

        jdbc.update("DELETE FROM league_membership WHERE league_id = :leagueId",
                new MapSqlParameterSource("leagueId", league.getId().value()));
        for (Membership member : league.getMembers()) {
            jdbc.update("""
                    INSERT INTO league_membership (league_id, account_email, joined_at)
                    VALUES (:leagueId, :accountEmail, :joinedAt)
                    """, new MapSqlParameterSource()
                    .addValue("leagueId", league.getId().value())
                    .addValue("accountEmail", member.getAccountEmail().value())
                    .addValue("joinedAt", Timestamp.from(member.getJoinedAt())));
        }
    }

    @Override
    public Optional<League> findById(LeagueId id) {
        List<LeagueRow> found = jdbc.query(
                "SELECT id, season_year, code, name, manager_email FROM league WHERE id = :id",
                new MapSqlParameterSource("id", id.value()), LEAGUE_ROW_MAPPER);
        return found.stream().findFirst().map(this::toLeague);
    }

    @Override
    public Optional<League> findByCode(LeagueCode code) {
        List<LeagueRow> found = jdbc.query(
                "SELECT id, season_year, code, name, manager_email FROM league WHERE code = :code",
                new MapSqlParameterSource("code", code.value()), LEAGUE_ROW_MAPPER);
        return found.stream().findFirst().map(this::toLeague);
    }

    @Override
    public List<League> findByMember(EmailAddress account) {
        List<LeagueRow> found = jdbc.query("""
                SELECT l.id, l.season_year, l.code, l.name, l.manager_email
                FROM league l
                JOIN league_membership m ON m.league_id = l.id
                WHERE m.account_email = :accountEmail
                """, new MapSqlParameterSource("accountEmail", account.value()), LEAGUE_ROW_MAPPER);
        return found.stream().map(this::toLeague).toList();
    }

    private League toLeague(LeagueRow row) {
        List<Membership> members = jdbc.query(
                "SELECT account_email, joined_at FROM league_membership WHERE league_id = :leagueId",
                new MapSqlParameterSource("leagueId", row.id().value()), MEMBERSHIP_ROW_MAPPER);
        return League.of(row.id(), row.season(), row.code(), row.name(), row.managerEmail(), members);
    }
}
