package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Ein Ergebnistipp: die Vorhersage eines Kontos zu genau einem Spiel.
 * Aggregate Root, Identität über {@link PredictionId}.
 *
 * Unveränderlich, mit genau einem Feld ausser der Identität — ein neuer
 * Tipp fürs selbe Spiel (Kriterium 16, bis zum Anstoß) ist ein Ersetzen per
 * Upsert über dieselbe {@link PredictionId}, kein benannter Übergang auf
 * einem bestehenden Objekt. Der Anstoss-Vergleich selbst gehört nicht
 * hierher: {@code Prediction} kennt kein {@link Game}, nur dessen {@link
 * GameId} — die Prüfung liegt in der Anwendungsschicht, die beide
 * Aggregate kennt.
 */
@AggregateRoot
public class Prediction {

    @Identity
    private final PredictionId id;
    private final GameScore score;

    private Prediction(PredictionId id, GameScore score) {
        this.id = id;
        this.score = score;
    }

    public static Prediction of(PredictionId id, GameScore score) {
        return new Prediction(id, score);
    }

    public PredictionId getId() {
        return id;
    }

    public GameScore getScore() {
        return score;
    }
}
