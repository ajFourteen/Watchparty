package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.ScheduleFeed;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.Matchday;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakeScheduleFeed implements ScheduleFeed {

    private final Map<Matchday, List<Game>> antworten = new LinkedHashMap<>();
    private final Set<Matchday> ausgefallen = new HashSet<>();

    public void antworteMit(Matchday matchday, Game... games) {
        antworten.put(matchday, List.of(games));
    }

    public void falleAusFuer(Matchday matchday) {
        ausgefallen.add(matchday);
    }

    @Override
    public List<Game> fetchMatchday(Matchday matchday) {
        if (ausgefallen.contains(matchday)) {
            throw new RuntimeException("Feed nicht erreichbar (Test)");
        }
        return antworten.getOrDefault(matchday, List.of());
    }
}
