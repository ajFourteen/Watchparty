package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.TippenStufen;

import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Tippen (Kapitel 13.4) auf der Port-to-Port-Ebene (docs/teststrategie.md,
 * Abschnitt 2.2) — das Zusammenspiel aus {@link PredictionService} und
 * {@link de.fourteen.watchparty.application.league.view.PredictionView}.
 * Die Sichtbarkeitsregel selbst (jeder Zweig, Mutation Score ≥ 99 %) steht
 * in {@code PredictionViewTest}; hier zaehlt das Zusammenspiel ueber echte
 * Repository-Aufrufe.
 */
@PortTest
class TippenScenarioTest extends DeutschesSzenario<TippenStufen, TippenStufen, TippenStufen> {

    @Test
    @Anforderung({ "13.4-b", "13.4-c" })
    void einTippVorDemAnstossWirdAngenommenUndIstSichtbar() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org");

        wenn().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        dann().istDerTippAngenommenWorden();
        wenn().ruftDenSpieltagAbAls("anna@example.org");
        dann().zeigtFuerDasSpielDenEigenenTipp("1", 24, 17);
    }

    @Test
    @Anforderung("13.4-c")
    void einTippAendertSichBisZumAnstossAufDenNeuestenStand() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        wenn().tipptFuerDasSpiel("anna@example.org", "1", 30, 20);

        dann().istDerTippAngenommenWorden();
        wenn().ruftDenSpieltagAbAls("anna@example.org");
        dann().zeigtFuerDasSpielDenEigenenTipp("1", 30, 20);
    }

    @Test
    @Anforderung("13.4-c")
    void nachDemAnstossWirdWederGeaendertNochNachgetragen() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().vergehtDieZeitUm(Duration.ofHours(1));

        wenn().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        dann().istDerTippAbgelehntWorden();
    }

    @Test
    @Anforderung("13.4-d")
    void fremdeTippsBleibenVorDemAnstossImZusammenspielVerdeckt() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().einKontoMitNamenExistiertFuer("Ben", "ben@example.org")
                .und().tipptFuerDasSpiel("ben@example.org", "1", 20, 10);

        wenn().ruftDenSpieltagAbAls("anna@example.org");

        dann().zeigtFuerDasSpielKeineFremdenTipps("1");
    }

    @Test
    @Anforderung("13.4-d")
    void fremdeTippsWerdenNachDemAnstossImZusammenspielSichtbar() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().einKontoMitNamenExistiertFuer("Ben", "ben@example.org")
                .und().tipptFuerDasSpiel("ben@example.org", "1", 20, 10)
                .und().vergehtDieZeitUm(Duration.ofHours(1));

        wenn().ruftDenSpieltagAbAls("anna@example.org");

        dann().zeigtFuerDasSpielDenFremdenTippVon("1", "Ben", 20, 10);
    }

    @Test
    @Anforderung("13.6-k")
    void derPunktestandIstDieSummeDerWertungspunkteUeberAlleBewertetenSpiele() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einSpielMitAnstossIn("2", Duration.ofHours(1))
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17)
                .und().tipptFuerDasSpiel("anna@example.org", "2", 30, 10)
                .und().dasSpielEndetMit("1", 24, 17)
                .und().dasSpielEndetMit("2", 20, 10);

        wenn().ruftDenPunktestandAbFuer("anna@example.org");

        // Spiel 1 exakt getroffen (6), Spiel 2 nur die Tendenz (3): 9 insgesamt.
        dann().zeigtDenPunktestand(9);
    }

    @Test
    @Anforderung("13.6-k")
    void nochNichtAusgewerteteSpieleZaehlenNichtZumPunktestand() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        wenn().ruftDenPunktestandAbFuer("anna@example.org");

        dann().zeigtDenPunktestand(0);
    }
}
