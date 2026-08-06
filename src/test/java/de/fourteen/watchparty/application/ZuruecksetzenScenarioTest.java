package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.ZuruecksetzenStufen;

import org.junit.jupiter.api.Test;

/**
 * Zuruecksetzen (Anforderung 8.7) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 8.7, 8.7-a und -- weil nur
 * der Host es darf -- nebenbei 10-b.
 */
@PortTest
class ZuruecksetzenScenarioTest
        extends DeutschesSzenario<ZuruecksetzenStufen, ZuruecksetzenStufen, ZuruecksetzenStufen> {

    @Test
    @Anforderung({ "8.7", "8.7-a", "10-b" })
    void derHostSetztDenRaumMittenInEinerOffenenRundeZurueck() {
        angenommen().einHostUndAnnaSpielenGeradeEineOffeneRunde();

        wenn().einSpielerOhneHostRolleVersuchtZurueckzusetzen();
        dann().dieRundeLaeuftUnveraendertWeiter();

        wenn().derHostSetztDenRaumZurueck();
        dann().derRaumIstLeerOhneSpielerOhneHostUndOhneLaufendeRunde();
    }
}
