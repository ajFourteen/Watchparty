package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.TeilnehmerkreisStufen;

import org.junit.jupiter.api.Test;

/**
 * Pilotszenario der Port-to-Port-Ebene (Phase 1 der Teststrategie-Umsetzung),
 * belegt 8.1-b: Der Teilnehmerkreis wird beim Oeffnen eingefroren, wer danach
 * beitritt, zahlt keine Strafe fuer eine Runde, an der er nicht teilnehmen
 * konnte. Nebenbei belegt es 8.1-a: Ben, im eingefrorenen Kreis, zahlt.
 */
@PortTest
class TeilnehmerkreisScenarioTest
        extends DeutschesSzenario<TeilnehmerkreisStufen, TeilnehmerkreisStufen, TeilnehmerkreisStufen> {

    @Test
    @Anforderung({ "8.1-a", "8.1-b" })
    void werNachOeffnenBeitrittZahltKeineStrafeFuerDieseRunde() {
        angenommen()
                .einHostUndBenSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTrittJetztErstBei();

        wenn().derHostTipptTouchdownSchliesstUndLoestAuf();

        dann()
                .zahltEineStrafe("Ben")
                .und().zahltKeineStrafeWeilErstNachOeffnenBeigetreten("Anna")
                .und().invariantenGeltenWeiterhin();
    }
}
