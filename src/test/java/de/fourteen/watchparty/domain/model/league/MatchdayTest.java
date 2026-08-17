package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class MatchdayTest {

    @Test
    void einSpieltagInnerhalbDerRegularSeasonIstGueltig() {
        Matchday matchday = Matchday.of(SeasonId.of(2026), 1);
        assertThat(matchday.season()).isEqualTo(SeasonId.of(2026));
        assertThat(matchday.week()).isEqualTo(1);
    }

    @Test
    void spieltagNullIstUngueltig() {
        assertThatThrownBy(() -> Matchday.of(SeasonId.of(2026), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void spieltagNeunzehnIstUngueltig() {
        assertThatThrownBy(() -> Matchday.of(SeasonId.of(2026), 19)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void spieltagAchtzehnIstDerLetzteGueltige() {
        assertThat(Matchday.of(SeasonId.of(2026), 18).week()).isEqualTo(18);
    }
}
