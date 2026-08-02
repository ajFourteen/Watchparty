package de.fourteen.watchparty.application.port.out;

import de.fourteen.watchparty.application.RoomActor;

import java.time.Duration;

/**
 * Schmale Abstraktion ueber verzoegerte Ausfuehrung, damit {@code RoomActor}
 * im Test ohne echte Wartezeit auf Auto-Close & Co. geprueft werden kann
 * (ADR-010). Produktiv steckt ein
 * {@code ScheduledExecutorScheduler} dahinter, im Test ein Fake, der Tasks
 * nur sammelt und auf Kommando feuert.
 */
public interface Scheduler {

    ScheduledTask schedule(Runnable task, Duration delay);

    void shutdown();

    @FunctionalInterface
    interface ScheduledTask {
        void cancel();
    }
}
