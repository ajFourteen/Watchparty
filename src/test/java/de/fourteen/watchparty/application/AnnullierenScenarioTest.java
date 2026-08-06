package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.AnnullierenStufen;

import org.junit.jupiter.api.Test;

/**
 * Annullieren (Anforderung 8.6) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 8.6, 8.6-a und 8.6-b.
 */
@PortTest
class AnnullierenScenarioTest extends DeutschesSzenario<AnnullierenStufen, AnnullierenStufen, AnnullierenStufen> {

    @Test
    @Anforderung({ "8.6", "8.6-a" })
    void annullierenLaesstKontenUndDenVerpassteRundenZaehlerUnberuehrt() {
        angenommen().einHostUndBenSindImRaumBenIstGetrennt();

        // Dreimal annulliert, damit ein faelschlich mitgezaehlter
        // Verpasste-Runden-Zaehler laengst die Pause (ab der zweiten
        // verpassten Runde) ausgeloest haette.
        for (int i = 0; i < 3; i++) {
            wenn().derHostOeffnetEineWetteTipptUndAnnulliert();
            dann()
                    .alleKontenSindUnveraendert()
                    .und().benIstAuchNachMehrerenAnnulliertenRundenNichtPausiert()
                    .und().invariantenGeltenWeiterhin();
        }
    }

    @Test
    @Anforderung("8.6-b")
    void nachDemAufloesenIstAnnullierenNichtMehrMoeglich() {
        angenommen()
                .einHostUndBenSindImRaumBenIstGetrennt()
                .und().derHostOeffnetEineWette();

        wenn().derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf();
        dann().dieRundeBleibtAufgeloestUndNichtAnnulliert();

        wenn().derHostVersuchtDanachZuAnnullieren();
        dann()
                .dieRundeBleibtAufgeloestUndNichtAnnulliert()
                .und().derHostBekommtEinenFehlerBeimVersuchNachDemAufloesenZuAnnullieren();
    }
}
