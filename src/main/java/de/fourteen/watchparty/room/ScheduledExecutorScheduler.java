package de.fourteen.watchparty.room;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Produktiv-Implementierung von {@link Scheduler} auf einem eigenen
 * Single-Thread-Pool. Der geplante Task selbst darf keinen Raumzustand
 * anfassen; er reiht bei Faelligkeit nur ein Kommando in den {@link RoomActor}
 * ein (ADR-010). Das Canceln hier ist reine Optimierung, keine Absicherung.
 */
public class ScheduledExecutorScheduler implements Scheduler {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "room-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public ScheduledTask schedule(Runnable task, Duration delay) {
        ScheduledFuture<?> future = executor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}
