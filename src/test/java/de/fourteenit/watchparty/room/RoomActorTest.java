package de.fourteenit.watchparty.room;

import de.fourteenit.watchparty.ws.ClientSession;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueft den in Etappe 1 eingefuehrten Testzugang {@code awaitIdle()}: Ohne
 * ihn waere jede Assertion nach einem {@code loop.execute(...)} race-behaftet,
 * weil die Verarbeitung asynchron auf dem Raum-Thread laeuft.
 */
class RoomActorTest {

    @Test
    void awaitIdleWartetBisDerJoinVerarbeitetIst() throws Exception {
        RoomActor actor = new RoomActor(new FakeClock(Instant.parse("2026-08-01T20:00:00Z")), new FakeScheduler());

        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("socket-1");
        when(socket.isOpen()).thenReturn(true);
        ClientSession session = new ClientSession(socket, Runnable::run);

        actor.connected(session);
        actor.join(session, "Alex", null);
        actor.awaitIdle();

        // Ohne awaitIdle koennte diese Pruefung vor der Verarbeitung laufen.
        verify(socket, atLeastOnce()).sendMessage(any());
    }
}
