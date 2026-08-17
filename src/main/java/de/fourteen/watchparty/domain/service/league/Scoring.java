package de.fourteen.watchparty.domain.service.league;

import org.jmolecules.ddd.annotation.Service;

import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.LeaguePoints;
import de.fourteen.watchparty.domain.model.league.ScoreBucket;

/**
 * Die Wertung eines Ergebnistipps gegen ein Endergebnis (13.5, ADR-038):
 * reine Funktion, zustandslos wie
 * {@link de.fourteen.watchparty.domain.service.Settlement} — dieselbe
 * Eingabe ergibt immer dieselbe Punktzahl, ohne Seiteneffekt und ohne
 * verstecktes Datum.
 *
 * Hoechste erreichte Stufe zaehlt, nicht die Summe: exaktes Ergebnis 6,
 * sonst richtige Tendenz und richtiger Abstands-Eimer 5, sonst nur richtige
 * Tendenz 3, sonst 0. Der Abstand wird nur bei richtiger Tendenz gewertet
 * — wer den Sieger verwechselt, hat nicht „das knappe Spiel erkannt",
 * sondern 0 Punkte, unabhaengig davon, wie nah die Zahlen liegen (13.5-b).
 */
@Service
@Criticality(level = Criticality.Level.HIGH,
        requirements = { "13.5-a", "13.5-b", "13.5-c", "13.5-d", "13.5-e" })
public final class Scoring {

    private Scoring() {
    }

    public static LeaguePoints score(GameScore predicted, GameScore actual) {
        if (predicted.equals(actual)) {
            return LeaguePoints.EXACT;
        }
        if (predicted.tendency() != actual.tendency()) {
            return LeaguePoints.NONE;
        }
        if (ScoreBucket.of(predicted.margin()) == ScoreBucket.of(actual.margin())) {
            return LeaguePoints.TENDENCY_AND_BUCKET;
        }
        return LeaguePoints.TENDENCY_ONLY;
    }
}
