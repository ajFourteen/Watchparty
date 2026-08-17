package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Ein Spiel eines Spieltags (Kriterium 8): Anstoß, beide Mannschaften,
 * Status und — nach dem Spiel — das Endergebnis. Aggregate Root.
 *
 * Aendert sich ueber zwei benannte Uebergaenge, beide vom Nachfuehr-Job
 * bzw. dem Handeintrag aus der Anwendungsschicht aufgerufen:
 * {@link #mergeFromFeed} fuer den regelmaessigen Abgleich mit dem Feed
 * (ADR-037), {@link #applyManualResult} fuer den Notweg aus Kriterium 14.
 * Identitaet, Spieltag und Mannschaften aendern sich nie -- nur Anstoss,
 * Status, Ergebnis und die Handeintrag-Markierung.
 */
@AggregateRoot
public class Game {

    @Identity
    private final GameId id;
    private final Matchday matchday;
    private final Team homeTeam;
    private final Team awayTeam;
    private Instant kickoff;
    private GameStatus status;
    private @Nullable GameScore score;
    private boolean manualOverride;

    private Game(GameId id, Matchday matchday, Team homeTeam, Team awayTeam, Instant kickoff,
            GameStatus status, @Nullable GameScore score, boolean manualOverride) {
        if (status == GameStatus.SCHEDULED && score != null) {
            throw new IllegalArgumentException("Ein noch nicht beendetes Spiel hat kein Ergebnis");
        }
        if (status == GameStatus.FINAL && score == null) {
            throw new IllegalArgumentException("Ein beendetes Spiel braucht ein Ergebnis");
        }
        this.id = id;
        this.matchday = matchday;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoff = kickoff;
        this.status = status;
        this.score = score;
        this.manualOverride = manualOverride;
    }

    /** Baut ein Spiel aus seinen Werten auf — frisch vom Feed geholt oder aus der Datenbank wieder aufgebaut. */
    public static Game of(GameId id, Matchday matchday, Team homeTeam, Team awayTeam, Instant kickoff,
            GameStatus status, @Nullable GameScore score, boolean manualOverride) {
        return new Game(id, matchday, homeTeam, awayTeam, kickoff, status, score, manualOverride);
    }

    /**
     * Gleicht diesen Stand mit einem frisch vom Feed geholten ab (ADR-037).
     * Der Anstoss wird immer uebernommen -- eine Verlegung (Flex-Scheduling)
     * gilt ab dann, ohne bereits abgegebene Tipps rueckwirkend zu entwerten
     * (Kriterium 10). Status und Ergebnis dagegen nur, solange kein
     * Handeintrag vorliegt: Ein Handeintrag ueberschreibt den Feed
     * (Kriterium 14) und bleibt bestehen, bis ihn ein neuer Handeintrag
     * ersetzt -- der Feed darf ihn nicht stillschweigend zuruecknehmen.
     */
    public void mergeFromFeed(Game feedGame) {
        this.kickoff = feedGame.kickoff;
        if (!manualOverride) {
            this.status = feedGame.status;
            this.score = feedGame.score;
        }
    }

    /** Der Notweg aus Kriterium 14: der Betreiber setzt ein Endergebnis von Hand. */
    public void applyManualResult(GameScore score) {
        this.score = score;
        this.status = GameStatus.FINAL;
        this.manualOverride = true;
    }

    public GameId getId() {
        return id;
    }

    public Matchday getMatchday() {
        return matchday;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public Instant getKickoff() {
        return kickoff;
    }

    public GameStatus getStatus() {
        return status;
    }

    public @Nullable GameScore getScore() {
        return score;
    }

    public boolean isManualOverride() {
        return manualOverride;
    }
}
