package de.fourteen.watchparty.domain.service.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.UnitTest;
import de.fourteen.watchparty.teststrategy.stufen.WertungStufen;

import org.junit.jupiter.api.Test;

/**
 * Die Wertung als reine Funktion (13.5), Kritikalitaet HIGH (ADR-038).
 * Szenarien aus {@code docs/features/005-tippspiel-liga.md} uebernommen.
 */
@UnitTest
class ScoringScenarioTest extends DeutschesSzenario<WertungStufen, WertungStufen, WertungStufen> {

    @Test
    @Anforderung("13.5-a")
    void hoechsteStufeZaehltNichtDieSumme() {
        angenommen().einSpielEndeteMit(24, 17);

        wenn()
                .tipptMitErgebnis("Anna", 24, 17)
                .und().tipptMitErgebnis("Ben", 27, 20)
                .und().tipptMitErgebnis("Cem", 31, 10)
                .und().tipptMitErgebnis("Dana", 17, 24);

        dann()
                .hatWertungspunkte("Anna", 6)
                .und().hatWertungspunkte("Ben", 5)
                .und().hatWertungspunkte("Cem", 3)
                .und().hatWertungspunkte("Dana", 0);
    }

    @Test
    @Anforderung("13.5-b")
    void falscheTendenzSchlaegtJedenAbstand() {
        angenommen().einSpielEndeteMit(24, 17);

        wenn().tipptMitErgebnis("Anna", 17, 24);

        dann().hatWertungspunkte("Anna", 0);
    }

    /**
     * Abstand 8 (Touchdown plus Two-Point) liegt noch im selben Eimer wie
     * das Endergebnis, Abstand 9 nicht mehr — die Grenze aus 13.5-c.
     */
    @Test
    @Anforderung("13.5-c")
    void dieGrenzenDerAbstandsEimer() {
        angenommen().einSpielEndeteMit(28, 20);

        wenn()
                .tipptMitErgebnis("Anna", 21, 13)
                .und().tipptMitErgebnis("Ben", 30, 21);

        dann()
                .hatWertungspunkte("Anna", 5)
                .und().hatWertungspunkte("Ben", 3);
    }

    @Test
    @Anforderung("13.5-c")
    void unentschiedenIstEinEigenerEimer() {
        angenommen().einSpielEndeteMit(20, 20);

        wenn()
                .tipptMitErgebnis("Anna", 20, 20)
                .und().tipptMitErgebnis("Ben", 17, 17)
                .und().tipptMitErgebnis("Cem", 21, 20);

        dann()
                .hatWertungspunkte("Anna", 6)
                .und().hatWertungspunkte("Ben", 5)
                .und().hatWertungspunkte("Cem", 0);
    }
}
