package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.WatchpartyTrennungStufen;

import org.junit.jupiter.api.Test;

/**
 * Trennung zweier Watchpartys (ADR-033, Anforderung 1-i) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2) -- der Kern von
 * Feature 004, Kritikalitaet HIGH: Keine Nachricht und kein Kommando einer
 * Watchparty darf auf eine andere wirken.
 */
@PortTest
class WatchpartyTrennungScenarioTest
        extends DeutschesSzenario<WatchpartyTrennungStufen, WatchpartyTrennungStufen, WatchpartyTrennungStufen> {

    @Test
    @Anforderung("1-i")
    void zweiWatchpartysRechnenGetrenntAb() {
        angenommen()
                .istHostVonWatchpartyA("Anna").und().trittWatchpartyABei("Ben")
                .und().istHostVonWatchpartyB("Charlie").und().trittWatchpartyBBei("Dana");

        wenn()
                .oeffnetEineWette("Anna")
                .und().tippt("Anna", "touchdown")
                .und().tippt("Ben", "punt")
                .und().schliesstUndLoestZugunstenVonAuf("Anna", "touchdown");

        dann()
                .habenSichDiePunktekontenGeaendertFuer("Anna", "Ben")
                .und().sindUnveraendertBeiStartguthaben("Charlie", "Dana")
                .und().istWeiterhinInPhase("Charlie", "IDLE");
    }

    @Test
    @Anforderung("1-i")
    void keinZustandVerlaesstSeineWatchparty() {
        angenommen()
                .istHostVonWatchpartyA("Anna")
                .und().istHostVonWatchpartyB("Ben")
                .und().merktSichDenNachrichtenstandVon("Ben");

        wenn()
                .oeffnetEineWette("Anna")
                .und().tippt("Anna", "touchdown");

        dann().hatSeitdemKeineEinzigeNachrichtBekommen("Ben");
    }

    @Test
    @Anforderung("1-i")
    void hostIstManInEinemRaumNichtUeberhaupt() {
        angenommen()
                .istHostVonWatchpartyA("Anna")
                .und().istHostVonWatchpartyB("Ben");

        wenn().oeffnetEineWette("Ben");

        dann()
                .istWeiterhinInPhase("Ben", "OPEN")
                .und().istWeiterhinInPhase("Anna", "IDLE");
    }

    @Test
    @Anforderung("1-i")
    void resetRaeumtNurDieEigeneWatchparty() {
        angenommen()
                .istHostVonWatchpartyA("Anna").und().trittWatchpartyABei("Charlie")
                .und().istHostVonWatchpartyB("Ben").und().trittWatchpartyBBei("Dana")
                .und().oeffnetEineWette("Anna")
                .und().tippt("Anna", "touchdown").und().tippt("Charlie", "punt")
                .und().schliesstUndLoestZugunstenVonAuf("Anna", "touchdown")
                .und().oeffnetEineWette("Ben")
                .und().tippt("Ben", "touchdown").und().tippt("Dana", "punt")
                .und().schliesstUndLoestZugunstenVonAuf("Ben", "touchdown");

        wenn().setztDenRaumZurueck("Anna");

        dann()
                .istLeerUndZurueckgesetzt("Anna")
                .und().hatWeiterhinGenauSpieler("Ben", 2)
                .und().istWeiterhinInPhase("Ben", "RESOLVED");
    }

    @Test
    @Anforderung("1-i")
    void einTokenGiltNurInSeinerWatchparty() {
        angenommen()
                .istHostVonWatchpartyA("Anna")
                .und().istHostVonWatchpartyB("Zoe");

        wenn().trittMitDemTokenVonAberInWatchpartyBBei("AnnaInB", "Anna");

        dann()
                .istDortEineNeueSpielerinMitStartguthaben("AnnaInB")
                .und().hatEineAndereSpielerIdAlsInWatchpartyA("AnnaInB", "Anna")
                .und().sindUnveraendertBeiStartguthaben("Anna");
    }

    @Test
    @Anforderung({ "1-i", "8.1" })
    void verpassteRundenZaehlenNurImEigenenRaum() {
        angenommen()
                .istHostVonWatchpartyA("HostA").und().trittWatchpartyABei("Anna").und().trennt("Anna")
                .und().istHostVonWatchpartyB("HostB").und().trittWatchpartyBBei("Ben").und().trennt("Ben");

        wenn()
                .oeffnetEineWette("HostA").und().tipptSchliesstUndLoestZugunstenVonTouchdownAuf("HostA")
                .und().oeffnetEineWette("HostA").und().tipptSchliesstUndLoestZugunstenVonTouchdownAuf("HostA");

        dann()
                .istPausiert("Anna", "HostA")
                .und().hatKeineRundeVerpasst("Ben", "HostB");
    }
}
