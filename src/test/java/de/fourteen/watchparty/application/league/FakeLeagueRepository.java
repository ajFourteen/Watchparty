package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.LeagueRepository;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakeLeagueRepository implements LeagueRepository {

    private final Map<LeagueId, League> byId = new LinkedHashMap<>();

    @Override
    public void save(League league) {
        byId.put(league.getId(), league);
    }

    @Override
    public Optional<League> findById(LeagueId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<League> findByCode(LeagueCode code) {
        return byId.values().stream().filter(l -> l.getCode().equals(code)).findFirst();
    }

    @Override
    public List<League> findByMember(EmailAddress account) {
        return byId.values().stream().filter(l -> l.isMember(account)).toList();
    }
}
