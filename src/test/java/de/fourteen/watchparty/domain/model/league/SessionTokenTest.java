package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class SessionTokenTest {

    @Test
    void generateErzeugtJedesMalEinenAnderenToken() {
        assertThat(SessionToken.generate()).isNotEqualTo(SessionToken.generate());
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> SessionToken.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofUebernimmtDenWert() {
        assertThat(SessionToken.of("abc").value()).isEqualTo("abc");
    }

    @Test
    void toStringZeigtDenKlartext() {
        assertThat(SessionToken.of("abc")).hasToString("abc");
    }
}
