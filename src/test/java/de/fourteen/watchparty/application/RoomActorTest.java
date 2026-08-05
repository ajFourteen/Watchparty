package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.teststrategy.PortTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueft den Testzugang {@code awaitIdle()}: Ohne ihn waere jede Assertion
 * nach einem {@code loop.execute(...)} race-behaftet, weil die Verarbeitung
 * asynchron auf dem Raum-Thread laeuft.
 *
 * Kommt seit dem Schnitt in Ringe ohne Mockito und ohne WebSocket aus — der
 * Actor spricht nur noch Sitzungs-IDs und Nachrichtenobjekte. Statt "es wurde
 * irgendetwas gesendet" laesst sich jetzt pruefen, <em>was</em> ankam.
 */
@PortTest
class RoomActorTest {

    @Test
    void awaitIdleWartetBisDerJoinVerarbeitetIst() {
        RecordingClientGateway gateway = new RecordingClientGateway();
        RoomActor actor = new RoomActor(
                new FakeClock(Instant.parse("2026-08-01T20:00:00Z")),
                new FakeScheduler(),
                new NoSnapshots(),
                gateway);

        actor.connected("socket-1");
        actor.join("socket-1", "Alex", null);
        actor.awaitIdle();

        // Ohne awaitIdle koennte diese Pruefung vor der Verarbeitung laufen.
        assertThat(gateway.messagesFor("socket-1")).hasAtLeastOneElementOfType(Messages.Welcome.class);
        assertThat(gateway.playerIdOf("socket-1")).isNotNull();
    }
}
