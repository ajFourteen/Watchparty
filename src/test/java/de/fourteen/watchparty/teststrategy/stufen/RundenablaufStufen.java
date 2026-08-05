package de.fourteen.watchparty.teststrategy.stufen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ein vollstaendiger Rundenablauf ueber einen echten Server und einen echten
 * WebSocket (docs/teststrategie.md, Abschnitt 2.4): Verdrahtung ueber
 * {@code config}, keine neue fachliche Abdeckung -- das hat die Port-Ebene
 * schon entschieden. Pilotszenario aus Phase 1 der Teststrategie-Umsetzung.
 */
public class RundenablaufStufen extends DeutscheStufe<RundenablaufStufen> {

    private final StandardWebSocketClient client = new StandardWebSocketClient();
    private final Map<String, RecordingClient> clientsByName = new LinkedHashMap<>();
    private int port;
    private String outcomeId;

    public RundenablaufStufen einServerLaeuftAufPort(int port) {
        this.port = port;
        return this;
    }

    public RundenablaufStufen hostUndAnnaTretenBei() throws Exception {
        beitreten("Host");
        beitreten("Anna");
        return this;
    }

    public RundenablaufStufen derHostOeffnetDieErsteWetteAusDemKatalog() throws Exception {
        RecordingClient host = clientVon("Host");
        JsonNode welcome = host.awaitType("WELCOME");
        String betId = welcome.path("catalog").get(0).path("id").asText();
        outcomeId = welcome.path("catalog").get(0).path("outcomes").get(0).path("id").asText();
        host.send("{\"type\":\"OPEN_BET\",\"betId\":\"" + betId + "\"}");
        host.awaitState(state -> "OPEN".equals(state.path("phase").asText()));
        return this;
    }

    public RundenablaufStufen beideTippenAufDenErstenAusgang() throws Exception {
        for (RecordingClient recording : clientsByName.values()) {
            recording.send("{\"type\":\"PLACE_PICK\",\"outcomeId\":\"" + outcomeId + "\"}");
        }
        for (RecordingClient recording : clientsByName.values()) {
            recording.awaitType("YOUR_PICK");
        }
        return this;
    }

    public RundenablaufStufen derHostSchliesstUndLoestZuGunstenDesErstenAusgangsAuf() throws Exception {
        RecordingClient host = clientVon("Host");
        host.send("{\"type\":\"CLOSE_BET\"}");
        host.awaitState(state -> "CLOSED".equals(state.path("phase").asText()));
        host.send("{\"type\":\"RESOLVE\",\"outcomeId\":\"" + outcomeId + "\"}");
        return this;
    }

    public RundenablaufStufen alleSehenAmEndeDiePhaseResolvedMitDemErgebnis() throws InterruptedException {
        for (RecordingClient recording : clientsByName.values()) {
            JsonNode state = recording.awaitState(s -> "RESOLVED".equals(s.path("phase").asText()));
            assertThat(state.path("winningOutcomeId").asText()).isEqualTo(outcomeId);
        }
        return this;
    }

    private void beitreten(String name) throws Exception {
        RecordingClient recording = new RecordingClient();
        WebSocketSession session = client
                .execute(recording, new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + "/ws"))
                .get(5, TimeUnit.SECONDS);
        recording.session = session;
        clientsByName.put(name, recording);
        recording.send("{\"type\":\"JOIN\",\"name\":\"" + name + "\"}");
        recording.awaitType("WELCOME");
    }

    private RecordingClient clientVon(String name) {
        return Objects.requireNonNull(clientsByName.get(name), "kein Beitritt fuer " + name);
    }

    private static final class RecordingClient extends TextWebSocketHandler {
        private final List<JsonNode> frames = new CopyOnWriteArrayList<>();
        private final ObjectMapper mapper = new ObjectMapper();
        private volatile WebSocketSession session;

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            frames.add(mapper.readTree(message.getPayload()));
        }

        void send(String payload) throws Exception {
            session.sendMessage(new TextMessage(payload));
        }

        JsonNode awaitType(String type) throws InterruptedException {
            return await(frame -> type.equals(frame.path("type").asText()));
        }

        JsonNode awaitState(Predicate<JsonNode> predicate) throws InterruptedException {
            return await(frame -> "STATE".equals(frame.path("type").asText()) && predicate.test(frame));
        }

        JsonNode await(Predicate<JsonNode> predicate) throws InterruptedException {
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
    }
}
