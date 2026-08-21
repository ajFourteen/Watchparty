package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class ReportMailTokenTest {

    @Test
    void generateErzeugtJedesMalEinenAnderenToken() {
        assertThat(ReportMailToken.generate()).isNotEqualTo(ReportMailToken.generate());
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> ReportMailToken.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofUebernimmtDenWert() {
        assertThat(ReportMailToken.of("abc").value()).isEqualTo("abc");
    }

    @Test
    void toStringZeigtDenKlartext() {
        assertThat(ReportMailToken.of("abc")).hasToString("abc");
    }
}
