package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.PredictionRepository;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.Prediction;
import de.fourteen.watchparty.domain.model.league.PredictionId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakePredictionRepository implements PredictionRepository {

    private final Map<PredictionId, Prediction> byId = new LinkedHashMap<>();

    @Override
    public void save(Prediction prediction) {
        byId.put(prediction.getId(), prediction);
    }

    @Override
    public List<Prediction> findByGame(GameId gameId) {
        return byId.values().stream().filter(p -> p.getId().gameId().equals(gameId)).toList();
    }

    @Override
    public List<Prediction> findByAccount(EmailAddress account) {
        return byId.values().stream().filter(p -> p.getId().accountEmail().equals(account)).toList();
    }
}
