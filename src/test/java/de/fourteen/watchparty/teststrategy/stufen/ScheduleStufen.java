package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.league.FakeGameRepository;
import de.fourteen.watchparty.application.league.FakeScheduleFeed;
import de.fourteen.watchparty.application.league.ScheduleSyncService;
import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port-to-Port-Stufe (docs/teststrategie.md, Abschnitt 2.2) fuer den
 * Spielplan-Abgleich (ADR-037): Eingang ist {@link ScheduleSyncService} als
 * Umsetzung von {@link ScheduleCommands}, Ausgaenge sind handgeschriebene
 * Test Doubles (ADR-025) statt Postgres und echtem Netz.
 */
public class ScheduleStufen extends DeutscheStufe<ScheduleStufen> {

    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Instant KICKOFF = Instant.parse("2026-09-10T17:00:00Z");

    private final FakeGameRepository games = new FakeGameRepository();
    private final FakeScheduleFeed feed = new FakeScheduleFeed();
    private final ScheduleCommands schedule = new ScheduleSyncService(feed, games);

    public ScheduleStufen derFeedMeldetFuerEinGeplantesSpiel(String gameId, Matchday matchday) {
        feed.antworteMit(matchday, Game.of(GameId.of(gameId), matchday, HOME, AWAY, KICKOFF, GameStatus.SCHEDULED, null, false));
        return self();
    }

    public ScheduleStufen istBereitsAlsBeendetGespeichertMit(String gameId, Matchday matchday, int heim, int gast) {
        games.save(Game.of(GameId.of(gameId), matchday, HOME, AWAY, KICKOFF, GameStatus.FINAL, GameScore.of(heim, gast), false));
        return self();
    }

    public ScheduleStufen derFeedFaelltAusFuer(Matchday matchday) {
        feed.falleAusFuer(matchday);
        return self();
    }

    public ScheduleStufen derFeedMeldetFuerDasSpielDasKorrigierteErgebnis(String gameId, Matchday matchday, int heim, int gast) {
        feed.antworteMit(matchday, Game.of(GameId.of(gameId), matchday, HOME, AWAY, KICKOFF, GameStatus.FINAL, GameScore.of(heim, gast), false));
        return self();
    }

    public ScheduleStufen derFeedMeldetDasSpielAlsAbgesagt(String gameId, Matchday matchday) {
        feed.antworteMit(matchday, Game.of(GameId.of(gameId), matchday, HOME, AWAY, KICKOFF, GameStatus.CANCELLED, null, false));
        return self();
    }

    public ScheduleStufen wirdDerSpieltagAbgeglichen(Matchday matchday) {
        schedule.syncMatchday(matchday);
        return self();
    }

    public ScheduleStufen wirdEineExternAbgerufeneFeedAntwortEingespielt(Matchday matchday) {
        schedule.ingestRelayedFeed(matchday, "vom-relay-mitgebracht");
        return self();
    }

    public ScheduleStufen wirdEinErgebnisVonHandGesetzt(String gameId, int heim, int gast) {
        schedule.setResultManually(GameId.of(gameId), GameScore.of(heim, gast));
        return self();
    }

    public ScheduleStufen kenntDasSpielMitDemStatus(String gameId, GameStatus erwartet) {
        assertThat(gefundenesSpiel(gameId).getStatus()).isEqualTo(erwartet);
        return self();
    }

    public ScheduleStufen kenntFuerDasSpielDasErgebnis(String gameId, int heim, int gast) {
        assertThat(gefundenesSpiel(gameId).getScore()).isEqualTo(GameScore.of(heim, gast));
        return self();
    }

    private Game gefundenesSpiel(String gameId) {
        Optional<Game> gefunden = games.findById(GameId.of(gameId));
        assertThat(gefunden).as("Spiel " + gameId + " sollte bekannt sein").isPresent();
        return gefunden.get();
    }
}
