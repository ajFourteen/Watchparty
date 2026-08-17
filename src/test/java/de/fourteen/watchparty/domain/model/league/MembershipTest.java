package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class MembershipTest {

    @Test
    void traegtKontoUndBeitrittszeitpunkt() {
        EmailAddress email = EmailAddress.of("anna@example.org");
        Instant now = Instant.parse("2026-08-17T20:00:00Z");

        Membership membership = Membership.of(email, now);

        assertThat(membership.getAccountEmail()).isEqualTo(email);
        assertThat(membership.getJoinedAt()).isEqualTo(now);
    }
}
