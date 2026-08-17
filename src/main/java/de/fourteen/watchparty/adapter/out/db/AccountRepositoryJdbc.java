package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * {@link AccountRepository} ueber {@link NamedParameterJdbcTemplate}
 * (ADR-035). {@link #save} ist ein Upsert ueber die E-Mail-Adresse — die
 * Identitaet des Kontos (kein separates {@code AccountId}).
 */
public class AccountRepositoryJdbc implements AccountRepository {

    private static final RowMapper<Account> ROW_MAPPER = (rs, rowNum) -> Account.of(
            EmailAddress.of(rs.getString("email")),
            DisplayName.of(rs.getString("display_name")),
            rs.getTimestamp("created_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public AccountRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Account account) {
        var params = new MapSqlParameterSource()
                .addValue("email", account.getEmail().value())
                .addValue("displayName", account.getDisplayName().value())
                .addValue("createdAt", Timestamp.from(account.getCreatedAt()));
        jdbc.update("""
                INSERT INTO account (email, display_name, created_at)
                VALUES (:email, :displayName, :createdAt)
                ON CONFLICT (email) DO UPDATE SET
                    display_name = EXCLUDED.display_name
                """, params);
    }

    @Override
    public Optional<Account> findByEmail(EmailAddress email) {
        List<Account> found = jdbc.query(
                "SELECT email, display_name, created_at FROM account WHERE email = :email",
                new MapSqlParameterSource("email", email.value()),
                ROW_MAPPER);
        return found.stream().findFirst();
    }

    @Override
    public void delete(EmailAddress email) {
        jdbc.update(
                "DELETE FROM account WHERE email = :email",
                new MapSqlParameterSource("email", email.value()));
    }
}
