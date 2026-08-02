package de.fourteen.watchparty.adapter.in.ws;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Deckt ADR-012 auf Klassenebene ab: {@link ClientSession#send(String)} darf
 * den aufrufenden (im Betrieb: den Raum-)Thread nie fuers Schreiben auf den
 * Socket blockieren, muss die Reihenfolge pro Client erhalten und darf bei
 * einem Client, der nichts mehr abnimmt, nicht unbegrenzt wachsen.
 */
class ClientSessionTest {

    private final ExecutorService sendPool = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        sendPool.shutdownNow();
    }

    @Test
    void sendKehrtSofortZurueckAuchWennDasEigentlicheSchreibenBlockiert() throws Exception {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.isOpen()).thenReturn(true);
        CountDownLatch writerBlockiert = new CountDownLatch(1);
        CountDownLatch writerDarfWeiter = new CountDownLatch(1);
        doAnswer(invocation -> {
            writerBlockiert.countDown();
            writerDarfWeiter.await();
            return null;
        }).when(socket).sendMessage(any(TextMessage.class));

        ClientSession session = new ClientSession(socket, sendPool);

        long start = System.nanoTime();
        session.send("erste-nachricht");
        long dauerMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(writerBlockiert.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(dauerMillis).isLessThan(200);

        writerDarfWeiter.countDown();
    }

    @Test
    void reihenfolgeProClientBleibtErhalten() throws Exception {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.isOpen()).thenReturn(true);
        List<String> geschrieben = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> {
            TextMessage message = invocation.getArgument(0);
            geschrieben.add(message.getPayload());
            return null;
        }).when(socket).sendMessage(any(TextMessage.class));

        ClientSession session = new ClientSession(socket, sendPool);
        for (int i = 0; i < 50; i++) {
            session.send("nachricht-" + i);
        }

        awaitAnzahl(geschrieben, 50);

        List<String> erwartet = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> "nachricht-" + i)
                .toList();
        assertThat(geschrieben).containsExactlyElementsOf(erwartet);
    }

    @Test
    void eineVolleAusgangsQueueSchliesstDieSessionStattUnbegrenztZuWachsen() throws Exception {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.isOpen()).thenReturn(true);
        CountDownLatch writerBlockiert = new CountDownLatch(1);
        CountDownLatch writerDarfWeiter = new CountDownLatch(1);
        doAnswer(invocation -> {
            writerBlockiert.countDown();
            writerDarfWeiter.await();
            return null;
        }).when(socket).sendMessage(any(TextMessage.class));

        ClientSession session = new ClientSession(socket, sendPool);
        session.send("erste-nachricht");
        assertThat(writerBlockiert.await(2, TimeUnit.SECONDS)).isTrue();

        // Der Sende-Thread haengt jetzt in sendMessage() fest, die
        // Ausgangs-Queue kann also nur noch wachsen.
        for (int i = 0; i < 205; i++) {
            session.send("nachricht-" + i);
        }

        writerDarfWeiter.countDown();
        verify(socket, timeout(2000).atLeastOnce()).close();
    }

    private static void awaitAnzahl(List<String> liste, int erwartet) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (liste.size() < erwartet && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }
}
