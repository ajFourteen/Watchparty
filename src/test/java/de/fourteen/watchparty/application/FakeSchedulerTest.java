package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.teststrategy.PortTest;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testinfrastruktur der Port-Ebene: {@link FakeScheduler} macht deren Zeit- und Reihenfolge-Szenarien deterministisch. */
@PortTest
class FakeSchedulerTest {

    @Test
    void feuertNurNichtGecancelteTasksBeimFireAll() {
        FakeScheduler scheduler = new FakeScheduler();
        List<String> fired = new ArrayList<>();

        scheduler.schedule(() -> fired.add("a"), Duration.ofSeconds(15));
        Scheduler.ScheduledTask taskB = scheduler.schedule(() -> fired.add("b"), Duration.ofSeconds(15));
        taskB.cancel();

        scheduler.fireAll();

        assertThat(fired).containsExactly("a");
    }

    @Test
    void firedTasksWerdenNachFireAllNichtErneutAusgefuehrt() {
        FakeScheduler scheduler = new FakeScheduler();
        List<String> fired = new ArrayList<>();
        scheduler.schedule(() -> fired.add("a"), Duration.ofSeconds(15));

        scheduler.fireAll();
        scheduler.fireAll();

        assertThat(fired).containsExactly("a");
    }
}
