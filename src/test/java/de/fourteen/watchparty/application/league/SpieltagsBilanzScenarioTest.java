package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.SpieltagsBilanzStufen;

import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Eigene Spieltags-Bilanz (Kapitel 13.9, Feature 006 Schnitt 1) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2) — das
 * Zusammenspiel aus {@link PredictionService} und
 * {@link de.fourteen.watchparty.application.league.view.ReportView}.
 */
@PortTest
class SpieltagsBilanzScenarioTest extends DeutschesSzenario<SpieltagsBilanzStufen, SpieltagsBilanzStufen, SpieltagsBilanzStufen> {

    @Test
    @Anforderung({ "13.9-a", "13.9-c" })
    void dieBilanzZeigtEndergebnisEigenenTippUndPunkteJeSpiel() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17)
                .und().dasSpielEndetMit("1", 27, 20);

        wenn().ruftDieBilanzAbFuer("anna@example.org");

        // Tendenz (Heimsieg) getroffen, Abstand 3 vs. 7 -- beides im 1-Score-Bucket (1-8): 5 Punkte.
        dann().zeigtFuerDasSpielDasEndergebnis("1", 27, 20)
                .und().zeigtFuerDasSpielDenEigenenTipp("1", 24, 17)
                .und().zeigtFuerDasSpielDiePunkte("1", 5);
    }

    @Test
    @Anforderung("13.9-c")
    void einGewertetesSpielOhneEigenenTippZaehltMitNullPunkten() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().dasSpielEndetMit("1", 27, 20);

        wenn().ruftDieBilanzAbFuer("anna@example.org");

        dann().zeigtFuerDasSpielKeinenEigenenTipp("1")
                .und().zeigtFuerDasSpielDiePunkte("1", 0);
    }

    @Test
    @Anforderung("13.9-b")
    void einNochNichtGewertetesSpielFehltInDerBilanz() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org");

        wenn().ruftDieBilanzAbFuer("anna@example.org");

        dann().enthaeltDasSpielNicht("1");
    }

    @Test
    @Anforderung("13.9-d")
    void dieBilanzTraegtDieSpieltagssumme() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einSpielMitAnstossIn("2", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17)
                .und().tipptFuerDasSpiel("anna@example.org", "2", 30, 10)
                .und().dasSpielEndetMit("1", 24, 17)
                .und().dasSpielEndetMit("2", 20, 10);

        wenn().ruftDieBilanzAbFuer("anna@example.org");

        // Spiel 1 exakt (6), Spiel 2 nur die Tendenz (3): 9 insgesamt.
        dann().zeigtDieSpieltagssumme(9);
    }

    @Test
    @Anforderung("13.9-e")
    void dieBilanzZeigtDenEigenenTippNichtDenFremden() {
        angenommen().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().einKontoMitNamenExistiertFuer("Ben", "ben@example.org")
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17)
                .und().tipptFuerDasSpiel("ben@example.org", "1", 3, 30)
                .und().dasSpielEndetMit("1", 24, 17);

        wenn().ruftDieBilanzAbFuer("anna@example.org");

        // Waere Bens Tipp durchgerutscht, stuende hier sein 3:30 statt Annas exaktem Treffer.
        dann().zeigtFuerDasSpielDenEigenenTipp("1", 24, 17)
                .und().zeigtFuerDasSpielDiePunkte("1", 6);
    }
}
