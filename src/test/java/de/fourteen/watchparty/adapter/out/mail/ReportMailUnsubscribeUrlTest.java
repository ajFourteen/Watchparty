package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code /league/report-mail/unsubscribe/TOKEN} ist die Route, die das
 * Frontend abfaengt und einloest (frontend/src/league/useLeagueAccount.js)
 * -- diese eine Zeile ist die einzige Stelle, die beide Seiten zusammenhaelt
 * (13.9-p, ADR-041), analog zu {@link LoginLinkUrlTest}.
 */
@UnitTest
class ReportMailUnsubscribeUrlTest {

    @Test
    void zeigtAufDieFrontendRouteFuersAbmelden() {
        ReportMailToken token = ReportMailToken.generate();
        Account account = Account.of(EmailAddress.of("anna@example.org"), DisplayName.of("Anna"),
                Instant.parse("2026-08-17T20:00:00Z"), true, token);

        String url = ReportMailUnsubscribeUrl.of("https://watchparty.example.org", account);

        assertThat(url).isEqualTo("https://watchparty.example.org/league/report-mail/unsubscribe/" + token.value());
    }
}
