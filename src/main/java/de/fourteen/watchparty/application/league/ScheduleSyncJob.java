package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.application.league.port.out.AlertSender;
import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Stoesst {@link ScheduleCommands#syncSeason} regelmaessig an, ohne dass
 * jemand etwas tut (Kriterium 9) — ueber den bereits vorhandenen
 * {@link Scheduler}-Port, denselben wie fuer Auto-Close bei den
 * Live-Wetten, hier nur mit einem deutlich selteneren Takt (ADR-037). Der
 * einzige bewusste Ausnahmefall von der Trennung der Spielmodi
 * ({@code ArchitectureTest}): {@code Scheduler} ist eine reine
 * Zeitplanungs-Abstraktion ohne jeden Live-Wetten-Begriff, kein Zugriff auf
 * {@code Room} oder verwandte Typen.
 *
 * Verzoegert sich selbst neu, statt einer festen Wiederholung des Ports zu
 * vertrauen (den es dafuer nicht gibt) — dieselbe Selbst-Nachplanung wie
 * {@code RoomActor} fuer Auto-Close.
 *
 * Zaehlt zusaetzlich aufeinanderfolgende Laeufe, in denen der Feed fuer
 * mindestens einen Spieltag nicht erreichbar war, und alarmiert ab {@link
 * #ALERT_THRESHOLD} Laeufen in Folge (docs/betrieb-tippspiel.md) — bewusst
 * hier und nicht in {@code ScheduleSyncService}: Das ist eine Eigenschaft
 * der *wiederholten* Ausfuehrung, nicht des einzelnen Abgleichs, den die
 * Port-to-Port-Szenarien pruefen. Der Zaehler laeuft nur ueber den einen
 * Scheduler-Thread, der diese Klasse aufruft — keine Nebenlaeufigkeit,
 * kein Synchronisierungsbedarf.
 */
public class ScheduleSyncJob {

    private static final int ALERT_THRESHOLD = 3;

    private final ScheduleCommands scheduleCommands;
    private final Scheduler scheduler;
    private final AlertSender alerts;
    private final SeasonId season;
    private final Duration interval;

    private Scheduler.@Nullable ScheduledTask task;
    private int consecutiveFailedRuns = 0;
    private boolean alerted = false;

    public ScheduleSyncJob(ScheduleCommands scheduleCommands, Scheduler scheduler, AlertSender alerts,
            SeasonId season, Duration interval) {
        this.scheduleCommands = scheduleCommands;
        this.scheduler = scheduler;
        this.alerts = alerts;
        this.season = season;
        this.interval = interval;
    }

    /** Synct sofort und danach im konfigurierten Takt weiter, bis {@link #stop}. */
    public void start() {
        runAndReschedule();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void runAndReschedule() {
        int failedMatchdays = scheduleCommands.syncSeason(season);
        recordRunResult(failedMatchdays);
        task = scheduler.schedule(this::runAndReschedule, interval);
    }

    private void recordRunResult(int failedMatchdays) {
        if (failedMatchdays == 0) {
            consecutiveFailedRuns = 0;
            alerted = false;
            return;
        }
        consecutiveFailedRuns++;
        if (consecutiveFailedRuns >= ALERT_THRESHOLD && !alerted) {
            alerts.feedUnreachable(season, consecutiveFailedRuns);
            alerted = true;
        }
    }
}
