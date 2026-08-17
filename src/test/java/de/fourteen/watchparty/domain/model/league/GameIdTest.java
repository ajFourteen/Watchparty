package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class GameIdTest {

    @Test
    void einWertWirdUebernommen() {
        assertThat(GameId.of("401872656").value()).isEqualTo("401872656");
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> GameId.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringZeigtDenKlartext() {
        assertThat(GameId.of("401872656")).hasToString("401872656");
    }
}
