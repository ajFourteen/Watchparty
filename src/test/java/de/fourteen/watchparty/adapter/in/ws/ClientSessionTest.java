package de.fourteen.watchparty.adapter.in.ws;

import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deckt ADR-012 auf Klassenebene ab: {@link ClientSession#send(String)} darf
 * den aufrufenden (im Betrieb: den Raum-)Thread nie fuers Schreiben auf den
 * Socket blockieren, muss die Reihenfolge pro Client erhalten und darf bei
 * einem Client, der nichts mehr abnimmt, nicht unbegrenzt wachsen.
 *
 * Arbeitet mit {@link FakeWebSocketSession} statt einem Mock-Framework: Was
 * hier geprueft wird, ist Verhalten unter Nebenlaeufigkeit, kein Aufruf-
 * protokoll.
 */
@AdapterTest
class ClientSessionTest {

    private static final Duration GEDULD = Duration.ofSeconds(2);

    private final ExecutorService sendPool = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        sendPool.shutdownNow();
    }

    @Test
    void sendKehrtSofortZurueckAuchWennDasEigentlicheSchreibenBlockiert() throws Exception {
        FakeWebSocketSession socket = new FakeWebSocketSession();
        CountDownLatch writerDarfWeiter = socket.blockiereBeimSenden();

        ClientSession session = new ClientSession(socket, sendPool);

        long start = System.nanoTime();
        session.send("erste-nachricht");
        long dauerMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(socket.warteBisSendenHaengt(GEDULD))
                .as("der Sende-Pool haengt wirklich im Schreibvorgang")
                .isTrue();
        assertThat(dauerMillis)
                .as("send() selbst hat nicht gewartet")
                .isLessThan(200);

        writerDarfWeiter.countDown();
    }

    @Test
    void reihenfolgeProClientBleibtErhalten() throws Exception {
        FakeWebSocketSession socket = new FakeWebSocketSession();

        ClientSession session = new ClientSession(socket, sendPool);
        for (int i = 0; i < 50; i++) {
            session.send("nachricht-" + i);
        }

        socket.warteBisGesendet(50, GEDULD);

        List<String> erwartet = IntStream.range(0, 50).mapToObj(i -> "nachricht-" + i).toList();
        assertThat(socket.gesendeteNachrichten()).containsExactlyElementsOf(erwartet);
    }

    @Test
    void eineVolleAusgangsQueueSchliesstDieSessionStattUnbegrenztZuWachsen() throws Exception {
        FakeWebSocketSession socket = new FakeWebSocketSession();
        CountDownLatch writerDarfWeiter = socket.blockiereBeimSenden();

        ClientSession session = new ClientSession(socket, sendPool);
        session.send("erste-nachricht");
        assertThat(socket.warteBisSendenHaengt(GEDULD)).isTrue();

        // Der Sende-Thread haengt jetzt im Schreibvorgang fest, die
        // Ausgangs-Queue kann also nur noch wachsen.
        for (int i = 0; i < 205; i++) {
            session.send("nachricht-" + i);
        }

        writerDarfWeiter.countDown();

        assertThat(socket.warteBisGeschlossen(GEDULD))
                .as("die Session wurde geschlossen statt weiter zu puffern")
                .isTrue();
        assertThat(socket.anzahlSchliessungen()).isPositive();
    }

    /**
     * Tomcat wirft beim Schreiben in eine gerade schliessende Session eine
     * {@code IllegalStateException}, keine {@code IOException} -- gefunden
     * beim Nachruesten von Phase 3.5 als unbehandelte Ausnahme im
     * Sende-Pool-Thread. Muss denselben Aufraeumpfad nehmen wie ein echter
     * I/O-Fehler, statt den Thread unbehandelt zu verlassen.
     */
    @Test
    void eineIllegalStateExceptionBeimSendenSchliesstDieSessionStattDenThreadZuVerlassen() throws Exception {
        FakeWebSocketSession socket = new FakeWebSocketSession();
        socket.wirftBeimNaechstenSendenEineIllegalStateException();

        ClientSession session = new ClientSession(socket, sendPool);
        session.send("nachricht");

        assertThat(socket.warteBisGeschlossen(GEDULD))
                .as("die Session wurde aufgeraeumt statt die Ausnahme unbehandelt zu lassen")
                .isTrue();
    }
}
