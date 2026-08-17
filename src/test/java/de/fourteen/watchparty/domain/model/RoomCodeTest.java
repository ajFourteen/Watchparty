package de.fourteen.watchparty.domain.model;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Form und Normalisierung des Raum-Codes (Anforderung 1-h), direkt geprueft
 * — ohne Raum, ohne Actor.
 */
@UnitTest
class RoomCodeTest {

    @Test
    @Anforderung("1-h")
    void vierZeichenAusDemAlphabetSindGueltig() {
        assertThat(RoomCode.of("AB3D").value()).isEqualTo("AB3D");
    }

    @Test
    @Anforderung("1-h")
    void einFalscheLaengeIstUngueltig() {
        assertThatThrownBy(() -> RoomCode.of("AB3")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoomCode.of("AB3DE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Anforderung("1-h")
    void oIUndLSindAlsErzeugteZeichenAusgeschlossen() {
        assertThatThrownBy(() -> RoomCode.of("ABOD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoomCode.of("ABID")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoomCode.of("ABLD")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Anforderung("1-h")
    void parseIstUnabhaengigVonGrossKleinschreibung() {
        assertThat(RoomCode.parse("ab3d")).isEqualTo(RoomCode.of("AB3D"));
    }

    @Test
    @Anforderung("1-h")
    void parseFaltetVerwechselbareZeichen() {
        assertThat(RoomCode.parse("ABOD")).isEqualTo(RoomCode.of("AB0D"));
        assertThat(RoomCode.parse("ABID")).isEqualTo(RoomCode.of("AB1D"));
        assertThat(RoomCode.parse("ABLD")).isEqualTo(RoomCode.of("AB1D"));
    }

    @Test
    @Anforderung("1-h")
    void parseTrimmtLeerraum() {
        assertThat(RoomCode.parse("  ab3d  ")).isEqualTo(RoomCode.of("AB3D"));
    }

    @Test
    @Anforderung("1-h")
    void parseLiefertNullFuerLeereEingabe() {
        assertThat(RoomCode.parse(null)).isNull();
        assertThat(RoomCode.parse("")).isNull();
        assertThat(RoomCode.parse("   ")).isNull();
    }

    @Test
    @Anforderung("1-h")
    void parseLiefertNullFuerUngueltigeForm() {
        assertThat(RoomCode.parse("AB3")).isNull();
        assertThat(RoomCode.parse("AB3D5")).isNull();
        assertThat(RoomCode.parse("AB3!")).isNull();
    }

    @Test
    @Anforderung("1-h")
    void randomLiefertImmerEineGueltigeForm() {
        for (int i = 0; i < 200; i++) {
            RoomCode code = RoomCode.random();
            assertThat(code.value()).hasSize(4);
            assertThat(code.value()).doesNotContain("O", "I", "L");
        }
    }

    @Test
    @Anforderung("1-h")
    void toStringZeigtDenKlartext() {
        assertThat(RoomCode.of("AB3D")).hasToString("AB3D");
    }
}
