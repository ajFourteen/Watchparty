package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.LeagueName;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.service.league.Standings;

import java.util.List;
import java.util.Optional;

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

    /**
     * Kumulierte Saison-Rangliste, die nur Spiele mit Spieltagsnummer &le; der
     * uebergebenen zaehlt (13.6-l) — Grundlage fuer die Platzveraenderung im
     * Report (13.9-i): einmal mit Spieltag N, einmal mit Spieltag N-1 abgefragt.
     */
    List<Standings.Entry> seasonStandingsThroughMatchday(LeagueId leagueId, Matchday matchday);

    /** Kriterium 30: alle Ligen, denen das Konto angehoert. */
    List<League> myLeagues(EmailAddress account);

    /** Nur fuer Mitglieder sichtbar — Nichtmitglied und unbekannte Liga sind absichtlich nicht zu unterscheiden. */
    Optional<League> league(EmailAddress requester, LeagueId leagueId);
}
