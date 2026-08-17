package de.fourteen.watchparty.adapter.in.ws;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adapter-Ebene (docs/teststrategie.md, Abschnitt 2.3): jeder Nachrichtentyp
 * aus {@link Messages} muss vollstaendig ankommen. Fixtures werden am
 * Port-Datentyp (den {@code Messages}-Records selbst) konstruiert, nicht
 * durch die Domaene erzeugt.
 */
@AdapterTest
class WebSocketClientGatewayTest {

    private final WebSocketClientGateway gateway = new WebSocketClientGateway();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void jederNachrichtentypSerialisiertVollstaendigMitTypFeld() throws Exception {
        FakeWebSocketSession session = new FakeWebSocketSession();
        gateway.register(session);

        gateway.send(session.getId(), new Messages.Welcome("AB3D", "p1", "t1", List.of(
                new Messages.BetView("drive-outcome", "Frage", "Anmerkung",
                        List.of(new Messages.OutcomeView("touchdown", "Touchdown", null)))),
                new Messages.Params(1000, 25, 25)));
        gateway.send(session.getId(), vollStaendigerZustand());
        gateway.send(session.getId(), new Messages.YourPick("touchdown", 25));
        gateway.send(session.getId(), new Messages.Error("Bitte zuerst beitreten."));

        session.warteBisGesendet(4, Duration.ofSeconds(2));
        List<String> frames = session.gesendeteNachrichten();
        assertThat(frames).hasSize(4);

        JsonNode welcome = mapper.readTree(frames.get(0));
        assertThat(welcome.path("type").asText()).isEqualTo("WELCOME");
        assertThat(welcome.path("roomCode").asText()).isEqualTo("AB3D");
        assertThat(welcome.path("catalog").get(0).path("outcomes").get(0).path("id").asText()).isEqualTo("touchdown");
        // 3.1-c: der Client bekommt die Parameter genannt, statt sie zu kennen
        assertThat(welcome.path("params").path("minStake").asInt()).isEqualTo(25);
        assertThat(welcome.path("params").path("penalty").asInt()).isEqualTo(25);
        assertThat(welcome.path("params").path("startingPoints").asInt()).isEqualTo(1000);

        JsonNode state = mapper.readTree(frames.get(1));
        assertThat(state.path("type").asText()).isEqualTo("STATE");
        assertThat(state.path("players").get(0).path("id").asText()).isEqualTo("p1");
        assertThat(state.path("revealedPicks").get(0).path("outcomeId").asText()).isEqualTo("touchdown");
        assertThat(state.path("deltas").path("p1").asInt()).isEqualTo(25);
        assertThat(state.path("nonPickers").get(0).asText()).isEqualTo("p2");

        JsonNode yourPick = mapper.readTree(frames.get(2));
        assertThat(yourPick.path("type").asText()).isEqualTo("YOUR_PICK");
        assertThat(yourPick.path("stake").asInt()).isEqualTo(25);

        JsonNode error = mapper.readTree(frames.get(3));
        assertThat(error.path("type").asText()).isEqualTo("ERROR");
        assertThat(error.path("message").asText()).isEqualTo("Bitte zuerst beitreten.");
    }

    /** Ein Zustand mit den RESOLVED-spezifischen Feldern belegt (Ergebnis, Pool, Deltas). */
    private static Messages.State vollStaendigerZustand() {
        return new Messages.State(
                List.of(new Messages.PlayerView("p1", "Anna", 1025, true, false, true)),
                "p1",
                "RESOLVED",
                3L,
                new Messages.BetView("drive-outcome", "Frage", null,
                        List.of(new Messages.OutcomeView("touchdown", "Touchdown", null))),
                null,
                987654321L,
                null,
                null,
                List.of(new Messages.RevealedPick("p1", "touchdown", 25)),
                List.of("p2"),
                "touchdown",
                25,
                false,
                null,
                Map.of("p1", 25));
    }
}
