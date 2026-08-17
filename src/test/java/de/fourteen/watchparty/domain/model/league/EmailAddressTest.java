package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class EmailAddressTest {

    @Test
    void eineGueltigeAdresseWirdUebernommen() {
        assertThat(EmailAddress.of("anna@example.org").value()).isEqualTo("anna@example.org");
    }

    @Test
    void wirdAufKleinschreibungNormalisiert() {
        assertThat(EmailAddress.of("Anna@Example.ORG")).isEqualTo(EmailAddress.of("anna@example.org"));
    }

    @Test
    void leerraumWirdGetrimmt() {
        assertThat(EmailAddress.of("  anna@example.org  ").value()).isEqualTo("anna@example.org");
    }

    @Test
    void ohneKlammeraffeIstUngueltig() {
        assertThatThrownBy(() -> EmailAddress.of("anna.example.org")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ohnePunktImDomainteilIstUngueltig() {
        assertThatThrownBy(() -> EmailAddress.of("anna@example")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void leerIstUngueltig() {
        assertThatThrownBy(() -> EmailAddress.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isValidLiefertFalseFuerNull() {
        assertThat(EmailAddress.isValid(null)).isFalse();
    }
}
