package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
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
 */
public class ScheduleSyncJob {

    private final ScheduleCommands scheduleCommands;
    private final Scheduler scheduler;
    private final SeasonId season;
    private final Duration interval;

    private Scheduler.@Nullable ScheduledTask task;

    public ScheduleSyncJob(ScheduleCommands scheduleCommands, Scheduler scheduler, SeasonId season, Duration interval) {
        this.scheduleCommands = scheduleCommands;
        this.scheduler = scheduler;
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
        scheduleCommands.syncSeason(season);
        task = scheduler.schedule(this::runAndReschedule, interval);
    }
}
