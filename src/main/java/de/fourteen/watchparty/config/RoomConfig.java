package de.fourteen.watchparty.config;

import de.fourteen.watchparty.application.RoomActor;
import de.fourteen.watchparty.application.port.out.ClientGateway;
import de.fourteen.watchparty.application.port.out.Scheduler;
import de.fourteen.watchparty.application.port.out.SnapshotRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Verdrahtet den {@link RoomActor} mit seinen Ports.
 *
 * Der Actor traegt selbst keine Annotation — er liegt im Anwendungsring und
 * soll Spring nicht kennen. Was frueher {@code @Component},
 * {@code @PostConstruct} und {@code @PreDestroy} an der Klasse waren, steht
 * jetzt hier als {@code initMethod} und {@code destroyMethod}.
 *
 * Die Reihenfolge bleibt dieselbe wie vorher: {@code loadOnStartup} reiht das
 * Laden des Snapshots als erstes Kommando ein und ist damit garantiert vor dem
 * ersten {@code JOIN} fertig (ADR-023).
 */
@Configuration
public class RoomConfig {

    @Bean(initMethod = "loadOnStartup", destroyMethod = "shutdown")
    public RoomActor roomActor(Clock clock, Scheduler scheduler, SnapshotRepository snapshots,
            ClientGateway clients) {
        return new RoomActor(clock, scheduler, snapshots, clients);
    }
}
