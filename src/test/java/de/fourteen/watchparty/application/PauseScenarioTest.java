package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.PauseStufen;

import org.junit.jupiter.api.Test;

/**
 * Die Verpasste-Runden-Pause (Anforderung 8.1) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 8.1, 8.1-d und 8.1-e.
 */
@PortTest
class PauseScenarioTest extends DeutschesSzenario<PauseStufen, PauseStufen, PauseStufen> {

    @Test
    @Anforderung({ "8.1", "8.1-d" })
    void getrennterSpielerZahltDieErstenZweiVerpasstenRundenUndPausiertAbDerDritten() {
        angenommen()
                .einHostUndSpielerSindImRaum("Anna")
                .und().derSpielerTrenntSich("Anna");

        wenn()
                .derHostOeffnetEineWette()
                .und().derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf();
        dann()
                .hatInsgesamtPunkteVerlorenUndIstPausiert("Anna", 25, false)
                .und().invariantenGeltenWeiterhin();

        wenn()
                .derHostOeffnetEineWette()
                .und().derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf();
        dann()
                .hatInsgesamtPunkteVerlorenUndIstPausiert("Anna", 50, true)
                .und().invariantenGeltenWeiterhin();

        wenn().derHostOeffnetEineWette();
        dann().derTeilnehmerkreisUmfasstGenauSpieler(1);

        wenn().derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf();
        dann()
                .hatInsgesamtPunkteVerlorenUndIstPausiert("Anna", 50, true)
                .und().invariantenGeltenWeiterhin();
    }

    @Test
    @Anforderung("8.1-e")
    void verbundenerSpielerZahltJedeRundeOhneJemalsZuPausieren() {
        angenommen().einHostUndSpielerSindImRaum("Ben");

        for (int runde = 1; runde <= 3; runde++) {
            wenn()
                    .derHostOeffnetEineWette()
                    .und().derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf();
            dann()
                    .hatInsgesamtPunkteVerlorenUndIstPausiert("Ben", runde * 25, false)
                    .und().invariantenGeltenWeiterhin();
        }
    }
}
