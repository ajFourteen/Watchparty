package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class ClientIpTest {

    @Test
    void eineAdresseWirdUebernommen() {
        assertThat(ClientIp.of("203.0.113.1").value()).isEqualTo("203.0.113.1");
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> ClientIp.of("")).isInstanceOf(IllegalArgumentException.class);
    }
}
