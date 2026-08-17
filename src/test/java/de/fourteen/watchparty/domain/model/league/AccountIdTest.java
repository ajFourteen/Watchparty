package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class AccountIdTest {

    @Test
    void newIdErzeugtJedesMalEineAndereId() {
        assertThat(AccountId.newId()).isNotEqualTo(AccountId.newId());
    }

    @Test
    void ofUndNewIdSindGleichBeiGleichemWert() {
        UUID value = UUID.randomUUID();
        assertThat(AccountId.of(value)).isEqualTo(AccountId.of(value));
    }

    @Test
    void toStringZeigtDenKlartext() {
        UUID value = UUID.randomUUID();
        assertThat(AccountId.of(value)).hasToString(value.toString());
    }
}
