package de.fourteen.watchparty.adapter.in.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fourteen.watchparty.application.port.out.ClientGateway;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Der WebSocket-seitige Adapter des Ausgangs-Ports {@link ClientGateway}.
 *
 * Haelt die offenen Verbindungen und uebersetzt Nachrichtenobjekte in JSON.
 * Beides wusste frueher der {@code RoomActor} selbst — er hielt eine Map von
 * {@code ClientSession} und einen eigenen {@code ObjectMapper}. Beides ist
 * Protokoll-Sache und liegt jetzt hier.
 *
 * Liegt bewusst neben dem {@link GameWebSocketHandler} unter {@code in/ws} und
 * nicht unter {@code out/}, obwohl er einen Ausgangs-Port bedient: WebSocket
 * ist eine Verbindung in beide Richtungen, und die Sitzungsverwaltung ueber
 * zwei Pakete zu verteilen waere schlechter als die kleine Unschaerfe im
 * Namen.
 *
 * Nebenlaeufigkeit: {@link #register}/{@link #unregister} laufen auf
 * WebSocket-Threads, {@link #send}/{@link #sendToAll} auf dem Raum-Thread —
 * deshalb eine ConcurrentHashMap. Beide Sendewege kehren sofort zurueck
 * (Invariante 2); geschrieben wird auf dem Sende-Pool.
 */
@Component
public class WebSocketClientGateway implements ClientGateway {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientGateway.class);

    private final ObjectMapper mapper = new ObjectMapper();

    /** Wird von WebSocket-Threads beruehrt, daher nebenlaeufigkeitsfest. */
    private final Map<String, ClientSession> clients = new ConcurrentHashMap<>();

    /**
     * Getrennter Pool fuer das Schreiben auf Sockets (ADR-012). Wird von allen
     * Sitzungen geteilt; pro Sitzung sorgt die Ausgangs-Queue fuer Reihenfolge.
     */
    private final ExecutorService sendPool = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "ws-send");
        thread.setDaemon(true);
        return thread;
    });

    ClientSession register(org.springframework.web.socket.WebSocketSession socket) {
        ClientSession client = new ClientSession(socket, sendPool);
        clients.put(socket.getId(), client);
        return client;
    }

    boolean unregister(String sessionId) {
        return clients.remove(sessionId) != null;
    }

    @Override
    public void send(String sessionId, Object message) {
        ClientSession client = clients.get(sessionId);
        if (client == null) {
            return;
        }
        String payload = serialize(message);
        if (payload != null) {
            client.send(payload);
        }
    }

    @Override
    public void sendToAll(Collection<String> sessionIds, Object message) {
        // Einmal serialisieren, nicht je Empfaenger -- dieselbe Optimierung,
        // die der Actor frueher selbst gemacht hat.
        String payload = serialize(message);
        if (payload == null) {
            return;
        }
        for (String sessionId : sessionIds) {
            ClientSession client = clients.get(sessionId);
            if (client != null) {
                client.send(payload);
            }
        }
    }

    private String serialize(Object message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Nachricht nicht serialisierbar", e);
            return null;
        }
    }

    @PreDestroy
    void shutdown() {
        sendPool.shutdownNow();
    }
}
