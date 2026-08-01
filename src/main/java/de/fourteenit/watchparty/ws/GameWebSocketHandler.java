package de.fourteenit.watchparty.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fourteenit.watchparty.room.RoomActor;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Duenne Schicht zwischen WebSocket und Raum.
 *
 * Wichtig (ADR-009): Diese Klasse aendert selbst keinen Spielzustand. Sie
 * uebersetzt eingehende Frames in Kommandos und reicht sie an den
 * {@link RoomActor} weiter. Auch Verbindungsauf- und -abbau laufen ueber
 * dieselbe Queue, damit ein Disconnect relativ zu anderen Ereignissen korrekt
 * einsortiert wird.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final RoomActor roomActor;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Getrennter Pool fuer das Schreiben auf Sockets (ADR-012). Wird von allen
     * Sessions geteilt; pro Session sorgt die Ausgangs-Queue fuer Reihenfolge.
     */
    private final ExecutorService sendPool = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "ws-send");
        thread.setDaemon(true);
        return thread;
    });

    /** Diese Map wird von WebSocket-Threads beruehrt, daher nebenlaeufigkeitsfest. */
    private final Map<String, ClientSession> clients = new ConcurrentHashMap<>();

    public GameWebSocketHandler(RoomActor roomActor) {
        this.roomActor = roomActor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        ClientSession client = new ClientSession(session, sendPool);
        clients.put(session.getId(), client);
        roomActor.connected(client);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ClientSession client = clients.get(session.getId());
        if (client == null) {
            return;
        }
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            switch (type) {
                case "JOIN" -> roomActor.join(
                        client,
                        node.path("name").asText(null),
                        node.path("token").asText(null));
                case "OPEN_BET" -> roomActor.openBet(client, node.path("betId").asText(null));
                case "PLACE_PICK" -> roomActor.placePick(
                        client,
                        node.path("outcomeId").asText(null),
                        node.hasNonNull("stake") ? node.path("stake").asInt() : null);
                case "CLOSE_BET" -> roomActor.closeBet(client);
                case "RESOLVE" -> roomActor.resolve(client, node.path("outcomeId").asText(null));
                case "ANNUL" -> roomActor.annul(client);
                default -> log.debug("Unbekannter Nachrichtentyp: {}", type);
            }
        } catch (Exception e) {
            log.debug("Ungueltige Nachricht von {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ClientSession client = clients.remove(session.getId());
        if (client != null) {
            roomActor.disconnected(client);
        }
    }

    @PreDestroy
    void shutdown() {
        sendPool.shutdownNow();
    }
}
