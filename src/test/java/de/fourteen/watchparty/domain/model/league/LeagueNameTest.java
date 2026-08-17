package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class LeagueNameTest {

    @Test
    void einNameBisVierzigZeichenIstGueltig() {
        assertThat(LeagueName.of("Büro-Liga").value()).isEqualTo("Büro-Liga");
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> LeagueName.of("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LeagueName.of("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ueberVierzigZeichenIstUngueltig() {
        assertThatThrownBy(() -> LeagueName.of("a".repeat(41))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void genauVierzigZeichenIstGueltig() {
        String name = "a".repeat(40);
        assertThat(LeagueName.of(name).value()).isEqualTo(name);
    }

    @Test
    void leerraumWirdGetrimmt() {
        assertThat(LeagueName.of("  Büro-Liga  ").value()).isEqualTo("Büro-Liga");
    }

    @Test
    void isValidLiefertFalseFuerNull() {
        assertThat(LeagueName.isValid(null)).isFalse();
    }
}
