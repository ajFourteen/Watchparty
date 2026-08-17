package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.adapter.out.db.support.PostgresAdapterSupport;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.SessionToken;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rundlauf gegen echtes Postgres (ADR-035, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was {@link de.fourteen.watchparty.application.league.port.out.AccountSessionRepository}
 * ausdrueckt?
 */
@AdapterTest
class AccountSessionRepositoryJdbcTest extends PostgresAdapterSupport {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");
    private static final Duration NINETY_DAYS = Duration.ofDays(90);

    private final AccountRepositoryJdbc accounts = new AccountRepositoryJdbc(JDBC);
    private final AccountSessionRepositoryJdbc repository = new AccountSessionRepositoryJdbc(JDBC);

    private static EmailAddress annaLegtEinKontoAn(AccountRepositoryJdbc accounts) {
        EmailAddress email = EmailAddress.of("anna@example.org");
        accounts.save(Account.of(email, DisplayName.of("Anna"), NOW));
        return email;
    }

    @Test
    void gespeicherteSitzungIstUeberDenTokenWiederAuffindbar() {
        EmailAddress email = annaLegtEinKontoAn(accounts);
        AccountSession session = AccountSession.start(email, NOW, NINETY_DAYS);

        repository.save(session);

        Optional<AccountSession> gefunden = repository.findByToken(session.getToken());
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getToken()).isEqualTo(session.getToken());
        assertThat(gefunden.get().getAccountEmail()).isEqualTo(email);
        assertThat(gefunden.get().getCreatedAt()).isEqualTo(session.getCreatedAt());
        assertThat(gefunden.get().getExpiresAt()).isEqualTo(session.getExpiresAt());
    }

    @Test
    void unbekannterTokenLiefertLeer() {
        assertThat(repository.findByToken(SessionToken.generate())).isEmpty();
    }

    @Test
    void loeschenNachKontoEntferntDieSitzung() {
        EmailAddress email = annaLegtEinKontoAn(accounts);
        AccountSession session = AccountSession.start(email, NOW, NINETY_DAYS);
        repository.save(session);

        repository.deleteByAccountEmail(email);

        assertThat(repository.findByToken(session.getToken())).isEmpty();
    }
}
