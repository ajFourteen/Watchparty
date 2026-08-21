package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.application.league.view.PredictionView;
import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.LeaguePoints;
import de.fourteen.watchparty.domain.model.league.Matchday;

/** Was von aussen ausgeloest werden kann, um Spieltage abzurufen und zu tippen (Kapitel 13.4). */
public interface PredictionCommands {

    /** Kriterium 15/19/20. Fremde Tipps sind vor dem Anstoss des jeweiligen Spiels nicht Teil der Antwort. */
    PredictionView.MatchdayView viewMatchday(EmailAddress requester, Matchday matchday);

    /** Kriterium 15/16: Ein Ergebnistipp ist bis zum Anstoss abgeb- und aenderbar, danach nicht mehr. */
    void submitPrediction(EmailAddress account, GameId gameId, GameScore score);

    /**
     * Die Summe der Wertungspunkte eines Kontos ueber alle bewerteten Spiele
     * hinweg, unabhaengig von einer Liga (13.6-c: Tipps und Punkte gehoeren
     * dem Konto, nicht der Liga — nur die Rangliste ist liga-spezifisch).
     */
    LeaguePoints totalPoints(EmailAddress account);

    /**
     * Die eigene Bilanz eines Spieltags (13.9, Feature 006 Schnitt 1): je
     * gewertetem Spiel Endergebnis, eigener Ergebnistipp und erreichte
     * Wertungspunkte, dazu die Spieltagssumme. Ausschliesslich das eigene
     * Konto — kein fremder Tipp ist Teil der Antwort (13.9-e).
     */
    ReportView.MatchdayReportView matchdayReport(EmailAddress account, Matchday matchday);
}
