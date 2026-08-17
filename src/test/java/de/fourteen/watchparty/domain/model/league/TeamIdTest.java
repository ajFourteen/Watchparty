package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class TeamIdTest {

    @Test
    void einKuerzelWirdUebernommen() {
        assertThat(TeamId.of("KC").value()).isEqualTo("KC");
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> TeamId.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringZeigtDasKuerzel() {
        assertThat(TeamId.of("KC")).hasToString("KC");
    }
}
