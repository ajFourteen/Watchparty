package de.fourteenit.watchparty.room;

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

    @Override
    public void shutdown() {
        // Kein echter Thread im Test, nichts abzubauen.
    }
}
