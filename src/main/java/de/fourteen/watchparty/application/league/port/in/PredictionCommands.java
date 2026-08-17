package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.application.league.view.PredictionView;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Matchday;

/** Was von aussen ausgeloest werden kann, um Spieltage abzurufen und zu tippen (Kapitel 13.4). */
public interface PredictionCommands {

    /** Kriterium 15/19/20. Fremde Tipps sind vor dem Anstoss des jeweiligen Spiels nicht Teil der Antwort. */
    PredictionView.MatchdayView viewMatchday(EmailAddress requester, Matchday matchday);

    /** Kriterium 15/16: Ein Ergebnistipp ist bis zum Anstoss abgeb- und aenderbar, danach nicht mehr. */
    void submitPrediction(EmailAddress account, GameId gameId, GameScore score);
}
