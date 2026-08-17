package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.adapter.out.db.support.PostgresAdapterSupport;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLink;
import de.fourteen.watchparty.domain.model.league.LoginLinkToken;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rundlauf gegen echtes Postgres (ADR-035, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was {@link de.fourteen.watchparty.application.league.port.out.LoginLinkRepository}
 * ausdrueckt?
 */
@AdapterTest
class LoginLinkRepositoryJdbcTest extends PostgresAdapterSupport {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");
    private static final Duration VALIDITY = Duration.ofMinutes(15);

    private final LoginLinkRepositoryJdbc repository = new LoginLinkRepositoryJdbc(JDBC);

    @Test
    void gespeicherterLinkIstUeberDenTokenWiederAuffindbar() {
        LoginLink link = LoginLink.issue(EmailAddress.of("anna@example.org"), DisplayName.of("Anna"), NOW, VALIDITY);

        repository.save(link);

        Optional<LoginLink> gefunden = repository.findByToken(link.getToken());
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getToken()).isEqualTo(link.getToken());
        assertThat(gefunden.get().getEmail()).isEqualTo(link.getEmail());
        assertThat(gefunden.get().getDisplayName()).isEqualTo(link.getDisplayName());
        assertThat(gefunden.get().getCreatedAt()).isEqualTo(link.getCreatedAt());
        assertThat(gefunden.get().getExpiresAt()).isEqualTo(link.getExpiresAt());
        assertThat(gefunden.get().isUsed()).isFalse();
    }

    @Test
    void unbekannterTokenLiefertLeer() {
        assertThat(repository.findByToken(LoginLinkToken.generate())).isEmpty();
    }

    @Test
    void verbrauchtStatusWirdMitgespeichert() {
        LoginLink link = LoginLink.issue(EmailAddress.of("anna@example.org"), DisplayName.of("Anna"), NOW, VALIDITY);
        repository.save(link);

        link.redeem(NOW);
        repository.save(link);

        Optional<LoginLink> gefunden = repository.findByToken(link.getToken());
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().isUsed()).isTrue();
    }
}
