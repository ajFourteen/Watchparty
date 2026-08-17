package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.LeagueName;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.service.league.Standings;

import java.util.List;

/** Was von aussen ausgeloest werden kann, um Ligen zu verwalten und ihre Rangliste abzurufen (Kapitel 13.6). */
public interface LeagueCommands {

    /** Kriterium 28: der anlegende Tipper ist Verwalter und zugleich erstes Mitglied. */
    LeagueId createLeague(EmailAddress manager, LeagueName name, SeasonId season);

    /** Kriterium 29: wer den Code hat, tritt bei. */
    void joinLeague(EmailAddress account, LeagueCode code);

    /** Kriterium 34: die eigenen Ergebnistipps bleiben unberuehrt und zaehlen in den uebrigen Ligen weiter. */
    void leaveLeague(EmailAddress account, LeagueId leagueId);

    /** Kriterium 31/32/35: Rangliste ueber die ganze Saison, nur die Mitglieder dieser Liga. */
    List<Standings.Entry> seasonStandings(LeagueId leagueId);

    /** Kriterium 33: Rangliste nur ueber einen einzelnen Spieltag. */
    List<Standings.Entry> matchdayStandings(LeagueId leagueId, Matchday matchday);
}
