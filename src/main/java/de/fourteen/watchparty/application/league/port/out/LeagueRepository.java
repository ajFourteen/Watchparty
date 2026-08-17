package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;

import java.util.List;
import java.util.Optional;

/** Ausgangs-Port fuer Ligen. Spricht {@link League} — ein Datenmodell der Domaene. */
public interface LeagueRepository {

    void save(League league);

    Optional<League> findById(LeagueId id);

    Optional<League> findByCode(LeagueCode code);

    /** Alle Ligen, denen ein Konto angehoert (Kriterium 30). */
    List<League> findByMember(EmailAddress account);
}
