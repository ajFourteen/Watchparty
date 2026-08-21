package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.ReportMailStufen;

import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Mailversand des Spieltags-Reports (Kapitel 13.9, Feature 010 Schnitt 5,
 * ADR-041) auf der Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt
 * 2.2) -- das Zusammenspiel aus {@link ScheduleSyncService}, {@link
 * ReportMailService} und {@link PredictionService}.
 */
@PortTest
class ReportMailScenarioTest extends DeutschesSzenario<ReportMailStufen, ReportMailStufen, ReportMailStufen> {

    @Test
    @Anforderung("13.9-n")
    void einTipperBestelltDenMailversand() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org");

        wenn().hatDenMailversandBestellt("anna@example.org");

        dann().dasOptInIstAktivFuer("anna@example.org");
    }

    @Test
    @Anforderung("13.9-n")
    void einTipperBestelltDenMailversandWiederAb() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().hatDenMailversandBestellt("anna@example.org");

        wenn().hatDenMailversandAbbestellt("anna@example.org");

        dann().dasOptInIstInaktivFuer("anna@example.org");
    }

    @Test
    @Anforderung({ "13.9-n", "13.2-b" })
    void einNeuesKontoHatDenMailversandNichtBestellt() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org");

        dann().dasOptInIstInaktivFuer("anna@example.org");
    }

    @Test
    @Anforderung({ "13.9-o", "13.9-n" })
    void ohneOptInKommtKeineMail() {
        angenommen().einKontoMitNamenExistiertFuer("Ben", "ben@example.org")
                .und().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().tipptFuerDasSpiel("ben@example.org", "1", 24, 17);

        wenn().derFeedMeldetFuerDasSpielDasEndergebnis("1", 27, 20);

        dann().bekommtKeineReportMail("ben@example.org");
    }

    @Test
    @Anforderung({ "13.9-o", "13.9-a" })
    void derFeedAbgleichLoestDenVersandAus() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().hatDenMailversandBestellt("anna@example.org")
                .und().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        wenn().derFeedMeldetFuerDasSpielDasEndergebnis("1", 27, 20);

        // Tendenz (Heimsieg) getroffen, Abstand 3 vs. 7 -- beide im 1-Score-Bucket (1-8): 5 Punkte.
        dann().bekommtGenauEineReportMail("anna@example.org")
                .und().dieReportMailZeigtDieSpieltagssumme("anna@example.org", 5);
    }

    @Test
    @Anforderung("13.9-o")
    void derHandeintragLoestDenVersandEbensoAus() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().hatDenMailversandBestellt("anna@example.org")
                .und().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17);

        wenn().wirdFuerDasSpielEinErgebnisVonHandGesetzt("1", 24, 17);

        dann().bekommtGenauEineReportMail("anna@example.org");
    }

    @Test
    @Anforderung({ "13.9-o", "13.3-f" })
    void einAbgesagtesSpielBlockiertDenVersandNicht() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().hatDenMailversandBestellt("anna@example.org")
                .und().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().einSpielMitAnstossIn("2", Duration.ofHours(1))
                .und().tipptFuerDasSpiel("anna@example.org", "2", 24, 17)
                .und().derFeedMeldetDasSpielAlsAbgesagt("1");

        wenn().derFeedMeldetFuerDasSpielDasEndergebnis("2", 24, 17);

        dann().bekommtGenauEineReportMail("anna@example.org");
    }

    @Test
    @Anforderung("13.9-o")
    void einBereitsAusgewerteterSpieltagVersendetKeinZweitesMal() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().hatDenMailversandBestellt("anna@example.org")
                .und().einSpielMitAnstossIn("1", Duration.ofHours(1))
                .und().tipptFuerDasSpiel("anna@example.org", "1", 24, 17)
                .und().derFeedMeldetFuerDasSpielDasEndergebnis("1", 24, 17);

        wenn().derFeedMeldetFuerDasSpielDasEndergebnis("1", 24, 17);

        dann().bekommtGenauEineReportMail("anna@example.org");
    }

    @Test
    @Anforderung("13.9-p")
    void derAbmeldelinkWirktOhneAnmeldung() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().hatDenMailversandBestellt("anna@example.org");

        wenn().wirdDerAbmeldelinkVonAufgerufen("anna@example.org");

        dann().dasOptInIstInaktivFuer("anna@example.org");
    }

    @Test
    @Anforderung("13.9-p")
    void einUnbekannterAbmeldelinkTokenVerraetNichts() {
        angenommen().einKontoMitNamenExistiertFuer("Anna", "anna@example.org")
                .und().hatDenMailversandBestellt("anna@example.org");

        wenn().wirdEinUnbekannterAbmeldelinkAufgerufen();

        // Kein Fehler, keine Ausnahme -- und Annas eigenes Opt-in bleibt unberuehrt.
        dann().dasOptInIstAktivFuer("anna@example.org");
    }
}
