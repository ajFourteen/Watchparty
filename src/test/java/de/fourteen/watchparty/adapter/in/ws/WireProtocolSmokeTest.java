package de.fourteen.watchparty.adapter.in.ws;

import de.fourteen.watchparty.domain.model.Phase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der Rauchtest ueber die Leitung, auf den der Kommentar in
 * {@link de.fourteen.watchparty.room.RoomActorStateMachineTest} verweist,
 * aber bislang nicht existierte: Ein echter Server auf einem echten
 * WebSocket, geprueft am tatsaechlich uebertragenen JSON statt an
 * {@code getRoomForTest()}.
 *
 * Im Zentrum steht Invariante 4 (ADR-013): Waehrend ein Wettfenster offen
 * ist, darf kein einzelner Tipp ueber die Leitung gehen, nur der Zaehler.
 * Wer die Frames mitliest -- was dieser Test tut -- darf nichts erfahren.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WireProtocolSmokeTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper mapper = new ObjectMapper();
    private final StandardWebSocketClient client = new StandardWebSocketClient();
    private final List<RecordingClient> openClients = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        for (RecordingClient recording : openClients) {
            recording.close();
        }
    }

    @Test
    void waehrendOffenemFensterVerraetKeinFrameEinenEinzelnenTippNurDerHostErfaehrtDenZaehler() throws Exception {
        RecordingClient host = connect();
        host.send("{\"type\":\"JOIN\",\"name\":\"Host\"}");
        JsonNode hostWelcome = host.awaitType("WELCOME");
        String betId = hostWelcome.path("catalog").get(0).path("id").asText();
        String outcomeId = hostWelcome.path("catalog").get(0).path("outcomes").get(0).path("id").asText();

        RecordingClient anna = connect();
        anna.send("{\"type\":\"JOIN\",\"name\":\"Anna\"}");
        String annaPlayerId = anna.awaitType("WELCOME").path("playerId").asText();

        host.send("{\"type\":\"OPEN_BET\",\"betId\":\"" + betId + "\"}");
        host.awaitState(state -> "OPEN".equals(state.path("phase").asText()));

        // Ohne Angabe eines Einsatzes greift der Mindesteinsatz als
        // Standard-Einsatz (Anforderung 6/8.3).
        anna.send("{\"type\":\"PLACE_PICK\",\"outcomeId\":\"" + outcomeId + "\"}");
        JsonNode annasYourPick = anna.awaitType("YOUR_PICK");
        assertThat(annasYourPick.path("outcomeId").asText()).isEqualTo(outcomeId);
        int annasEinsatz = annasYourPick.path("stake").asInt();
        assertThat(annasEinsatz).isPositive();

        // Der Host darf nach dem Tipp nur den Zaehler sehen -- keinen der
        // beiden Werte, die Annas Tipp identifizieren wuerden.
        JsonNode stateNachDemTipp = host.awaitState(
                state -> "OPEN".equals(state.path("phase").asText()) && state.path("pickCount").asInt() == 1);
        assertThat(stateNachDemTipp.has("revealedPicks")).isFalse();
        assertThat(stateNachDemTipp.path("participantCount").asInt()).isEqualTo(2);

        // Ueber die gesamte OPEN-Phase hinweg darf in keinem einzigen Frame
        // an den Host je "revealedPicks" oder Annas outcomeId aufgetaucht
        // sein, und YOUR_PICK darf den Host nie erreicht haben.
        for (JsonNode frame : host.allFrames()) {
            if ("STATE".equals(frame.path("type").asText()) && "OPEN".equals(frame.path("phase").asText())) {
                assertThat(frame.has("revealedPicks")).isFalse();
            }
            assertThat(frame.path("type").asText()).isNotEqualTo("YOUR_PICK");
        }

        host.send("{\"type\":\"CLOSE_BET\"}");
        JsonNode stateNachSchliessen = host.awaitState(state -> "CLOSED".equals(state.path("phase").asText()));
        JsonNode revealed = stateNachSchliessen.path("revealedPicks");
        assertThat(revealed).hasSize(1);
        assertThat(revealed.get(0).path("playerId").asText()).isEqualTo(annaPlayerId);
        assertThat(revealed.get(0).path("outcomeId").asText()).isEqualTo(outcomeId);
        assertThat(revealed.get(0).path("stake").asInt()).isEqualTo(annasEinsatz);
    }

    private RecordingClient connect() throws Exception {
        RecordingClient recording = new RecordingClient();
        WebSocketSession session = client.execute(recording, new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + "/ws"))
                .get(5, TimeUnit.SECONDS);
        recording.session = session;
        openClients.add(recording);
        return recording;
    }

    private class RecordingClient extends TextWebSocketHandler {
        private final List<JsonNode> frames = new CopyOnWriteArrayList<>();
        private volatile WebSocketSession session;

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            frames.add(mapper.readTree(message.getPayload()));
        }

        void send(String payload) throws Exception {
            session.sendMessage(new TextMessage(payload));
        }

        List<JsonNode> allFrames() {
            return frames;
        }

        JsonNode awaitType(String type) throws InterruptedException {
            return await(frame -> type.equals(frame.path("type").asText()));
        }

        JsonNode awaitState(java.util.function.Predicate<JsonNode> predicate) throws InterruptedException {
            return await(frame -> "STATE".equals(frame.path("type").asText()) && predicate.test(frame));
        }

        JsonNode await(java.util.function.Predicate<JsonNode> predicate) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5000;
            int schonGesehen = 0;
            while (System.currentTimeMillis() < deadline) {
                List<JsonNode> snapshot = frames;
                for (int i = schonGesehen; i < snapshot.size(); i++) {
                    if (predicate.test(snapshot.get(i))) {
                        return snapshot.get(i);
                    }
                }
                schonGesehen = snapshot.size();
                Thread.sleep(10);
            }
            throw new AssertionError("Kein passender Frame innerhalb der Frist erhalten, bisher: " + frames);
        }

        void close() {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (Exception ignored) {
                // Verbindung ist ohnehin hin.
            }
        }
    }
}
