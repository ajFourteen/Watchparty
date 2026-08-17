package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class LoginLinkTest {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");
    private static final Duration VALIDITY = Duration.ofMinutes(15);

    private static LoginLink issued() {
        return LoginLink.issue(EmailAddress.of("anna@example.org"), DisplayName.of("Anna"), NOW, VALIDITY);
    }

    @Test
    @Anforderung("13.2-c")
    void einFrischAusgestellterLinkIstGueltig() {
        assertThat(issued().isValid(NOW)).isTrue();
    }

    @Test
    @Anforderung("13.2-c")
    void nachAblaufIstErUngueltig() {
        LoginLink link = issued();
        assertThat(link.isValid(NOW.plus(VALIDITY).plusSeconds(1))).isFalse();
    }

    @Test
    @Anforderung("13.2-c")
    void genauAmVerfallszeitpunktIstErSchonUngueltig() {
        LoginLink link = issued();
        assertThat(link.isValid(NOW.plus(VALIDITY))).isFalse();
    }

    @Test
    @Anforderung("13.2-c")
    void redeemMachtIhnDanachUngueltig() {
        LoginLink link = issued();
        link.redeem(NOW);
        assertThat(link.isValid(NOW)).isFalse();
        assertThat(link.isUsed()).isTrue();
    }

    @Test
    @Anforderung("13.2-c")
    void einVerbrauchterLinkLaesstSichNichtEinLoesen() {
        LoginLink link = issued();
        link.redeem(NOW);
        assertThatThrownBy(() -> link.redeem(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Anforderung("13.2-c")
    void einAbgelaufenerLinkLaesstSichNichtEinLoesen() {
        LoginLink link = issued();
        assertThatThrownBy(() -> link.redeem(NOW.plus(VALIDITY)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ofBautEinenBestehendenLinkUnveraendertWiederAuf() {
        LoginLinkToken token = LoginLinkToken.generate();
        EmailAddress email = EmailAddress.of("anna@example.org");
        DisplayName name = DisplayName.of("Anna");
        Instant expiresAt = NOW.plus(VALIDITY);

        LoginLink link = LoginLink.of(token, email, name, NOW, expiresAt, true);

        assertThat(link.getToken()).isEqualTo(token);
        assertThat(link.getEmail()).isEqualTo(email);
        assertThat(link.getDisplayName()).isEqualTo(name);
        assertThat(link.getCreatedAt()).isEqualTo(NOW);
        assertThat(link.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(link.isUsed()).isTrue();
    }
}
