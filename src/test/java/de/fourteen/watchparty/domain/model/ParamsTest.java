package de.fourteen.watchparty.domain.model;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Anforderung 3.1: Startguthaben 1000, Mindesteinsatz 25, Nicht-Tipper-Strafe
 * 25 -- alle drei an einer Stelle im Code (3.1-a). Bis zu diesem Test stand
 * das Startguthaben separat als {@code Room.STARTING_POINTS}
 * (docs/offene-entscheidungen.md); {@link Params} ist seither die einzige
 * Quelle fuer alle drei Werte.
 */
@UnitTest
class ParamsTest {

    @Test
    @Anforderung({ "3.1", "3.1-a" })
    void startguthabenMindesteinsatzUndStrafeStehenAlleDreiInParams() {
        assertThat(Params.DEFAULT.startingPoints()).isEqualTo(Points.of(1000));
        assertThat(Params.DEFAULT.minStake()).isEqualTo(Points.of(25));
        assertThat(Params.DEFAULT.penalty()).isEqualTo(Points.of(25));
    }
}
