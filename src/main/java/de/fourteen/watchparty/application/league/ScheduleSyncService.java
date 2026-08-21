package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.MatchdayCompletionListener;
import de.fourteen.watchparty.application.league.port.out.ScheduleFeed;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Setzt {@link ScheduleCommands} um (ADR-037). Erkennt zusaetzlich den
 * Uebergang eines Spieltags in den vollstaendig ausgewerteten Zustand
 * (13.9-o, ADR-041) und meldet ihn an {@link MatchdayCompletionListener} --
 * unabhaengig davon, ob der ausloesende Statuswechsel vom Feed-Abgleich
 * ({@link #merge}) oder vom Handeintrag ({@link #setResultManually}) kommt.
 */
public class ScheduleSyncService implements ScheduleCommands {

    private static final Logger log = LoggerFactory.getLogger(ScheduleSyncService.class);

    private final ScheduleFeed feed;
    private final GameRepository games;
    private final MatchdayCompletionListener matchdayCompletionListener;

    public ScheduleSyncService(ScheduleFeed feed, GameRepository games,
            MatchdayCompletionListener matchdayCompletionListener) {
        this.feed = feed;
        this.games = games;
        this.matchdayCompletionListener = matchdayCompletionListener;
    }

    @Override
    public void syncMatchday(Matchday matchday) {
        syncMatchdayReportingSuccess(matchday);
    }

    @Override
    public int syncSeason(SeasonId season) {
        int failedMatchdays = 0;
        for (int week = 1; week <= Matchday.REGULAR_SEASON_WEEKS; week++) {
            if (!syncMatchdayReportingSuccess(Matchday.of(season, week))) {
                failedMatchdays++;
            }
        }
        return failedMatchdays;
    }

    /** @return false, wenn der Feed fuer diesen Spieltag nicht erreichbar war. */
    private boolean syncMatchdayReportingSuccess(Matchday matchday) {
        List<Game> fetched;
        try {
            fetched = feed.fetchMatchday(matchday);
        } catch (RuntimeException e) {
            log.warn("Feed nicht erreichbar fuer {} -- letzter bekannter Stand bleibt stehen (Kriterium 11)",
                    matchday, e);
            return false;
        }
        merge(fetched);
        return true;
    }

    @Override
    public void ingestRelayedFeed(Matchday matchday, String rawResponse) {
        merge(feed.parseExternalResponse(matchday, rawResponse));
    }

    private void merge(List<Game> fetched) {
        for (Game feedGame : fetched) {
            games.findById(feedGame.getId()).ifPresentOrElse(
                    bestehendesSpiel -> {
                        boolean warBereitsAusgewertetVorher = istAusgewertet(bestehendesSpiel.getStatus());
                        bestehendesSpiel.mergeFromFeed(feedGame);
                        games.save(bestehendesSpiel);
                        if (!warBereitsAusgewertetVorher && istAusgewertet(bestehendesSpiel.getStatus())) {
                            meldeFallsSpieltagVollstaendig(bestehendesSpiel.getMatchday());
                        }
                    },
                    () -> {
                        games.save(feedGame);
                        if (istAusgewertet(feedGame.getStatus())) {
                            meldeFallsSpieltagVollstaendig(feedGame.getMatchday());
                        }
                    });
        }
    }

    @Override
    public void setResultManually(GameId gameId, GameScore score) {
        Game game = games.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Unbekanntes Spiel: " + gameId));
        boolean warBereitsAusgewertetVorher = istAusgewertet(game.getStatus());
        game.applyManualResult(score);
        games.save(game);
        if (!warBereitsAusgewertetVorher) {
            meldeFallsSpieltagVollstaendig(game.getMatchday());
        }
    }

    /** FINAL oder CANCELLED -- beides schliesst das Warten auf ein Ergebnis ab (13.3-f, 13.9-o). */
    private static boolean istAusgewertet(GameStatus status) {
        return status == GameStatus.FINAL || status == GameStatus.CANCELLED;
    }

    /** Meldet hoechstens einmal: Erreicht ist dieser Zustand nur beim Uebergang des zuletzt offenen Spiels. */
    private void meldeFallsSpieltagVollstaendig(Matchday matchday) {
        List<Game> spieleDesSpieltags = games.findByMatchday(matchday);
        boolean vollstaendig = !spieleDesSpieltags.isEmpty()
                && spieleDesSpieltags.stream().allMatch(spiel -> istAusgewertet(spiel.getStatus()));
        if (vollstaendig) {
            matchdayCompletionListener.onMatchdayCompleted(matchday);
        }
    }
}
