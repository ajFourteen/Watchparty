package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.Matchday;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakeGameRepository implements GameRepository {

    private final Map<String, Game> byId = new LinkedHashMap<>();

    @Override
    public void save(Game game) {
        byId.put(game.getId().value(), game);
    }

    @Override
    public Optional<Game> findById(GameId id) {
        return Optional.ofNullable(byId.get(id.value()));
    }

    @Override
    public List<Game> findByMatchday(Matchday matchday) {
        return byId.values().stream().filter(g -> g.getMatchday().equals(matchday)).toList();
    }
}
