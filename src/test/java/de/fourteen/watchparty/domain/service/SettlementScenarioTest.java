package de.fourteen.watchparty.domain.service;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.UnitTest;
import de.fourteen.watchparty.teststrategy.stufen.AbrechnungStufen;

import org.junit.jupiter.api.Test;

/**
 * Abrechnung als reine Funktion (Anforderung 7/8), Kritikalitaet HIGH
 * (docs/teststrategie.md, Abschnitt 3.1 des Umsetzungsplans). Szenarien
 * zuerst aus {@code anforderungen.md} abgeleitet, dann dem Bestand
 * gegenuebergestellt (Abschnitt 9.2) -- jedes hier deckt sich mit einem
 * gleichwertigen Beispiel in {@code SettlementTest}, jetzt in der Sprache
 * der Anforderungen statt in Klassennamen und Zahlen ohne Kontext.
 */
@UnitTest
class SettlementScenarioTest extends DeutschesSzenario<AbrechnungStufen, AbrechnungStufen, AbrechnungStufen> {

    @Test
    @Anforderung("8.1-c")
    void strafeWirdAufDenKontostandGekappt() {
        angenommen()
                .einTippAufTouchdownMitEinsatz("a", 25)
                .und().einNichtTipperMitKontostand("d", 10);

        wenn().dieRundeWirdMitAusgangTouchdownAbgerechnet();

        dann()
                .zahlt("d", -10)
                .und().zahlt("a", 10)
                .und().dieSummeAllerDeltasIstExaktNull();
    }

    /**
     * 7.1 (Anteil = max(Einsatz, Mindesteinsatz)) und 7.1-a (Einsatz und
     * Anteil sind entkoppelt) in einem Szenario: "a" setzt weniger als den
     * Mindesteinsatz, zaehlt aber wie "b" mit dem Mindestanteil -- die
     * zusaetzlichen Punkte kommen von "c", der verliert, nicht aus dem
     * Nichts.
     */
    @Test
    @Anforderung({ "7.1", "7.1-a", "2-b" })
    void mindestanteilGreiftUndKommtVonDenVerlierernNichtAusDemNichts() {
        angenommen()
                .einTippAufTouchdownMitEinsatz("a", 10)
                .und().einTippAufTouchdownMitEinsatz("b", 25)
                .und().einTippAufPuntMitEinsatz("c", 50);

        wenn().dieRundeWirdMitAusgangTouchdownAbgerechnet();

        dann()
                .zahlt("a", 33)
                .und().zahlt("b", 17)
                .und().zahlt("c", -50)
                .und().dieSummeAllerDeltasIstExaktNull();
    }

    /**
     * 7.2: Auszahlungen sind ganzzahlig, der Rest geht nach dem
     * Groessste-Reste-Verfahren an die Gewinner, die Summe trifft exakt den
     * Pool -- hier 100 (75 Einsaetze + 25 eingesammelte Strafe), aufgeteilt
     * unter drei gleich grossen Anteilen.
     */
    @Test
    @Anforderung({ "7.2", "2-a" })
    void restVerteilungNachGroesstenRestenTrifftExaktDenPool() {
        angenommen()
                .einTippAufTouchdownMitEinsatz("a", 25)
                .und().einTippAufTouchdownMitEinsatz("b", 25)
                .und().einTippAufTouchdownMitEinsatz("c", 25)
                .und().einNichtTipperMitKontostand("d", 1000);

        wenn().dieRundeWirdMitAusgangTouchdownAbgerechnet();

        dann()
                .zahlt("a", 9)
                .und().zahlt("b", 8)
                .und().zahlt("c", 8)
                .und().zahlt("d", -25)
                .und().dieSummeAllerDeltasIstExaktNull();
    }

    /**
     * 8.2 (Push: niemand tippt den Gewinner-Ausgang, alle Einsaetze gehen
     * zurueck) und 8.2-a (die eingesammelten Strafen werden anteilig unter
     * allen Tippern verteilt) in einem Szenario: "a" und "b" tippen beide
     * "punt", gewonnen hat "touchdown" -- ihre Einsaetze kommen zurueck,
     * die Strafe von "d" wird trotzdem anteilig unter ihnen verteilt.
     */
    @Test
    @Anforderung({ "8.2", "8.2-a" })
    void pushGibtEinsaetzeZurueckUndVerteiltStrafenAnteiligAnAlleTipper() {
        angenommen()
                .einTippAufPuntMitEinsatz("a", 50)
                .und().einTippAufPuntMitEinsatz("b", 25)
                .und().einNichtTipperMitKontostand("d", 1000);

        wenn().dieRundeWirdMitAusgangTouchdownAbgerechnet();

        dann()
                .zahlt("a", 17)
                .und().zahlt("b", 8)
                .und().zahlt("d", -25)
                .und().dieSummeAllerDeltasIstExaktNull();
    }

    /**
     * 8.3: Auch mit 0 Punkten darf mitgewettet werden, die Null ist kein
     * absorbierender Zustand -- "a" setzt zwangsweise 0, gewinnt aber die
     * gesamten Punkte von "b" ueber den Mindest-Anteil.
     */
    @Test
    @Anforderung("8.3")
    void spielerMitNullPunktenGehtAllInUndKannUeberDenMindestanteilGewinnen() {
        angenommen()
                .einTippAufTouchdownMitEinsatz("a", 0)
                .und().einTippAufPuntMitEinsatz("b", 100);

        wenn().dieRundeWirdMitAusgangTouchdownAbgerechnet();

        dann()
                .zahlt("a", 100)
                .und().zahlt("b", -100)
                .und().dieSummeAllerDeltasIstExaktNull();
    }

    /**
     * 8.5: Tippen alle denselben richtigen Ausgang, bekommt jeder
     * naeherungsweise seinen Einsatz zurueck -- hier exakt, weil beide
     * Einsaetze gleich hoch sind.
     */
    @Test
    @Anforderung("8.5")
    void alleTippenDenselbenRichtigenAusgangErgibtNettoNull() {
        angenommen()
                .einTippAufTouchdownMitEinsatz("a", 25)
                .und().einTippAufTouchdownMitEinsatz("b", 25);

        wenn().dieRundeWirdMitAusgangTouchdownAbgerechnet();

        dann()
                .bekommtNettoNichtsWeilEinsatzGleichAuszahlungIst("a")
                .und().bekommtNettoNichtsWeilEinsatzGleichAuszahlungIst("b");
    }
}
