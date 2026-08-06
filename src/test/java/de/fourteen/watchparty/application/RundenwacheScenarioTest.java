package de.fourteen.watchparty.application;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.RundenwacheStufen;

import org.junit.jupiter.api.Test;

/**
 * Die Rundenwache aus ADR-010 in der Sprache der Fachabteilung
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 5-a bis 5-d: Das Fenster
 * schliesst bei Ablauf der 15 Sekunden oder beim Host-Klick -- je nachdem,
 * was zuerst eintritt -- und ein danach feuernder, veralteter Timer
 * verrechnet nichts doppelt und schliesst keine bereits wieder offene neue
 * Runde.
 */
@PortTest
class RundenwacheScenarioTest extends DeutschesSzenario<RundenwacheStufen, RundenwacheStufen, RundenwacheStufen> {

    @Test
    @Anforderung({ "5-a", "5-b" })
    void derHostOeffnetEineWetteDieNachFuenfzehnSekundenAutomatischSchliesst() {
        angenommen().derHostIstImRaum();

        wenn()
                .derHostOeffnetEineWette()
                .und().fuenfzehnSekundenVergehenUndDerAutoCloseTimerFeuert();

        dann().dasFensterIstAutomatischGeschlossen();
    }

    @Test
    @Anforderung({ "5-c", "5-d" })
    void derHostSchliesstVonHandEinDanachFeuernderAutoCloseTimerVerrechnetNichtDoppelt() {
        angenommen()
                .derHostIstImRaum()
                .und().derHostOeffnetEineWette();

        wenn()
                .derHostSchliesstVonHand()
                .und().derVeralteteAutoCloseTimerFeuertTrotzdem();

        dann().dieRundeBleibtEinfachGeschlossen();
    }

    @Test
    @Anforderung("5-d")
    void einVeralteterAutoCloseTimerEinerVorherigenRundeSchliesstDieNeueBereitsOffeneRundeNicht() {
        angenommen().derHostIstImRaum();

        wenn()
                .derHostOeffnetEineWette()
                // Der Auto-Close-Task der ersten Runde bleibt im FakeScheduler
                // eingereiht, obwohl die Runde laengst manuell aufgeloest ist --
                // das Cancel beim Oeffnen der zweiten Runde ist laut ADR-010
                // keine hinreichende Absicherung, nur die Runden-ID-Wache ist es.
                .und().derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf()
                .und().derHostOeffnetEineWette()
                .und().derVeralteteAutoCloseTimerFeuertTrotzdem();

        dann().dieNeueRundeBleibtOffen();
    }
}
