package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class PredictionIdTest {

    @Test
    void gleichesKontoUndGleichesSpielSindGleich() {
        PredictionId a = PredictionId.of(EmailAddress.of("anna@example.org"), GameId.of("1"));
        PredictionId b = PredictionId.of(EmailAddress.of("anna@example.org"), GameId.of("1"));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void unterschiedlichesSpielIstEinAnderesPaar() {
        PredictionId a = PredictionId.of(EmailAddress.of("anna@example.org"), GameId.of("1"));
        PredictionId b = PredictionId.of(EmailAddress.of("anna@example.org"), GameId.of("2"));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void unterschiedlichesKontoIstEinAnderesPaar() {
        PredictionId a = PredictionId.of(EmailAddress.of("anna@example.org"), GameId.of("1"));
        PredictionId b = PredictionId.of(EmailAddress.of("ben@example.org"), GameId.of("1"));
        assertThat(a).isNotEqualTo(b);
    }
}
