package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.NichtTipperStufen;

import org.junit.jupiter.api.Test;

/**
 * Wer nicht getippt hat, ab dem Schliessen sichtbar (Anforderung 8.1-f,
 * Feature 002) — auf der Port-to-Port-Ebene (docs/teststrategie.md,
 * Abschnitt 2.2).
 *
 * Der Client kann diese Menge nicht selbst bilden: Der Teilnehmerkreis ist
 * beim Oeffnen eingefroren (8.1-b) und ein pausierter Spieler faellt heraus
 * (8.1-d) — beides Regeln, die nach Invariante 3 auf dem Server bleiben.
 */
@PortTest
class NichtTipperScenarioTest
        extends DeutschesSzenario<NichtTipperStufen, NichtTipperStufen, NichtTipperStufen> {

    @Test
    @Anforderung({ "8.1-f", "9-b" })
    void abDemSchliessenNenntDerZustandWerNichtGetipptHat() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTippt();

        wenn().derHostSchliesstDasFenster();

        dann()
                .giltAlsTeilnehmerOhneTipp("Host")
                .und().giltNichtAlsTeilnehmerOhneTipp("Anna")
                .und().invariantenGeltenWeiterhin();
    }

    @Test
    @Anforderung({ "8.1-f", "6-b" })
    void solangeDasFensterOffenIstVerraetDerZustandDieNichtTipperNicht() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette();

        wenn().annaTippt();

        dann()
                .verraetDerZustandDieNichtTipperNicht()
                .und().invariantenGeltenWeiterhin();
    }

    @Test
    @Anforderung({ "8.1-f", "8.1-b" })
    void werErstNachDemOeffnenBeitrittStehtNichtInDerListe() {
        angenommen()
                .einHostUndBenSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTrittJetztErstBei();

        wenn().derHostTippt().und().derHostSchliesstDasFenster();

        dann()
                .giltAlsTeilnehmerOhneTipp("Ben")
                .und().giltNichtAlsTeilnehmerOhneTipp("Anna")
                .und().invariantenGeltenWeiterhin();
    }

    /**
     * Nach dem Aufloesen bleibt die Liste stehen: Die Oberflaeche zeigt im
     * Ergebnis denselben Teilnehmerkreis wie in der Aufdeckung, nur mit dem
     * tatsaechlichen Strafbetrag aus den Deltas statt mit der Ankuendigung.
     */
    @Test
    @Anforderung({ "8.1-f", "8.1-a" })
    void auchNachDemAufloesenBleibtSichtbarWerNichtGetipptHat() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTippt()
                .und().derHostSchliesstDasFenster();

        wenn().derHostLoestAuf();

        dann()
                .giltAlsTeilnehmerOhneTipp("Host")
                .und().giltNichtAlsTeilnehmerOhneTipp("Anna")
                .und().invariantenGeltenWeiterhin();
    }
}
