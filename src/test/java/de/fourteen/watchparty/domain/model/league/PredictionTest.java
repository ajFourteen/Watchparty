package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class PredictionTest {

    @Test
    @Anforderung("13.4-b")
    void traegtIdUndErgebnistipp() {
        PredictionId id = PredictionId.of(EmailAddress.of("anna@example.org"), GameId.of("1"));
        GameScore score = GameScore.of(24, 17);

        Prediction prediction = Prediction.of(id, score);

        assertThat(prediction.getId()).isEqualTo(id);
        assertThat(prediction.getScore()).isEqualTo(score);
    }
}
