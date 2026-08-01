package de.fourteenit.watchparty.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fourteenit.watchparty.protocol.Messages;
import de.fourteenit.watchparty.ws.ClientSession;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Der Eventloop des Raums (ADR-009).
 *
 * Alles, was Raum- oder Session-Zustand aendert, wird hier eingereiht und von
 * genau einem Thread seriell abgearbeitet. Die WebSocket-Threads rufen nur die
 * oeffentlichen Methoden auf und fassen den Zustand nie direkt an.
 *
 * Dadurch ist die gesamte Logik in {@code handle*} gewoehnlicher
 * Single-Thread-Code: kein synchronized, kein volatile, keine Concurrent-
 * Collections. Spaeter loesen sich Timer-vs-Bet und manueller-vs-automatischer
 * Marktschluss allein ueber die Reihenfolge in dieser Queue auf.
 */
@Component
public class RoomActor {

    private static final Logger log = LoggerFactory.getLogger(RoomActor.class);

    private final ExecutorService loop = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "room-actor");
        thread.setDaemon(true);
        return thread;
    });

    private final ObjectMapper mapper = new ObjectMapper();
    private final Room room = new Room();

    /** Nur vom Raum-Thread beruehrt, daher eine gewoehnliche HashMap. */
    private final Map<String, ClientSession> sessions = new LinkedHashMap<>();

    // --- Eintrittspunkte (aufgerufen von WebSocket-Threads) ------------------

    public void connected(ClientSession session) {
        loop.execute(() -> sessions.put(session.getId(), session));
    }

    public void disconnected(ClientSession session) {
        loop.execute(() -> handleDisconnected(session));
    }

    public void join(ClientSession session, String name, String token) {
        loop.execute(() -> handleJoin(session, name, token));
    }

    public void hostAction(ClientSession session) {
        loop.execute(() -> handleHostAction(session));
    }

    // --- Verarbeitung (laeuft ausschliesslich auf dem Raum-Thread) -----------

    private void handleJoin(ClientSession session, String rawName, String token) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.length() > 20) {
            sendTo(session, new Messages.Error("Bitte einen Namen mit 1 bis 20 Zeichen eingeben."));
            return;
        }

        Player player = room.byToken(token);
        if (player != null) {
            // Reconnect (ADR-014): dasselbe Konto, neue Verbindung.
            player.setName(name);
            player.setConnected(true);
        } else {
            player = room.addPlayer(UUID.randomUUID().toString(), UUID.randomUUID().toString(), name);
        }

        session.setPlayerId(player.getId());
        room.reassignHostIfNeeded();

        sendTo(session, new Messages.Welcome(player.getId(), player.getToken()));
        broadcastState();
        log.info("{} ist dabei ({} Spieler im Raum)", player.getName(), room.players().size());
    }

    private void handleDisconnected(ClientSession session) {
        sessions.remove(session.getId());
        String playerId = session.getPlayerId();
        if (playerId == null) {
            return;
        }
        // Nur wenn keine andere Session mehr auf diesen Spieler zeigt.
        boolean stillOnline = sessions.values().stream()
                .anyMatch(other -> playerId.equals(other.getPlayerId()));
        if (!stillOnline) {
            Player player = room.byId(playerId);
            if (player != null) {
                player.setConnected(false);
            }
        }
        room.reassignHostIfNeeded();
        broadcastState();
    }

    private void handleHostAction(ClientSession session) {
        if (!room.isHost(session.getPlayerId())) {
            sendTo(session, new Messages.Error("Nur der Host kann das ausloesen."));
            return;
        }
        room.bumpHostActionCount();
        broadcastState();
    }

    // --- Ausgang ------------------------------------------------------------

    private void broadcastState() {
        Messages.State state = snapshot();
        String payload = serialize(state);
        if (payload == null) {
            return;
        }
        for (ClientSession session : sessions.values()) {
            session.send(payload);
        }
    }

    private Messages.State snapshot() {
        List<Messages.PlayerView> views = new ArrayList<>();
        for (Player player : room.players()) {
            views.add(new Messages.PlayerView(
                    player.getId(),
                    player.getName(),
                    player.getPoints(),
                    player.isConnected(),
                    room.isHost(player.getId())));
        }
        return new Messages.State(views, room.getHostPlayerId(), room.getHostActionCount());
    }

    private void sendTo(ClientSession session, Object message) {
        String payload = serialize(message);
        if (payload != null) {
            session.send(payload);
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
        loop.shutdownNow();
    }
}
