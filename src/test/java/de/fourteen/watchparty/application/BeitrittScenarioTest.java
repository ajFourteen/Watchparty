package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.BeitrittStufen;

import org.junit.jupiter.api.Test;

/**
 * Beitritt (Anforderung 1/3) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-e, 3-a und 3-c.
 */
@PortTest
class BeitrittScenarioTest extends DeutschesSzenario<BeitrittStufen, BeitrittStufen, BeitrittStufen> {

    @Test
    @Anforderung({ "1-e", "3-a", "3-c" })
    void beitrittBrauchtNurEinenNamenUndBringtDasFestgelegteStartguthaben() {
        wenn().einSpielerTrittNurMitEinemNamenBei("Anna");

        dann().hatDasFestgelegteStartguthaben("Anna");
    }
}
