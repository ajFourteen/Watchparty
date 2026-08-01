package de.fourteenit.watchparty.room;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Bindet Uhr und Scheduler produktiv an {@link RoomActor}. Im Test tritt an
 * ihre Stelle eine Fake-Uhr und ein Scheduler, der Tasks nur sammelt und auf
 * Kommando feuert (mvp-plan.md, Etappe 1).
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "shutdown")
    public Scheduler scheduler() {
        return new ScheduledExecutorScheduler();
    }
}
