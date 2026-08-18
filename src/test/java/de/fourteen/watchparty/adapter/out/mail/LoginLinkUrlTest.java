package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLink;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code /league/login/TOKEN} ist die Route, die das Frontend abfaengt und
 * einloest (frontend/src/league/useLeagueAccount.js) -- diese eine Zeile
 * ist die einzige Stelle, die beide Seiten zusammenhaelt.
 */
@UnitTest
class LoginLinkUrlTest {

    @Test
    void zeigtAufDieFrontendRouteFuersEinloesen() {
        LoginLink link = LoginLink.issue(EmailAddress.of("anna@example.org"), DisplayName.of("Anna"),
                Instant.parse("2026-08-17T20:00:00Z"), Duration.ofMinutes(15));

        String url = LoginLinkUrl.of("https://watchparty.example.org", link);

        assertThat(url).isEqualTo("https://watchparty.example.org/league/login/" + link.getToken().value());
    }
}
