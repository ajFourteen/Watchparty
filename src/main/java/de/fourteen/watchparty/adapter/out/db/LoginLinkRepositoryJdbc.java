package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.application.league.port.out.LoginLinkRepository;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLink;
import de.fourteen.watchparty.domain.model.league.LoginLinkToken;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * {@link LoginLinkRepository} ueber {@link NamedParameterJdbcTemplate}
 * (ADR-035). {@link #save} ist ein Upsert ueber den Token — nach der
 * Ausstellung aendert sich nur noch {@code used} beim Einloesen.
 */
public class LoginLinkRepositoryJdbc implements LoginLinkRepository {

    private static final RowMapper<LoginLink> ROW_MAPPER = (rs, rowNum) -> LoginLink.of(
            LoginLinkToken.of(rs.getString("token")),
            EmailAddress.of(rs.getString("email")),
            DisplayName.of(rs.getString("display_name")),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getBoolean("used"));

    private final NamedParameterJdbcTemplate jdbc;

    public LoginLinkRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(LoginLink link) {
        var params = new MapSqlParameterSource()
                .addValue("token", link.getToken().value())
                .addValue("email", link.getEmail().value())
                .addValue("displayName", link.getDisplayName().value())
                .addValue("createdAt", Timestamp.from(link.getCreatedAt()))
                .addValue("expiresAt", Timestamp.from(link.getExpiresAt()))
                .addValue("used", link.isUsed());
        jdbc.update("""
                INSERT INTO login_link (token, email, display_name, created_at, expires_at, used)
                VALUES (:token, :email, :displayName, :createdAt, :expiresAt, :used)
                ON CONFLICT (token) DO UPDATE SET used = EXCLUDED.used
                """, params);
    }

    @Override
    public Optional<LoginLink> findByToken(LoginLinkToken token) {
        List<LoginLink> found = jdbc.query(
                "SELECT token, email, display_name, created_at, expires_at, used FROM login_link WHERE token = :token",
                new MapSqlParameterSource("token", token.value()),
                ROW_MAPPER);
        return found.stream().findFirst();
    }
}
