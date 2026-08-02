package de.fourteen.watchparty.adapter.in.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Haelt eine WebSocket-Verbindung samt eigener Ausgangs-Queue.
 *
 * Umsetzung von ADR-012: Der Raum-Thread ruft — ueber den
 * {@code WebSocketClientGateway} — nur {@link #send(String)} auf und legt die
 * Nachricht in die Queue. Das eigentliche Schreiben auf den Socket passiert
 * auf einem Sende-Pool. Damit kann ein langsames oder eingeschlafenes Handy
 * den Eventloop nicht mehr stallen.
 *
 * Reine Infrastruktur: ein Socket und eine Queue, sonst nichts. Die Zuordnung
 * Sitzung-zu-Spieler lag frueher als Feld hier und ist seit dem Schnitt in
 * Ringe im {@code RoomActor} — sie ist Anwendungszustand und gehoerte nie
 * neben den Socket.
 *
 * Das Flag {@code draining} stellt sicher, dass immer nur ein Thread gleichzeitig
 * auf dieser Session schreibt (WebSocketSession ist nicht thread-safe) und dass
 * die Reihenfolge der Nachrichten pro Client erhalten bleibt.
 */
public class ClientSession {

    private static final Logger log = LoggerFactory.getLogger(ClientSession.class);

    /** Schutz gegen unbegrenztes Wachsen, wenn ein Client gar nichts mehr abnimmt. */
    private static final int MAX_QUEUED = 200;

    private final WebSocketSession session;
    private final Executor sendPool;
    private final Queue<String> outbox = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean draining = new AtomicBoolean(false);

    public ClientSession(WebSocketSession session, Executor sendPool) {
        this.session = session;
        this.sendPool = sendPool;
    }

    public String getId() {
        return session.getId();
    }

    public void send(String payload) {
        if (outbox.size() >= MAX_QUEUED) {
            log.warn("Ausgangs-Queue voll, schliesse Session {}", getId());
            closeQuietly();
            return;
        }
        outbox.add(payload);
        scheduleDrain();
    }

    private void scheduleDrain() {
        if (draining.compareAndSet(false, true)) {
            sendPool.execute(this::drain);
        }
    }

    private void drain() {
        try {
            String message;
            while ((message = outbox.poll()) != null) {
                if (!session.isOpen()) {
                    outbox.clear();
                    return;
                }
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.debug("Senden fehlgeschlagen, Session {}: {}", getId(), e.getMessage());
                    outbox.clear();
                    closeQuietly();
                    return;
                }
            }
        } finally {
            draining.set(false);
            // Waehrend des Leerens kann Neues dazugekommen sein.
            if (!outbox.isEmpty() && session.isOpen()) {
                scheduleDrain();
            }
        }
    }

    public void closeQuietly() {
        try {
            session.close();
        } catch (IOException ignored) {
            // Verbindung ist ohnehin hin.
        }
    }
}
