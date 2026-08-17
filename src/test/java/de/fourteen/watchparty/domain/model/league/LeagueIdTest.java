package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class LeagueIdTest {

    @Test
    void newIdErzeugtJedesMalEineAndereId() {
        assertThat(LeagueId.newId()).isNotEqualTo(LeagueId.newId());
    }

    @Test
    void ofUndNewIdSindGleichBeiGleichemWert() {
        UUID value = UUID.randomUUID();
        assertThat(LeagueId.of(value)).isEqualTo(LeagueId.of(value));
    }

    @Test
    void toStringZeigtDenKlartext() {
        UUID value = UUID.randomUUID();
        assertThat(LeagueId.of(value)).hasToString(value.toString());
    }
}
