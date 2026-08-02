package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.domain.model.Room;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Sammelt geplante Tasks nur ein und feuert sie ausschliesslich auf Kommando
 * ({@link #fireAll()}). Damit lassen sich die zwei bekannten Fallen
 * (verspaeteter Auto-Close, Tipp kurz vor Schluss) als gewoehnliche
 * Ereignis-Sequenz durchspielen, ohne auf echte 15 Sekunden zu warten.
 */
public class FakeScheduler implements Scheduler {

    private static final class Pending {
        final Runnable task;
        boolean cancelled;

        Pending(Runnable task) {
            this.task = task;
        }
    }

    private final List<Pending> pending = new ArrayList<>();

    @Override
    public ScheduledTask schedule(Runnable task, Duration delay) {
        Pending scheduled = new Pending(task);
        pending.add(scheduled);
        return () -> scheduled.cancelled = true;
    }

    /** Feuert alle bislang geplanten, nicht zwischenzeitlich gecancelten Tasks. */
    public void fireAll() {
        List<Pending> due = new ArrayList<>(pending);
        pending.clear();
        for (Pending scheduled : due) {
            if (!scheduled.cancelled) {
                scheduled.task.run();
            }
        }
    }

    public int pendingCount() {
        return (int) pending.stream().filter(scheduled -> !scheduled.cancelled).count();
    }

    /**
     * Feuert den am laengsten wartenden Task, ungeachtet einer Cancellation.
     * Bildet die Race ab, die ADR-010 begruendet: Der Task kann beim Server
     * schon enqueued sein, wenn der Room-Thread ihn canceln will -- die
     * Absicherung ist dann allein die Runden-ID-Wache, nicht das Cancel.
     */
    public void fireOldestIgnoringCancellation() {
        if (pending.isEmpty()) {
            return;
        }
        pending.remove(0).task.run();
    }

    @Override
    public void shutdown() {
        // Kein echter Thread im Test, nichts abzubauen.
    }
}
