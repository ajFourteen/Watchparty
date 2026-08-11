package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.VollstaendigeBeteiligungStufen;

import org.junit.jupiter.api.Test;

/**
 * Anforderung 5-g auf der Port-to-Port-Ebene (docs/teststrategie.md,
 * Abschnitt 2.2), Feature 003: Das Fenster schliesst sofort, sobald alle
 * Teilnehmer des eingefrorenen Kreises getippt haben.
 *
 * Der Kreis ist derselbe wie bei der Strafe (8.1-b/8.1-d) -- deshalb halten
 * ein Nachzuegler und ein pausierter Spieler die Runde nicht auf, ein
 * getrennter Spieler ohne Pause dagegen schon.
 */
@PortTest
class VollstaendigeBeteiligungScenarioTest
        extends DeutschesSzenario<VollstaendigeBeteiligungStufen, VollstaendigeBeteiligungStufen,
                VollstaendigeBeteiligungStufen> {

    @Test
    @Anforderung("5-g")
    void habenAlleGetipptSchliesstDasFensterVonSelbst() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette();

        wenn()
                .annaTipptTouchdown()
                .und().derHostTipptPunt();

        dann()
                .dasFensterIstGeschlossen()
                .und().wederDieZeitNochDerHostHabenGeschlossen()
                .und().keinZustandZeigteEinOffenesFensterMitVollzaehligenTipps()
                .und().beideTippsLiegenOffen();
    }

    @Test
    @Anforderung("5-g")
    void solangeEinerFehltBleibtDasFensterOffen() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette();

        wenn().annaTipptTouchdown();

        dann()
                .dasFensterIstNochOffen()
                .und().esFehltNochEinTipp();
    }

    @Test
    @Anforderung({ "5-g", "8.1-b" })
    void werZuSpaetKommtHaeltDieRundeNichtAuf() {
        angenommen()
                .derHostIstImRaum()
                .und().derHostOeffnetEineWette()
                .und().annaTrittJetztErstBei();

        wenn().derHostTipptTouchdown();

        dann().dasFensterIstGeschlossen();
    }

    @Test
    @Anforderung({ "5-g", "8.1-d" })
    void einPausierterSpielerHaeltDieRundeNichtAuf() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().annaTrenntSich()
                .und().annaVerpasstZweiRundenUndPausiert();

        wenn()
                .derHostOeffnetEineWette()
                .und().derHostTipptTouchdown();

        dann().dasFensterIstGeschlossen();
    }

    @Test
    @Anforderung({ "5-g", "5-b" })
    void einGetrennterSpielerOhnePauseHaeltDieRundeAufBisDieZeitAblaeuft() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().annaTrenntSich()
                .und().derHostOeffnetEineWette();

        wenn().derHostTipptTouchdown();
        dann().dasFensterIstNochOffen();

        wenn().fuenfzehnSekundenVergehenUndDerAutoCloseTimerFeuert();
        dann().dasFensterIstGeschlossen();
    }

    @Test
    @Anforderung({ "5-g", "9-c" })
    void derTippDerDasFensterSchliesstZaehltWieJederAndere() {
        angenommen()
                .einHostUndAnnaSindImRaum()
                .und().derHostOeffnetEineWette();

        wenn()
                .annaTipptTouchdown()
                .und().derHostTipptPunt()
                .und().derHostLoestZugunstenVonTouchdownAuf();

        dann()
                .hatDenGanzenPoolGewonnen("Anna")
                .und().invariantenGeltenWeiterhin();
    }
}
