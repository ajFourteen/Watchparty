package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.Prediction;

import java.util.List;

/** Ausgangs-Port fuer Ergebnistipps. Spricht {@link Prediction} — ein Datenmodell der Domaene. */
public interface PredictionRepository {

    /** Ein Upsert ueber die PredictionId (Konto, Spiel) — ein neuer Tipp ersetzt den bestehenden (Kriterium 16). */
    void save(Prediction prediction);

    List<Prediction> findByGame(GameId gameId);

    /** Alle Ergebnistipps eines Kontos, ueber alle Spiele und Ligen hinweg (13.6-c) — fuer den ligaunabhaengigen Punktestand. */
    List<Prediction> findByAccount(EmailAddress account);
}
