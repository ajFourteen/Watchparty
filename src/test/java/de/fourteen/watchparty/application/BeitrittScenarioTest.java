package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.BeitrittStufen;

import org.junit.jupiter.api.Test;

/**
 * Beitritt (Anforderung 1/3) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-e, 3-a, 3-c und 3.1-c.
 */
@PortTest
class BeitrittScenarioTest extends DeutschesSzenario<BeitrittStufen, BeitrittStufen, BeitrittStufen> {

    @Test
    @Anforderung({ "1-e", "3-a", "3-c" })
    void beitrittBrauchtNurEinenNamenUndBringtDasFestgelegteStartguthaben() {
        wenn().einSpielerTrittNurMitEinemNamenBei("Anna");

        dann().hatDasFestgelegteStartguthaben("Anna");
    }

    /**
     * Die Parameter aus 3.1 gelten bis zum Probelauf als vorlaeufig
     * (docs/offene-entscheidungen.md). Deshalb darf der Client sie nicht
     * selbst kennen -- er bekommt sie beim Beitritt gesagt (Feature 002).
     */
    @Test
    @Anforderung({ "3.1-c" })
    void derBeitrittNenntDieDreiParameter() {
        wenn().einSpielerTrittNurMitEinemNamenBei("Anna");

        dann().kenntDieDreiParameterAusDemWelcome("Anna");
    }

    @Test
    @Anforderung({ "1-g" })
    void werOhneCodeBeitrittErzeugtEineWatchpartyUndIstIhrHost() {
        wenn().einSpielerTrittNurMitEinemNamenBei("Anna");

        dann().istHostDerNeuenWatchparty("Anna");
    }

    @Test
    @Anforderung({ "1-g" })
    void werDenCodeKenntKommtInDieselbeWatchparty() {
        wenn().einSpielerTrittNurMitEinemNamenBei("Anna")
                .und().einSpielerTrittNurMitEinemNamenBei("Ben");

        dann().istInDerselbenWatchpartyWie("Anna", "Ben");
    }

    @Test
    @Anforderung({ "1-h", "1-i" })
    void einUnbekannterCodeIstEinFehlerKeinNeuerRaum() {
        wenn().einSpielerTrittNurMitEinemNamenBei("Anna")
                .und().trittMitUnbekanntemCodeBei("Ben", "ZZZZ");

        dann().bekommtEinenFehlerWeilDerCodeUnbekanntIst("Ben");
    }

    @Test
    @Anforderung({ "1-h" })
    void grossUndKleinschreibungDesCodesIstGleichgueltig() {
        wenn().einSpielerTrittNurMitEinemNamenBei("Anna")
                .und().trittMitDemCodeVonInKleinschreibungBei("Ben", "Anna");

        dann().istInDerselbenWatchpartyWie("Anna", "Ben");
    }
}
