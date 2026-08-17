package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class LeagueCodeTest {

    @Test
    @Anforderung("13.6-b")
    void sechsZeichenAusDemAlphabetSindGueltig() {
        assertThat(LeagueCode.of("AB3D5F").value()).isEqualTo("AB3D5F");
    }

    @Test
    void einFalscheLaengeIstUngueltig() {
        assertThatThrownBy(() -> LeagueCode.of("AB3D5")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LeagueCode.of("AB3D5FG")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oIUndLSindAlsErzeugteZeichenAusgeschlossen() {
        assertThatThrownBy(() -> LeagueCode.of("ABOD5F")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LeagueCode.of("ABID5F")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LeagueCode.of("ABLD5F")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseIstUnabhaengigVonGrossKleinschreibung() {
        assertThat(LeagueCode.parse("ab3d5f")).isEqualTo(LeagueCode.of("AB3D5F"));
    }

    @Test
    void parseFaltetVerwechselbareZeichen() {
        assertThat(LeagueCode.parse("ABOD5F")).isEqualTo(LeagueCode.of("AB0D5F"));
        assertThat(LeagueCode.parse("ABID5F")).isEqualTo(LeagueCode.of("AB1D5F"));
        assertThat(LeagueCode.parse("ABLD5F")).isEqualTo(LeagueCode.of("AB1D5F"));
    }

    @Test
    void parseTrimmtLeerraum() {
        assertThat(LeagueCode.parse("  ab3d5f  ")).isEqualTo(LeagueCode.of("AB3D5F"));
    }

    @Test
    void parseLiefertNullFuerLeereEingabe() {
        assertThat(LeagueCode.parse(null)).isNull();
        assertThat(LeagueCode.parse("")).isNull();
        assertThat(LeagueCode.parse("   ")).isNull();
    }

    @Test
    void parseLiefertNullFuerUngueltigeForm() {
        assertThat(LeagueCode.parse("AB3")).isNull();
        assertThat(LeagueCode.parse("AB3D5F7")).isNull();
        assertThat(LeagueCode.parse("AB3D5!")).isNull();
    }

    @Test
    void randomLiefertImmerEineGueltigeForm() {
        for (int i = 0; i < 200; i++) {
            LeagueCode code = LeagueCode.random();
            assertThat(code.value()).hasSize(6);
            assertThat(code.value()).doesNotContain("O", "I", "L");
        }
    }

    @Test
    void toStringZeigtDenKlartext() {
        assertThat(LeagueCode.of("AB3D5F")).hasToString("AB3D5F");
    }
}
