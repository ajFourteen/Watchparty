package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.LigaStufen;

import org.junit.jupiter.api.Test;

/**
 * Ligen und Rangliste (Kapitel 13.6) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2).
 */
@PortTest
class LigenScenarioTest extends DeutschesSzenario<LigaStufen, LigaStufen, LigaStufen> {

    @Test
    @Anforderung({ "13.6-a", "13.6-b", "13.6-e" })
    void anlegenBeitretenUndDieRanglisteZeigtBeideMitglieder() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().einKontoMitNamenExistiertFuer("Ben", "ben@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("A", "Büro-Liga", 2026, "anna@example.org")
                .und().trittDerLigaBei("ben@example.org", "A")
                .und().einSpielIstAmSpieltagBeendetMit("1", 1, 24, 17)
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17)
                .und().tipptFuerDasSpiel("ben@example.org", "1", 20, 10);

        wenn().ruftDieSaisonRanglisteDerLigaAb("A");

        dann().zeigtInDerRanglisteGenauDieseKonten("anna@example.org", "ben@example.org")
                .und().zeigtFuerDasKontoWertungspunkte("anna@example.org", 6)
                .und().zeigtFuerDasKontoWertungspunkte("ben@example.org", 3);
    }

    @Test
    @Anforderung("13.6-c")
    void einTippZaehltInAllenLigenDesTippersGleichzeitig() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("A", "Liga A", 2026, "anna@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("B", "Liga B", 2026, "anna@example.org")
                .und().einSpielIstAmSpieltagBeendetMit("1", 1, 24, 17)
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        wenn().ruftDieSaisonRanglisteDerLigaAb("A");
        dann().zeigtFuerDasKontoWertungspunkte("anna@example.org", 6);

        wenn().ruftDieSaisonRanglisteDerLigaAb("B");
        dann().zeigtFuerDasKontoWertungspunkte("anna@example.org", 6);
    }

    @Test
    @Anforderung("13.6-d")
    void nachDemVerlassenZaehltDerTippNochInDenUebrigenLigen() {
        angenommen().einKontoMitNamenExistiertFuer("Ben", "ben@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("A", "Liga A", 2026, "ben@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("B", "Liga B", 2026, "ben@example.org")
                .und().einSpielIstAmSpieltagBeendetMit("1", 1, 24, 17)
                .und().tipptFuerDasSpiel("ben@example.org", "1", 24, 17);

        wenn().verlaesstDieLiga("ben@example.org", "A");

        dann().istIstNochMitgliedDerLiga("ben@example.org", "B");
        wenn().ruftDieSaisonRanglisteDerLigaAb("A");
        dann().zeigtInDerRanglisteGenauDieseKonten();
        wenn().ruftDieSaisonRanglisteDerLigaAb("B");
        dann().zeigtFuerDasKontoWertungspunkte("ben@example.org", 6);
    }

    @Test
    @Anforderung("13.6-h")
    void dieSpieltagsRanglisteZaehltNurDenGefragtenSpieltag() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("A", "Liga A", 2026, "anna@example.org")
                .und().einSpielIstAmSpieltagBeendetMit("1", 1, 24, 17)
                .und().einSpielIstAmSpieltagBeendetMit("2", 2, 24, 17)
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17)
                .und().tipptFuerDasSpiel("anna@example.org", "2", 24, 17);

        wenn().ruftDieSpieltagsRanglisteDerLigaAb("A", 1);

        dann().zeigtFuerDasKontoWertungspunkte("anna@example.org", 6);
    }

    @Test
    @Anforderung("13.6-i")
    void ranglistenZweierLigenSindVollstaendigGetrennt() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().einKontoMitNamenExistiertFuer("Ben", "ben@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("A", "Liga A", 2026, "anna@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("B", "Liga B", 2026, "ben@example.org")
                .und().einSpielIstAmSpieltagBeendetMit("1", 1, 24, 17)
                .und().tipptFuerDasSpiel("ben@example.org", "1", 24, 17);

        wenn().ruftDieSaisonRanglisteDerLigaAb("A");

        dann().zeigtInDerRanglisteGenauDieseKonten("anna@example.org");
    }

    @Test
    @Anforderung("13.6-j")
    void eineErgebniskorrekturRechnetDieRanglisteNeu() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().legtEineLigaAnFuerDieSaisonAls("A", "Liga A", 2026, "anna@example.org")
                .und().einSpielIstAmSpieltagBeendetMit("1", 1, 24, 17)
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        wenn().korrigiertDasErgebnisDesSpielsAuf("1", 24, 21)
                .und().ruftDieSaisonRanglisteDerLigaAb("A");

        dann().zeigtFuerDasKontoWertungspunkte("anna@example.org", 5);
    }
}
