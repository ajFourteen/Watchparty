package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.RollenStufen;

import org.junit.jupiter.api.Test;

/**
 * Rollen und Host-Uebergabe (Anforderung 10) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2).
 */
@PortTest
class RollenScenarioTest extends DeutschesSzenario<RollenStufen, RollenStufen, RollenStufen> {

    @Test
    @Anforderung({ "10-a", "10.1", "10.1-c" })
    void derFruehesteBeigetreteneVerbundeneSpielerIstHostUndSteuertAlleFuenfKnoepfe() {
        // Weder "Anna" noch die Beitrittsreihenfolge braucht einen eigenen
        // Einstiegsschritt fuer die Host-Rolle (10.1-c) -- derselbe join()
        // wie fuer jeden anderen Spieler auch.
        angenommen().dreiSpielerTretenBeiInDieserReihenfolge("Anna", "Ben", "Carla");

        dann().istHost("Anna");

        wenn().derHostSteuertEinenVollstaendigenRundenablaufUndSetztDanachZurueck("Anna");

        dann().keinDieserBefehleWurdeVomHostAbgelehnt("Anna");
    }

    @Test
    @Anforderung({ "10.1-a", "10.1-b" })
    void hostRolleWandertSofortBeiVerbindungsverlustUndKehrtErstNachDemAufloesenZurueck() {
        angenommen().dreiSpielerTretenBeiInDieserReihenfolge("Anna", "Ben", "Carla");

        wenn().derHostVerliertDieVerbindungWaehrendDasFensterOffenIst("Anna");
        dann().wirdSofortHost("Ben");

        wenn().derFruehereHostKehrtWaehrendDesOffenenFenstersZurueck("Anna");
        dann().dieHostRolleBleibtVorerstBeim("Ben");

        wenn().derAmtierendeHostSchliesstUndLoestZugunstenVonTouchdownAuf("Ben");
        dann().derFruehereHostBekommtDieRolleZurueck("Anna");
    }
}
