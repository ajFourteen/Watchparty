package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;

/** Was von aussen ausgeloest werden kann, um Spielplan und Ergebnisse zu pflegen (ADR-037). */
public interface ScheduleCommands {

    /** Gleicht einen Spieltag mit dem Feed ab (Kriterium 9). Ein Ausfall des Feeds laesst den Stand unangetastet (Kriterium 11). */
    void syncMatchday(Matchday matchday);

    /** Gleicht die gesamte Regular Season ab, Spieltag fuer Spieltag — ein einzelner ausgefallener Spieltag haelt die uebrigen nicht auf. */
    void syncSeason(SeasonId season);

    /** Der Notweg aus Kriterium 14: der Betreiber setzt ein Endergebnis von Hand. */
    void setResultManually(GameId gameId, GameScore score);
}
