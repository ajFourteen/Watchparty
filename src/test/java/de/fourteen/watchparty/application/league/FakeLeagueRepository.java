package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.LeagueRepository;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.Membership;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handgeschriebenes Test Double (ADR-025, kein Mockito).
 *
 * Gibt bei jeder Abfrage eine eigene {@link League}-Instanz heraus statt der
 * gespeicherten. Das ist kein Detail: Solange der Fake dieselbe Instanz
 * zurueckreichte, wirkte jede Aenderung am gelesenen Objekt sofort im
 * "Speicher", und ein Lese-Aendere-Schreibe-Zyklus konnte gar nichts
 * verlieren. Genau deshalb blieb der Beitrittsverlust aus 13.6-b auf der
 * Port-Ebene unsichtbar und fiel erst am echten Postgres auf. Ein Test
 * Double darf nachsichtiger sein als die Wirklichkeit, wo es um Technik
 * geht — nicht dort, wo es die gepruefte Zusage aushebelt.
 */
public class FakeLeagueRepository implements LeagueRepository {

    private final Map<LeagueId, League> byId = new LinkedHashMap<>();

    @Override
    public void save(League league) {
        byId.put(league.getId(), kopie(league));
    }

    @Override
    public Optional<League> findById(LeagueId id) {
        return Optional.ofNullable(byId.get(id)).map(FakeLeagueRepository::kopie);
    }

    @Override
    public Optional<League> findByCode(LeagueCode code) {
        return byId.values().stream()
                .filter(l -> l.getCode().equals(code))
                .findFirst()
                .map(FakeLeagueRepository::kopie);
    }

    @Override
    public List<League> findByMember(EmailAddress account) {
        return byId.values().stream()
                .filter(l -> l.isMember(account))
                .map(FakeLeagueRepository::kopie)
                .toList();
    }

    @Override
    public void addMember(LeagueId league, Membership member) {
        League gespeichert = byId.get(league);
        if (gespeichert == null) {
            return;
        }
        gespeichert.join(member.getAccountEmail(), member.getJoinedAt());
    }

    @Override
    public void removeMember(LeagueId league, EmailAddress account) {
        League gespeichert = byId.get(league);
        if (gespeichert == null) {
            return;
        }
        gespeichert.leave(account);
    }

    private static League kopie(League league) {
        return League.of(league.getId(), league.getSeason(), league.getCode(), league.getName(),
                league.getManagerEmail(), new ArrayList<>(league.getMembers()));
    }
}
