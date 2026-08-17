package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.Matchday;

import java.util.List;
import java.util.Optional;

/** Ausgangs-Port fuer Spiele. Spricht {@link Game} — ein Datenmodell der Domaene. */
public interface GameRepository {

    void save(Game game);

    Optional<Game> findById(GameId id);

    List<Game> findByMatchday(Matchday matchday);
}
