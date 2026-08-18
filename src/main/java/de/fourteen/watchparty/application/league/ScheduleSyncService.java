package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.ScheduleFeed;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

/** Setzt {@link ScheduleCommands} um (ADR-037). */
public class ScheduleSyncService implements ScheduleCommands {

    private static final Logger log = LoggerFactory.getLogger(ScheduleSyncService.class);

    private final ScheduleFeed feed;
    private final GameRepository games;

    public ScheduleSyncService(ScheduleFeed feed, GameRepository games) {
        this.feed = feed;
        this.games = games;
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

        for (Game feedGame : fetched) {
            games.findById(feedGame.getId()).ifPresentOrElse(
                    bestehendesSpiel -> {
                        bestehendesSpiel.mergeFromFeed(feedGame);
                        games.save(bestehendesSpiel);
                    },
                    () -> games.save(feedGame));
        }
        return true;
    }

    @Override
    public void setResultManually(GameId gameId, GameScore score) {
        Game game = games.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Unbekanntes Spiel: " + gameId));
        game.applyManualResult(score);
        games.save(game);
    }
}
