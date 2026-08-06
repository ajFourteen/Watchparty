package de.fourteen.watchparty.adapter.in.ws;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ein von Hand geschriebenes Test Double fuer {@link WebSocketSession}.
 *
 * Bewusst kein Mock-Framework: Was dieser Test wirklich braucht — beim
 * Senden blockieren, die Reihenfolge mitschreiben, Schliessungen zaehlen —
 * ist Verhalten, kein Aufzeichnen von Aufrufen. Als Double steht es
 * ausdruecklich da, ist im Debugger lesbar und bricht nicht, wenn sich die
 * Signatur einer Methode aendert, die niemand benutzt.
 *
 * Wird von mehreren Threads beruehrt (Testthread und Sende-Pool), deshalb
 * nebenlaeufigkeitsfeste Felder.
 */
class FakeWebSocketSession implements WebSocketSession {

    private final String id;
    private volatile boolean open = true;

    private final List<String> gesendet = new CopyOnWriteArrayList<>();
    private final AtomicInteger schliessungen = new AtomicInteger();
    private final CountDownLatch geschlossen = new CountDownLatch(1);

    /** Nicht null, solange {@link #sendMessage} im Aufruf haengen bleiben soll. */
    private volatile CountDownLatch freigabe;
    private volatile CountDownLatch sendenHaengt;

    private volatile boolean wirftBeimNaechstenSenden;

    FakeWebSocketSession() {
        this("fake-socket");
    }

    FakeWebSocketSession(String id) {
        this.id = id;
    }

    // --- Steuerung durch den Test -------------------------------------------

    /**
     * Laesst den naechsten {@code sendMessage}-Aufruf haengen, bis der
     * zurueckgegebene Riegel geoeffnet wird. So laesst sich ein langsames oder
     * eingeschlafenes Handy nachstellen (ADR-012).
     */
    CountDownLatch blockiereBeimSenden() {
        freigabe = new CountDownLatch(1);
        sendenHaengt = new CountDownLatch(1);
        return freigabe;
    }

    /** Wartet, bis wirklich ein Sendeversuch im blockierten Aufruf steht. */
    boolean warteBisSendenHaengt(Duration timeout) throws InterruptedException {
        return sendenHaengt.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Wartet, bis mindestens {@code anzahl} Nachrichten geschrieben wurden. */
    void warteBisGesendet(int anzahl, Duration timeout) throws InterruptedException {
        long ende = System.nanoTime() + timeout.toNanos();
        while (gesendet.size() < anzahl && System.nanoTime() < ende) {
            Thread.sleep(5);
        }
    }

    boolean warteBisGeschlossen(Duration timeout) throws InterruptedException {
        return geschlossen.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    List<String> gesendeteNachrichten() {
        return List.copyOf(gesendet);
    }

    /**
     * Bildet nach, dass Tomcat beim Schreiben in eine gerade schliessende
     * Session eine {@link IllegalStateException} wirft, keine
     * {@link java.io.IOException} -- {@code isOpen()} hat das zu diesem
     * Zeitpunkt noch nicht mitbekommen.
     */
    void wirftBeimNaechstenSendenEineIllegalStateException() {
        wirftBeimNaechstenSenden = true;
    }

    int anzahlSchliessungen() {
        return schliessungen.get();
    }

    // --- WebSocketSession ---------------------------------------------------

    @Override
    public void sendMessage(WebSocketMessage<?> message) {
        if (wirftBeimNaechstenSenden) {
            wirftBeimNaechstenSenden = false;
            throw new IllegalStateException("Message will not be sent because the WebSocket session has been closed");
        }
        CountDownLatch riegel = freigabe;
        if (riegel != null) {
            sendenHaengt.countDown();
            try {
                riegel.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        gesendet.add(((TextMessage) message).getPayload());
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
        schliessungen.incrementAndGet();
        geschlossen.countDown();
    }

    @Override
    public void close(CloseStatus status) {
        close();
    }

    @Override
    public String getId() {
        return id;
    }

    // --- Der Rest der Schnittstelle, den dieser Test nicht braucht -----------
    //
    // Bewusst als Fehler und nicht als stilles null: Wer eine dieser Methoden
    // benutzt, soll es merken, statt einer NullPointerException an ganz
    // anderer Stelle nachzugehen.

    @Override
    public URI getUri() {
        throw nichtGebraucht("getUri");
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
        throw nichtGebraucht("getHandshakeHeaders");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return new HashMap<>();
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public String getAcceptedProtocol() {
        return null;
    }

    @Override
    public void setTextMessageSizeLimit(int messageSizeLimit) {
        throw nichtGebraucht("setTextMessageSizeLimit");
    }

    @Override
    public int getTextMessageSizeLimit() {
        return 0;
    }

    @Override
    public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        throw nichtGebraucht("setBinaryMessageSizeLimit");
    }

    @Override
    public int getBinaryMessageSizeLimit() {
        return 0;
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
        return List.of();
    }

    private static UnsupportedOperationException nichtGebraucht(String methode) {
        return new UnsupportedOperationException(
                "FakeWebSocketSession." + methode + " wird von keinem Test gebraucht");
    }
}
