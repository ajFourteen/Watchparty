package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.ReconnectStufen;

import org.junit.jupiter.api.Test;

/**
 * Reconnect gezielt in jeder Phase (ADR-014) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2). Das Handy mitten im offenen
 * Fenster zu sperren ist der Realfall, nicht die Ausnahme -- deshalb jede
 * Phase einzeln, wie im bisherigen {@code ReconnectTest}, jetzt aber ueber
 * die Ports statt {@code getRoomForTest()} geprueft.
 */
@PortTest
class ReconnectScenarioTest extends DeutschesSzenario<ReconnectStufen, ReconnectStufen, ReconnectStufen> {

    @Test
    void reconnectWaehrendIdleGibtDasselbeKontoZurueck() {
        angenommen()
                .hostUndAnnaSindImRaum()
                .und().annaTrenntSich();

        wenn().annaVerbindetSichWiederUndBehaeltDasselbeKonto();
    }

    @Test
    void reconnectWaehrendOpenKannNochTippenWennNochNichtGetippt() {
        angenommen()
                .hostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTrenntSich();

        wenn().annaVerbindetSichWiederUndBehaeltDasselbeKonto();

        dann().annaKannJetztNochTippen();
    }

    @Test
    void reconnectWaehrendOpenErhaeltDenEigenenBereitsAbgegebenenTippErneut() {
        angenommen()
                .hostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTipptPuntMitEinsatz(40)
                .und().annaTrenntSich();

        wenn().annaVerbindetSichWiederUndBehaeltDasselbeKonto();

        dann().annaBekommtIhrenBereitsAbgegebenenTippErneutAlsYourPick("punt", 40);
    }

    @Test
    void reconnectWaehrendClosedLaesstDieAufgedecktenTippsUnveraendert() {
        angenommen()
                .hostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTipptPuntMitEinsatz(50)
                .und().derHostTipptTouchdownMitEinsatzUndSchliesstDasFenster(100);

        wenn()
                .annaTrenntSich()
                .und().annaVerbindetSichWiederUndBehaeltDasselbeKonto();

        dann().dieAufgedecktenTippsSindWeiterhinVollstaendigSichtbar(2);
    }

    @Test
    void reconnectWaehrendResolvedZeigtWeiterhinDasErgebnis() {
        angenommen()
                .hostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTipptPuntMitEinsatz(50)
                .und().derHostTipptTouchdownMitEinsatzUndSchliesstDasFenster(100)
                .und().derHostLoestZugunstenVonTouchdownAuf()
                .und().annaTrenntSich();

        wenn().annaVerbindetSichWiederUndBehaeltDasselbeKonto();

        dann().dasErgebnisIstWeiterhinSichtbar("touchdown");
    }

    @Test
    @Anforderung("8.1-d")
    void reconnectSetztDenVerpassteRundenZaehlerZurueck() {
        angenommen()
                .hostUndAnnaSindImRaum()
                .und().annaTrenntSich()
                .und().annaVerpasstEineRundeGetrenntUndIstNochNichtPausiert();

        wenn()
                .annaVerbindetSichWiederUndBehaeltDasselbeKonto()
                .und().annaTrenntSich()
                .und().annaVerpasstEineRundeGetrenntUndIstNochNichtPausiert();

        dann().annaIstNachDemReconnectNochImmerNichtPausiert();
    }
}
