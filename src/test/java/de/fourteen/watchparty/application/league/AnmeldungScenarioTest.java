package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.LoginStufen;

import org.junit.jupiter.api.Test;

/**
 * Anmeldung ueber Magic Link (ADR-036, Kapitel 13.2) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2) — Kritikalitaet
 * HIGH (Feature-Dokument: "wer fremde Links erraet oder eine fremde Sitzung
 * bekommt, tippt unter fremdem Namen").
 */
@PortTest
class AnmeldungScenarioTest extends DeutschesSzenario<LoginStufen, LoginStufen, LoginStufen> {

    @Test
    @Anforderung({ "13.2-a", "13.2-b" })
    void neuesKontoEntstehtErstBeimEinloesenMitDemMitgeschicktenNamen() {
        angenommen().keinKontoExistiertFuer("anna@example.org");

        wenn().fordertMitNamenEinenLinkAnFuer("Anna", "anna@example.org")
                .und().loestDenLetztenLinkFuerEin("anna@example.org");

        dann().istSieDamitAngemeldet()
                .und().existiertEinKontoMitDemNamenFuer("Anna", "anna@example.org");
    }

    @Test
    @Anforderung("13.2-b")
    void beiBestehendemKontoBleibtDerNameBeimAnmeldenUnveraendert() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org");

        wenn().fordertMitNamenEinenLinkAnFuer("Anna Zwei", "anna@example.org")
                .und().loestDenLetztenLinkFuerEin("anna@example.org");

        dann().istSieDamitAngemeldet()
                .und().existiertEinKontoMitDemNamenFuer("Anna", "anna@example.org");
    }

    @Test
    @Anforderung("13.2-c")
    void einVerbrauchterAnmeldelinkMeldetNiemandenAn() {
        angenommen().fordertMitNamenEinenLinkAnFuer("Anna", "anna@example.org")
                .und().loestDenLetztenLinkFuerEin("anna@example.org")
                .und().istSieDamitAngemeldet();

        wenn().loestDenselbenLinkNochEinmalEinFuer("anna@example.org");

        dann().istSieDamitNichtAngemeldet();
    }

    @Test
    @Anforderung("13.2-d")
    void dieAnmeldeantwortVerraetNichtWerEinKontoHat() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().keinKontoExistiertFuer("niemand@example.org");

        wenn().fordertFuerBeideAdressenEinenLinkAn("anna@example.org", "niemand@example.org");

        dann().bekommenBeideAdressenGenauEineNachricht("anna@example.org", "niemand@example.org");
    }

    @Test
    @Anforderung("13.2-e")
    void dasRateLimitVerhindertDenVersand() {
        angenommen().dasRateLimitFuerDieAdresseGreiftBereits("anna@example.org");

        wenn().fordertMitNamenEinenLinkAnFuer("Anna", "anna@example.org");

        dann().wurdeKeineNachrichtVersendet();
    }

    @Test
    @Anforderung("13.2-f")
    void eineSitzungHaeltNeunzigTage() {
        angenommen().fordertMitNamenEinenLinkAnFuer("Anna", "anna@example.org")
                .und().loestDenLetztenLinkFuerEin("anna@example.org");

        wenn().vergehenNeunzigTageMinusEineSekunde();
        dann().istIhreSitzungNochGueltig();

        wenn().vergehtNochEineSekunde();
        dann().istIhreSitzungAbgelaufen();
    }

    @Test
    @Anforderung("13.2-h")
    void kontoLoeschenEntferntEsUndSeineSitzungen() {
        angenommen().fordertMitNamenEinenLinkAnFuer("Anna", "anna@example.org")
                .und().loestDenLetztenLinkFuerEin("anna@example.org");

        wenn().wirdIhrKontoGeloescht("anna@example.org");

        dann().existiertKeinKontoMehrFuer("anna@example.org")
                .und().istIhreBisherigeSitzungNichtMehrHinterlegt();
    }
}
