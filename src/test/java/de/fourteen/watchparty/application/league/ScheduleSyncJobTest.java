package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.FakeScheduler;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.PortTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der Nachfuehr-Job stoesst sich selbst regelmaessig an, ohne dass jemand
 * etwas tut (Kriterium 9, ADR-037) -- geprueft mit {@link FakeScheduler}
 * wie bei {@code RoomActor}s Auto-Close, ohne auf echte Zeit zu warten.
 */
@PortTest
class ScheduleSyncJobTest {

    private static final SeasonId SEASON = SeasonId.of(2026);
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");

    @Test
    @Anforderung("13.3-b")
    void startSynctSofortUndPlantSichSelbstNeu() {
        FakeGameRepository games = new FakeGameRepository();
        FakeScheduleFeed feed = new FakeScheduleFeed();
        Matchday erstenSpieltag = Matchday.of(SEASON, 1);
        feed.antworteMit(erstenSpieltag, Game.of(GameId.of("1"), erstenSpieltag, HOME, AWAY,
                Instant.parse("2026-09-10T17:00:00Z"), GameStatus.SCHEDULED, null, false));
        FakeScheduler scheduler = new FakeScheduler();
        ScheduleSyncJob job = new ScheduleSyncJob(new ScheduleSyncService(feed, games), scheduler, new FakeAlertSender(), SEASON,
                Duration.ofMinutes(15));

        job.start();

        assertThat(games.findById(GameId.of("1"))).as("der erste Abgleich passiert sofort, ohne Wartezeit").isPresent();
        assertThat(scheduler.pendingCount()).as("der naechste Lauf ist bereits eingeplant").isEqualTo(1);
    }

    @Test
    @Anforderung("13.3-b")
    void feuertDerGeplanteTaskWirdErneutSynchronisiertUndWiederNeuGeplant() {
        FakeGameRepository games = new FakeGameRepository();
        FakeScheduleFeed feed = new FakeScheduleFeed();
        FakeScheduler scheduler = new FakeScheduler();
        ScheduleSyncJob job = new ScheduleSyncJob(new ScheduleSyncService(feed, games), scheduler, new FakeAlertSender(), SEASON,
                Duration.ofMinutes(15));
        job.start();

        scheduler.fireAll();

        assertThat(scheduler.pendingCount()).as("nach jedem Lauf steht wieder genau ein naechster an").isEqualTo(1);
    }

    @Test
    void nachDreiFehlgeschlagenenLaeufenInFolgeWirdAlarmiert() {
        FakeGameRepository games = new FakeGameRepository();
        FakeScheduleFeed feed = new FakeScheduleFeed();
        feed.falleAusFuer(Matchday.of(SEASON, 1));
        FakeScheduler scheduler = new FakeScheduler();
        FakeAlertSender alerts = new FakeAlertSender();
        ScheduleSyncJob job = new ScheduleSyncJob(new ScheduleSyncService(feed, games), scheduler, alerts, SEASON,
                Duration.ofMinutes(15));

        job.start();
        assertThat(alerts.alerts()).as("nach dem ersten fehlgeschlagenen Lauf noch kein Alarm").isEmpty();
        scheduler.fireAll();
        assertThat(alerts.alerts()).as("nach dem zweiten fehlgeschlagenen Lauf noch kein Alarm").isEmpty();
        scheduler.fireAll();

        assertThat(alerts.alerts()).hasSize(1);
        assertThat(alerts.alerts().get(0).season()).isEqualTo(SEASON);
        assertThat(alerts.alerts().get(0).consecutiveFailedRuns()).isEqualTo(3);
    }

    @Test
    void nachEinemErneutErfolgreichenLaufFaelltDerZaehlerZurueckUndAlarmiertBeimNaechstenAusfallErneut() {
        FakeGameRepository games = new FakeGameRepository();
        FakeScheduleFeed feed = new FakeScheduleFeed();
        Matchday spieltag = Matchday.of(SEASON, 1);
        feed.falleAusFuer(spieltag);
        FakeScheduler scheduler = new FakeScheduler();
        FakeAlertSender alerts = new FakeAlertSender();
        ScheduleSyncJob job = new ScheduleSyncJob(new ScheduleSyncService(feed, games), scheduler, alerts, SEASON,
                Duration.ofMinutes(15));
        job.start();
        scheduler.fireAll();
        scheduler.fireAll();
        assertThat(alerts.alerts()).hasSize(1);

        feed.istWiederErreichbarFuer(spieltag);
        scheduler.fireAll();
        feed.falleAusFuer(spieltag);
        scheduler.fireAll();
        scheduler.fireAll();
        scheduler.fireAll();

        assertThat(alerts.alerts()).as("ein neuer Ausfall alarmiert erneut, nachdem der Zaehler zurueckgesetzt wurde").hasSize(2);
    }

    @Test
    void stopBrichtDenNaechstenGeplantenLaufAb() {
        FakeGameRepository games = new FakeGameRepository();
        FakeScheduleFeed feed = new FakeScheduleFeed();
        FakeScheduler scheduler = new FakeScheduler();
        ScheduleSyncJob job = new ScheduleSyncJob(new ScheduleSyncService(feed, games), scheduler, new FakeAlertSender(), SEASON,
                Duration.ofMinutes(15));
        job.start();

        job.stop();

        assertThat(scheduler.pendingCount()).isZero();
    }
}
