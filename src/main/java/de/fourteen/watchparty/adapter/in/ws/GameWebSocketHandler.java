package de.fourteen.watchparty.adapter.in.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fourteen.watchparty.application.port.in.RoomCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Duenne Schicht zwischen WebSocket und Raum: der Eingangs-Adapter.
 *
 * Wichtig (ADR-009): Diese Klasse aendert selbst keinen Spielzustand. Sie
 * uebersetzt eingehende Frames in Kommandos auf {@link RoomCommands} und
 * reicht sie weiter. Auch Verbindungsauf- und -abbau laufen ueber dieselbe
 * Queue, damit ein Disconnect relativ zu anderen Ereignissen korrekt
 * einsortiert wird.
 *
 * Weitergereicht wird nur die Sitzungs-ID, nie das Verbindungsobjekt — der
 * Anwendungsring kennt keine WebSockets. Die Verbindung selbst haelt der
 * {@link WebSocketClientGateway}.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final RoomCommands room;
    private final WebSocketClientGateway gateway;
    private final ObjectMapper mapper = new ObjectMapper();

    public GameWebSocketHandler(RoomCommands room, WebSocketClientGateway gateway) {
        this.room = room;
        this.gateway = gateway;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        gateway.register(session);
        room.connected(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            switch (type) {
                case "JOIN" -> room.join(
                        sessionId,
                        node.path("name").asText(null),
                        node.path("token").asText(null));
                case "OPEN_BET" -> room.openBet(sessionId, node.path("betId").asText(null));
                case "PLACE_PICK" -> room.placePick(
                        sessionId,
                        node.path("outcomeId").asText(null),
                        node.hasNonNull("stake") ? node.path("stake").asInt() : null);
                case "CLOSE_BET" -> room.closeBet(sessionId);
                case "RESOLVE" -> room.resolve(sessionId, node.path("outcomeId").asText(null));
                case "ANNUL" -> room.annul(sessionId);
                case "RESET" -> room.reset(sessionId);
                default -> log.debug("Unbekannter Nachrichtentyp: {}", type);
            }
        } catch (Exception e) {
            log.debug("Ungueltige Nachricht von {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Nur melden, wenn die Sitzung wirklich registriert war -- sonst
        // wuerde ein doppelter Close ein zweites Kommando einreihen.
        if (gateway.unregister(session.getId())) {
            room.disconnected(session.getId());
        }
    }
}
