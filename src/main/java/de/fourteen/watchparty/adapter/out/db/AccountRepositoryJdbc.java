package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.AccountId;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link AccountRepository} ueber {@link NamedParameterJdbcTemplate}
 * (ADR-035). {@link #save} ist ein Upsert ueber die Konto-ID: Stufe 2 legt
 * nur neue Konten an, Stufe 3 bringt mit dem Loeschen und moeglichen
 * Aenderungen des Anzeigenamens den zweiten Fall, ohne dass sich diese
 * Methode dafuer aendern muss.
 */
public class AccountRepositoryJdbc implements AccountRepository {

    private static final RowMapper<Account> ROW_MAPPER = (rs, rowNum) -> Account.of(
            AccountId.of(rs.getObject("id", UUID.class)),
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
                .addValue("id", account.getId().value())
                .addValue("email", account.getEmail().value())
                .addValue("displayName", account.getDisplayName().value())
                .addValue("createdAt", Timestamp.from(account.getCreatedAt()));
        jdbc.update("""
                INSERT INTO account (id, email, display_name, created_at)
                VALUES (:id, :email, :displayName, :createdAt)
                ON CONFLICT (id) DO UPDATE SET
                    email = EXCLUDED.email,
                    display_name = EXCLUDED.display_name
                """, params);
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        List<Account> found = jdbc.query(
                "SELECT id, email, display_name, created_at FROM account WHERE id = :id",
                new MapSqlParameterSource("id", id.value()),
                ROW_MAPPER);
        return found.stream().findFirst();
    }

    @Override
    public Optional<Account> findByEmail(EmailAddress email) {
        List<Account> found = jdbc.query(
                "SELECT id, email, display_name, created_at FROM account WHERE email = :email",
                new MapSqlParameterSource("email", email.value()),
                ROW_MAPPER);
        return found.stream().findFirst();
    }
}
