package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.Membership;

import java.util.List;
import java.util.Optional;

/** Ausgangs-Port fuer Ligen. Spricht {@link League} — ein Datenmodell der Domaene. */
public interface LeagueRepository {

    void save(League league);

    Optional<League> findById(LeagueId id);

    Optional<League> findByCode(LeagueCode code);

    /** Alle Ligen, denen ein Konto angehoert (Kriterium 30). */
    List<League> findByMember(EmailAddress account);

    /**
     * Nimmt genau ein Mitglied auf — als einzelner, fuer sich unteilbarer
     * Schreibvorgang, nicht als Nebenwirkung von {@link #save(League)}.
     *
     * Der Grund ist ein nachgewiesener Datenverlust: {@code joinLeague}
     * liest die Liga, laesst {@link League#join} die Fachregel pruefen und
     * schreibt zurueck. Zwei Beitritte, die denselben gelesenen Stand
     * vorfinden, ueberschreiben sich dabei gegenseitig — der zuerst
     * geschriebene Beitritt verschwindet spurlos, entgegen 13.6-b. Anders
     * als bei den Live-Wetten schuetzt hier kein Raum-Thread (Invariante 1):
     * Das Tippspiel laeuft auf Request-Threads.
     *
     * Bereits Mitglied zu sein bleibt wirkungslos statt ein Fehler zu sein —
     * dieselbe Zusage wie {@link League#join}.
     */
    void addMember(LeagueId league, Membership member);

    /** Das Gegenstueck zu {@link #addMember}: entfernt genau ein Mitglied (Kriterium 34). */
    void removeMember(LeagueId league, EmailAddress account);
}
