package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class TeamTest {

    @Test
    void kuerzelUndNameWerdenUebernommen() {
        Team team = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
        assertThat(team.id()).isEqualTo(TeamId.of("KC"));
        assertThat(team.name()).isEqualTo("Kansas City Chiefs");
    }

    @Test
    void leererNameIstUngueltig() {
        assertThatThrownBy(() -> Team.of(TeamId.of("KC"), "")).isInstanceOf(IllegalArgumentException.class);
    }
}
