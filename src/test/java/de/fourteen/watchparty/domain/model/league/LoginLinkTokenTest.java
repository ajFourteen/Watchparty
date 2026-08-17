package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class LoginLinkTokenTest {

    @Test
    void generateErzeugtJedesMalEinenAnderenToken() {
        assertThat(LoginLinkToken.generate()).isNotEqualTo(LoginLinkToken.generate());
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> LoginLinkToken.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofUebernimmtDenWert() {
        assertThat(LoginLinkToken.of("abc").value()).isEqualTo("abc");
    }

    @Test
    void toStringZeigtDenKlartext() {
        assertThat(LoginLinkToken.of("abc")).hasToString("abc");
    }
}
