package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.WettmechanikStufen;

import org.junit.jupiter.api.Test;

/**
 * Wettmechanik (Anforderung 6) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 6-a, 6-c, 6-e und 6-f.
 */
@PortTest
class WettmechanikScenarioTest
        extends DeutschesSzenario<WettmechanikStufen, WettmechanikStufen, WettmechanikStufen> {

    @Test
    @Anforderung({ "6-a", "6-c" })
    void tippOhneEinsatzSetztDenMindesteinsatzUndEinZweiterVersuchWirdAbgelehnt() {
        angenommen()
                .einHostUndBenSindImRaum()
                .und().derHostOeffnetEineWette();

        wenn().derHostTipptOhneEinsatzanzugeben();
        dann().derEigeneTippDesHostsZeigtEinsatz(25);

        wenn().derHostVersuchtErneutZuTippenMitAnderemAusgangUndEinsatz();
        dann()
                .derZweiteTippversuchWirdAbgelehnt()
                .und().nachDemSchliessenBleibtDerErsteTippGueltig("touchdown", 25);
    }

    @Test
    @Anforderung("6-e")
    void derEinsatzWirdAufDenEigenenKontostandGedeckelt() {
        angenommen()
                .einHostUndBenSindImRaum()
                .und().derHostOeffnetEineWette();

        wenn().benTipptMitEinemEinsatzWeitUeberDemEigenenKontostand();

        dann().bensEinsatzIstAufDenEigenenKontostandGedeckelt(1000);
    }

    @Test
    @Anforderung("6-f")
    void werWenigerAlsDenMindesteinsatzHatGehtZwangsweiseAllIn() {
        angenommen()
                .einHostUndBenSindImRaum()
                .und().derHostOeffnetEineWette()
                .und().derHostGewinntUndBenVerliertSeinenGesamtenKontostandInEinerRunde();

        wenn()
                .derHostOeffnetEineWette()
                .und().benTipptOhneEinsatzanzugebenMitNullPunkten();

        dann().bensZwangsweiserAllInEinsatzIst(0);
    }

    @Test
    @Anforderung("4-b")
    void derHostWaehltEineWetteAusDemServereigenenKatalogAus() {
        angenommen().einHostUndBenSindImRaum();

        wenn().derHostWaehltAusDemKatalogDieWette("field-goal-attempt");

        dann().dieOffeneRundeIstDieGewaehlteWette("field-goal-attempt");
    }
}
