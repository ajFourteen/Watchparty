package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.application.league.port.out.AccountSessionRepository;
import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.SessionToken;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * {@link AccountSessionRepository} ueber {@link NamedParameterJdbcTemplate}
 * (ADR-035).
 */
public class AccountSessionRepositoryJdbc implements AccountSessionRepository {

    private static final RowMapper<AccountSession> ROW_MAPPER = (rs, rowNum) -> AccountSession.of(
            SessionToken.of(rs.getString("token")),
            EmailAddress.of(rs.getString("account_email")),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public AccountSessionRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(AccountSession session) {
        var params = new MapSqlParameterSource()
                .addValue("token", session.getToken().value())
                .addValue("accountEmail", session.getAccountEmail().value())
                .addValue("createdAt", Timestamp.from(session.getCreatedAt()))
                .addValue("expiresAt", Timestamp.from(session.getExpiresAt()));
        jdbc.update("""
                INSERT INTO account_session (token, account_email, created_at, expires_at)
                VALUES (:token, :accountEmail, :createdAt, :expiresAt)
                ON CONFLICT (token) DO NOTHING
                """, params);
    }

    @Override
    public Optional<AccountSession> findByToken(SessionToken token) {
        List<AccountSession> found = jdbc.query(
                "SELECT token, account_email, created_at, expires_at FROM account_session WHERE token = :token",
                new MapSqlParameterSource("token", token.value()),
                ROW_MAPPER);
        return found.stream().findFirst();
    }

    @Override
    public void deleteByAccountEmail(EmailAddress accountEmail) {
        jdbc.update(
                "DELETE FROM account_session WHERE account_email = :accountEmail",
                new MapSqlParameterSource("accountEmail", accountEmail.value()));
    }
}
