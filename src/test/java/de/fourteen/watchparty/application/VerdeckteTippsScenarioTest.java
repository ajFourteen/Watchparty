package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.VerdeckteTippsStufen;

import org.junit.jupiter.api.Test;

/**
 * Invariante 4 (ADR-013) auf der Port-to-Port-Ebene (docs/teststrategie.md,
 * Abschnitt 3.1): Solange das Fenster offen ist, ist nur sichtbar, wie viele
 * getippt haben, nicht was (6-b). Ab dem Schliessen liegen alle Tipps offen,
 * und es kann nicht mehr getippt werden (9-b).
 */
@PortTest
class VerdeckteTippsScenarioTest
        extends DeutschesSzenario<VerdeckteTippsStufen, VerdeckteTippsStufen, VerdeckteTippsStufen> {

    @Test
    @Anforderung({ "6-b", "9-b" })
    void waehrendDesFenstersNurDerZaehlerNachDemSchliessenDerVolleTipp() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette();

        wenn().annaTipptTouchdownMitEinsatz(100);

        dann()
                .keinFrameAnDenHostVerraetWasAnnaGetipptHat()
                .und().derHostHatKeinYourPickFuerAnnasTippErhalten()
                .und().invariantenGeltenWeiterhin();

        wenn().derHostSchliesstDasFenster();

        dann()
                .jetztIstAnnasTippFuerAlleSichtbar(100)
                .und().einWeitererTippversuchVonAnnaWirdAbgelehntWeilDasFensterZuIst()
                .und().invariantenGeltenWeiterhin();
    }
}
