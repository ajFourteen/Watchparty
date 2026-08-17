package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class DisplayNameTest {

    @Test
    void einNameBisZwanzigZeichenIstGueltig() {
        assertThat(DisplayName.of("Anna").value()).isEqualTo("Anna");
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> DisplayName.of("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DisplayName.of("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ueberZwanzigZeichenIstUngueltig() {
        assertThatThrownBy(() -> DisplayName.of("a".repeat(21))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void genauZwanzigZeichenIstGueltig() {
        String name = "a".repeat(20);
        assertThat(DisplayName.of(name).value()).isEqualTo(name);
    }

    @Test
    void leerraumWirdGetrimmt() {
        assertThat(DisplayName.of("  Anna  ").value()).isEqualTo("Anna");
    }

    @Test
    void isValidLiefertFalseFuerNull() {
        assertThat(DisplayName.isValid(null)).isFalse();
    }
}
