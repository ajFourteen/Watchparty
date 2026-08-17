package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.PortTest;
import de.fourteen.watchparty.teststrategy.stufen.ScheduleStufen;

import org.junit.jupiter.api.Test;

/**
 * Spielplan-Abgleich (ADR-037, Kapitel 13.3) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2).
 */
@PortTest
class ScheduleScenarioTest extends DeutschesSzenario<ScheduleStufen, ScheduleStufen, ScheduleStufen> {

    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);

    @Test
    @Anforderung("13.3-a")
    void einNeuesSpielVomFeedWirdUebernommen() {
        angenommen().derFeedMeldetFuerEinGeplantesSpiel("1", MATCHDAY);

        wenn().wirdDerSpieltagAbgeglichen(MATCHDAY);

        dann().kenntDasSpielMitDemStatus("1", GameStatus.SCHEDULED);
    }

    @Test
    @Anforderung("13.3-e")
    void eineErgebniskorrekturDesFeedsWirdUebernommen() {
        angenommen().istBereitsAlsBeendetGespeichertMit("1", MATCHDAY, 24, 17);

        wenn().derFeedMeldetFuerDasSpielDasKorrigierteErgebnis("1", MATCHDAY, 24, 21)
                .und().wirdDerSpieltagAbgeglichen(MATCHDAY);

        dann().kenntFuerDasSpielDasErgebnis("1", 24, 21);
    }

    @Test
    @Anforderung("13.3-d")
    void faelltDerFeedAusBleibtDerLetzteBekannteStandStehen() {
        angenommen().istBereitsAlsBeendetGespeichertMit("1", MATCHDAY, 24, 17)
                .und().derFeedFaelltAusFuer(MATCHDAY);

        wenn().wirdDerSpieltagAbgeglichen(MATCHDAY);

        dann().kenntDasSpielMitDemStatus("1", GameStatus.FINAL)
                .und().kenntFuerDasSpielDasErgebnis("1", 24, 17);
    }

    @Test
    @Anforderung("13.3-f")
    void einVomFeedAlsAbgesagtGemeldetesSpielTraegtDiesenStatus() {
        angenommen().derFeedMeldetDasSpielAlsAbgesagt("1", MATCHDAY);

        wenn().wirdDerSpieltagAbgeglichen(MATCHDAY);

        dann().kenntDasSpielMitDemStatus("1", GameStatus.CANCELLED);
    }

    @Test
    @Anforderung("13.3-g")
    void einHandeintragUeberschreibtDenFeedUndBleibtAuchNachEinemErneutenAbgleich() {
        angenommen().istBereitsAlsBeendetGespeichertMit("1", MATCHDAY, 24, 17);

        wenn().wirdEinErgebnisVonHandGesetzt("1", 30, 20)
                .und().derFeedMeldetFuerDasSpielDasKorrigierteErgebnis("1", MATCHDAY, 24, 21)
                .und().wirdDerSpieltagAbgeglichen(MATCHDAY);

        dann().kenntFuerDasSpielDasErgebnis("1", 30, 20);
    }
}
