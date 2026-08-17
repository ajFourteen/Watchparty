package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class SeasonIdTest {

    @Test
    void einJahrWirdUebernommen() {
        assertThat(SeasonId.of(2026).year()).isEqualTo(2026);
    }

    @Test
    void einUnplausiblesJahrIstUngueltig() {
        assertThatThrownBy(() -> SeasonId.of(1899)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringZeigtDasJahr() {
        assertThat(SeasonId.of(2026)).hasToString("2026");
    }
}
