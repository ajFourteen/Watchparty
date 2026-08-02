package de.fourteen.watchparty.room;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
