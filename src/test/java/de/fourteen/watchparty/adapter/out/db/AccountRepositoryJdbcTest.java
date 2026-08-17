package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.adapter.out.db.support.PostgresAdapterSupport;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rundlauf gegen echtes Postgres (ADR-035, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was {@link de.fourteen.watchparty.application.league.port.out.AccountRepository}
 * ausdrueckt? Fixture entsteht direkt ueber {@link Account}, ohne Umweg ueber
 * eine Anwendungsschicht, die es fuer Konten in dieser Stufe noch nicht gibt.
 */
@AdapterTest
class AccountRepositoryJdbcTest extends PostgresAdapterSupport {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");

    private final AccountRepositoryJdbc repository = new AccountRepositoryJdbc(JDBC);

    @Test
    void gespeichertesKontoIstUeberDieEmailWiederAuffindbar() {
        Account account = Account.of(EmailAddress.of("anna@example.org"), DisplayName.of("Anna"), NOW);

        repository.save(account);

        Optional<Account> gefunden = repository.findByEmail(EmailAddress.of("anna@example.org"));
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getEmail()).isEqualTo(account.getEmail());
        assertThat(gefunden.get().getDisplayName()).isEqualTo(account.getDisplayName());
        assertThat(gefunden.get().getCreatedAt()).isEqualTo(account.getCreatedAt());
    }

    @Test
    void unbekannteEmailLiefertLeer() {
        assertThat(repository.findByEmail(EmailAddress.of("niemand@example.org"))).isEmpty();
    }

    @Test
    void speichernMitDerselbenEmailAktualisiertStattZuDuplizieren() {
        repository.save(Account.of(EmailAddress.of("anna@example.org"), DisplayName.of("Anna"), NOW));
        repository.save(Account.of(EmailAddress.of("anna@example.org"), DisplayName.of("Anna B"), NOW));

        Optional<Account> gefunden = repository.findByEmail(EmailAddress.of("anna@example.org"));
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getDisplayName()).isEqualTo(DisplayName.of("Anna B"));
    }

    @Test
    void geloeschtesKontoIstDanachNichtMehrAuffindbar() {
        EmailAddress email = EmailAddress.of("anna@example.org");
        repository.save(Account.of(email, DisplayName.of("Anna"), NOW));

        repository.delete(email);

        assertThat(repository.findByEmail(email)).isEmpty();
    }
}
