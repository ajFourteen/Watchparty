package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class AccountTest {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");

    @Test
    void ofBautEinKontoAusSeinenWertenAuf() {
        EmailAddress email = EmailAddress.of("anna@example.org");
        DisplayName name = DisplayName.of("Anna");

        Account account = Account.of(email, name, NOW);

        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.getDisplayName()).isEqualTo(name);
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
    }
}
