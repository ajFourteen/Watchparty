package de.fourteen.watchparty.adapter.out.ratelimit;

import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Das gleitende Zeitfenster selbst (Kriterium 4) — kein Netz, kein Container, aber Adapter-Logik. */
@AdapterTest
class InMemoryRateLimiterTest {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");

    @Test
    void erlaubtBisZurGrenze() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(3, Duration.ofMinutes(15));

        assertThat(limiter.allow("anna", NOW)).isTrue();
        assertThat(limiter.allow("anna", NOW)).isTrue();
        assertThat(limiter.allow("anna", NOW)).isTrue();
        assertThat(limiter.allow("anna", NOW)).isFalse();
    }

    @Test
    void verschiedeneSchluesselStoerenSichNicht() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, Duration.ofMinutes(15));

        assertThat(limiter.allow("anna", NOW)).isTrue();
        assertThat(limiter.allow("ben", NOW)).isTrue();
    }

    @Test
    void nachAblaufDesFenstersIstWiederPlatz() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, Duration.ofMinutes(15));

        assertThat(limiter.allow("anna", NOW)).isTrue();
        assertThat(limiter.allow("anna", NOW.plus(Duration.ofMinutes(15)).plusSeconds(1))).isTrue();
    }

    @Test
    void kurzVorAblaufNochBlockiert() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, Duration.ofMinutes(15));

        assertThat(limiter.allow("anna", NOW)).isTrue();
        assertThat(limiter.allow("anna", NOW.plus(Duration.ofMinutes(15)).minusSeconds(1))).isFalse();
    }
}
