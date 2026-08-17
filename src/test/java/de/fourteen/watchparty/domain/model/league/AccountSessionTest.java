package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class AccountSessionTest {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");
    private static final Duration NINETY_DAYS = Duration.ofDays(90);

    @Test
    @Anforderung("13.2-f")
    void istInnerhalbVonNeunzigTagenGueltig() {
        AccountSession session = AccountSession.start(EmailAddress.of("anna@example.org"), NOW, NINETY_DAYS);
        assertThat(session.isValid(NOW.plus(NINETY_DAYS).minusSeconds(1))).isTrue();
    }

    @Test
    @Anforderung("13.2-f")
    void istNachNeunzigTagenAbgelaufen() {
        AccountSession session = AccountSession.start(EmailAddress.of("anna@example.org"), NOW, NINETY_DAYS);
        assertThat(session.isValid(NOW.plus(NINETY_DAYS))).isFalse();
    }

    @Test
    void ofBautEineBestehendeSitzungUnveraendertWiederAuf() {
        SessionToken token = SessionToken.generate();
        EmailAddress email = EmailAddress.of("anna@example.org");
        Instant expiresAt = NOW.plus(NINETY_DAYS);

        AccountSession session = AccountSession.of(token, email, NOW, expiresAt);

        assertThat(session.getToken()).isEqualTo(token);
        assertThat(session.getAccountEmail()).isEqualTo(email);
        assertThat(session.getCreatedAt()).isEqualTo(NOW);
        assertThat(session.getExpiresAt()).isEqualTo(expiresAt);
    }
}
