package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.WiederanlaufStufen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Wiederanlauf aus dem Snapshot (ADR-023) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2) -- der Weg Room -> Datei -> neuer
 * RoomActor -> Room, jetzt ueber die Ports statt {@code getRoomForTest()}
 * geprueft. Ergaenzt {@code SnapshotTest} (Datenmodell) und
 * {@code SnapshotStoreTest} (I/O) um den vollstaendigen Weg samt
 * Weiterspielen danach.
 */
@PortTest
class WiederanlaufScenarioTest
        extends DeutschesSzenario<WiederanlaufStufen, WiederanlaufStufen, WiederanlaufStufen> {

    @Test
    void fehlendeDateiStartetLeerenRaum(@TempDir Path verzeichnis) {
        angenommen().dasSnapshotVerzeichnisIst(verzeichnis);

        wenn()
                .derRaumStartet()
                .und().trittBei("Neu");

        dann().istJetztDerEinzigeSpielerUndHost("Neu");
    }

    @Test
    void idleErhaeltPunkteNamenUndTokenUndErlaubtReconnect(@TempDir Path verzeichnis) {
        angenommen()
                .dasSnapshotVerzeichnisIst(verzeichnis)
                .und().derRaumStartet()
                .und().trittBei("Host")
                .und().trittBei("Anna")
                .und().derHostOeffnetEineWette()
                .und().derHostTipptTouchdownMitEinsatz(234)
                .und().derHostSchliesstUndLoestZugunstenVonTouchdownAuf();

        wenn()
                .derServerWirdNeuGestartet()
                .und().trittMitDemAltenTokenWiederBei("Host");

        dann()
                .derRaumEnthaeltGenauSpieler("Host", 2)
                .und().istWiederVerbunden("Host")
                .und().hatPunkte("Host", 1025);
    }

    @Test
    void offeneRundeInDerZukunftPlantAutoCloseNeuUndErlaubtWeiterTippen(@TempDir Path verzeichnis) {
        angenommen()
                .dasSnapshotVerzeichnisIst(verzeichnis)
                .und().derRaumStartet()
                .und().trittBei("Host")
                .und().derHostOeffnetEineWette()
                .und().dieZeitVergeht(5);

        wenn()
                .derServerWirdNeuGestartet()
                .und().trittMitDemAltenTokenWiederBei("Host");

        dann()
                .dasFensterIstFuer("Host", "OPEN")
                .und().einNeuerAutoCloseIstEingeplant()
                .und().kannWeiterhinTippen("Host");

        dann().dasFensterSchliesstZumUrspruenglichVorgesehenenZeitpunkt("Host", 10);
    }

    @Test
    void abgelaufeneOffeneRundeIstBereitsGeschlossen(@TempDir Path verzeichnis) {
        angenommen()
                .dasSnapshotVerzeichnisIst(verzeichnis)
                .und().derRaumStartet()
                .und().trittBei("Host")
                .und().derHostOeffnetEineWette()
                .und().derHostTipptTouchdownMitEinsatz(100)
                .und().dieZeitVergeht(20);

        wenn()
                .derServerWirdNeuGestartet()
                .und().trittMitDemAltenTokenWiederBei("Host");

        dann().dasFensterIstFuer("Host", "CLOSED");
    }

    @Test
    void unbekannteWetteVerwirftNurDieRunde(@TempDir Path verzeichnis) throws Exception {
        angenommen()
                .dasSnapshotVerzeichnisIst(verzeichnis)
                .und().derRaumStartet()
                .und().trittBei("Host")
                .und().derHostOeffnetEineWette();

        wenn()
                .derServerWirdNeuGestartet()
                .und().derWettkatalogEintragDerRundeWirdDurchEineUnbekannteWetteErsetzt();

        wenn()
                .derServerWirdNeuGestartet()
                .und().trittMitDemAltenTokenWiederBei("Host");

        dann()
                .dasFensterIstFuer("Host", "IDLE")
                .und().derRaumEnthaeltGenauSpieler("Host", 1);
    }

    @Test
    @Anforderung("1-c")
    void abgelaufenerSnapshotStartetLeerenRaumTrotzVorhandenerDatei(@TempDir Path verzeichnis) {
        angenommen()
                .dasSnapshotVerzeichnisIst(verzeichnis)
                .und().derRaumStartet()
                .und().trittBei("Host")
                .und().dieZeitVergehtUeberDieVerfallszeitHinaus();

        wenn()
                .derServerWirdNeuGestartet()
                .und().trittBei("Neu");

        dann().istJetztDerEinzigeSpielerUndHost("Neu");
    }

    @Test
    void kaputteDateiStartetLeerenRaumStattDenStartZuZerschiessen(@TempDir Path verzeichnis) throws Exception {
        angenommen()
                .dasSnapshotVerzeichnisIst(verzeichnis)
                .und().derRaumStartet()
                .und().trittBei("Host");

        wenn()
                .dieDateiWirdDurchKaputtesJsonErsetzt()
                .und().derRaumStartet()
                .und().trittBei("Neu");

        dann().istJetztDerEinzigeSpielerUndHost("Neu");
    }

    @Test
    void deaktiviertePersistenzSchreibtNie(@TempDir Path verzeichnis) {
        angenommen().dasSnapshotVerzeichnisIst(verzeichnis);

        wenn()
                .keinePersistenzAktiv()
                .und().trittBei("Host");

        dann().keineDateiWirdGeschrieben();
    }
}
