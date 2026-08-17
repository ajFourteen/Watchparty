package de.fourteen.watchparty.domain.service.league;

import org.jmolecules.ddd.annotation.Service;

import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.LeaguePoints;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Die Rangliste einer Liga (Kriterium 31/32): Mitglieder, gewertete Spiele
 * und die Ergebnistipps dazu ergeben eine Reihenfolge. Reine Funktion,
 * zustandslos wie {@link Scoring} — welche Spiele in {@code scoredGames}
 * stehen, entscheidet der Aufrufer (die ganze Saison oder ein einzelner
 * Spieltag, Kriterium 33), nicht dieser Dienst.
 *
 * Wer ein Spiel nicht getippt hat, bekommt dafuer 0 Wertungspunkte ohne
 * Strafe (Kriterium 18) — {@code predictionOf} liefert dann {@link
 * Optional#empty()} und das Spiel geht ohne Beitrag in die Summe ein.
 *
 * Gleichstand (Kriterium 32): zuerst die Gesamtpunktzahl, dann die Zahl der
 * exakten Ergebnisse, dann die Zahl der richtigen Tendenzen (ein exaktes
 * Ergebnis zaehlt hier mit, da es zwangslaeufig auch die Tendenz trifft).
 * Bleibt es dabei gleich, teilen sich die Tipper denselben Platz — die
 * naechste unterscheidbare Stufe erhaelt die Platznummer nach der Groesse
 * der Gruppe, nicht die naechste laufende Zahl (Formel-1-Zaehlung: 1., 1.,
 * 3., nicht 1., 1., 2.).
 */
@Service
public final class Standings {

    private Standings() {
    }

    public record Member(EmailAddress email, DisplayName displayName) {
    }

    public record ScoredGame(GameId gameId, GameScore actualScore) {
    }

    public record Entry(EmailAddress email, DisplayName displayName, LeaguePoints totalPoints,
            int exactCount, int correctTendencyCount, int rank) {
    }

    public static List<Entry> compute(List<Member> members, List<ScoredGame> scoredGames,
            BiFunction<EmailAddress, GameId, Optional<GameScore>> predictionOf) {
        List<Entry> unranked = new ArrayList<>();
        for (Member member : members) {
            int totalPoints = 0;
            int exactCount = 0;
            int correctTendencyCount = 0;
            for (ScoredGame game : scoredGames) {
                Optional<GameScore> predicted = predictionOf.apply(member.email(), game.gameId());
                if (predicted.isEmpty()) {
                    continue;
                }
                LeaguePoints points = Scoring.score(predicted.get(), game.actualScore());
                totalPoints += points.value();
                if (points.equals(LeaguePoints.EXACT)) {
                    exactCount++;
                }
                if (points.value() > 0) {
                    correctTendencyCount++;
                }
            }
            unranked.add(new Entry(member.email(), member.displayName(),
                    new LeaguePoints(totalPoints), exactCount, correctTendencyCount, 0));
        }
        return rank(unranked);
    }

    private static List<Entry> rank(List<Entry> entries) {
        List<Entry> sorted = entries.stream()
                .sorted(Comparator
                        .comparing(Entry::totalPoints)
                        .thenComparingInt(Entry::exactCount)
                        .thenComparingInt(Entry::correctTendencyCount)
                        .reversed())
                .toList();

        List<Entry> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Entry current = sorted.get(i);
            int platz = (i > 0 && gleicherStand(sorted.get(i - 1), current)) ? ranked.get(i - 1).rank() : i + 1;
            ranked.add(new Entry(current.email(), current.displayName(), current.totalPoints(),
                    current.exactCount(), current.correctTendencyCount(), platz));
        }
        return ranked;
    }

    private static boolean gleicherStand(Entry a, Entry b) {
        return a.totalPoints().equals(b.totalPoints())
                && a.exactCount() == b.exactCount()
                && a.correctTendencyCount() == b.correctTendencyCount();
    }
}
