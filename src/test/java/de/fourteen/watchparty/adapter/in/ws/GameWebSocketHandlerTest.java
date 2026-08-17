package de.fourteen.watchparty.adapter.in.ws;

import de.fourteen.watchparty.application.port.in.RoomCommands;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adapter-Ebene (docs/teststrategie.md, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was der Port ausdruecken kann? Hier speziell die
 * Kehrseite -- ein kaputtes oder unvollstaendiges Frame darf weder die
 * Verbindung noch den Raum-Thread toeten (Invariante 2), sondern muss still
 * verworfen werden. Keine neue fachliche Abdeckung, das hat die Port-Ebene
 * schon entschieden.
 */
@AdapterTest
class GameWebSocketHandlerTest {

    private final RecordingRoomCommands commands = new RecordingRoomCommands();
    private final WebSocketClientGateway gateway = new WebSocketClientGateway();
    private final GameWebSocketHandler handler = new GameWebSocketHandler(commands, gateway);

    @Test
    void kaputtesJsonToetetWederVerbindungNochLoestEinKommandoAus() {
        FakeWebSocketSession session = new FakeWebSocketSession();
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{ das ist kein json"));

        assertThat(session.isOpen()).isTrue();
        assertThat(commands.aufrufe()).containsExactly("connected(" + session.getId() + ")");
    }

    @Test
    void unbekannterNachrichtentypWirdStillIgnoriert() {
        FakeWebSocketSession session = new FakeWebSocketSession();
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"DAS_GIBT_ES_NICHT\"}"));

        assertThat(session.isOpen()).isTrue();
        assertThat(commands.aufrufe()).containsExactly("connected(" + session.getId() + ")");
    }

    @Test
    void frameOhneTypFeldWirdStillIgnoriert() {
        FakeWebSocketSession session = new FakeWebSocketSession();
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"name\":\"Anna\"}"));

        assertThat(session.isOpen()).isTrue();
        assertThat(commands.aufrufe()).containsExactly("connected(" + session.getId() + ")");
    }

    @Test
    void joinOhneNameFeldWirdMitNullWeitergereichtStattZuWerfen() {
        FakeWebSocketSession session = new FakeWebSocketSession();
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\"}"));

        assertThat(session.isOpen()).isTrue();
        assertThat(commands.aufrufe()).containsExactly(
                "connected(" + session.getId() + ")",
                "join(" + session.getId() + ", null, null, null)");
    }

    @Test
    void createRoomOhneNameFeldWirdMitNullWeitergereichtStattZuWerfen() {
        FakeWebSocketSession session = new FakeWebSocketSession();
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"CREATE_ROOM\"}"));

        assertThat(session.isOpen()).isTrue();
        assertThat(commands.aufrufe()).containsExactly(
                "connected(" + session.getId() + ")",
                "createRoom(" + session.getId() + ", null)");
    }

    @Test
    void placePickOhneEinsatzFeldWirdMitNullEinsatzWeitergereicht() {
        FakeWebSocketSession session = new FakeWebSocketSession();
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"PLACE_PICK\",\"outcomeId\":\"touchdown\"}"));

        assertThat(commands.aufrufe()).containsExactly(
                "connected(" + session.getId() + ")",
                "placePick(" + session.getId() + ", touchdown, null)");
    }

    /** Von Hand geschrieben statt Mockito (ADR-025): zeichnet nur auf, was tatsaechlich ankam. */
    private static final class RecordingRoomCommands implements RoomCommands {
        private final List<String> aufrufe = new ArrayList<>();

        List<String> aufrufe() {
            return aufrufe;
        }

        @Override
        public void connected(String sessionId) {
            aufrufe.add("connected(" + sessionId + ")");
        }

        @Override
        public void disconnected(String sessionId) {
            aufrufe.add("disconnected(" + sessionId + ")");
        }

        @Override
        public void createRoom(String sessionId, String name) {
            aufrufe.add("createRoom(" + sessionId + ", " + name + ")");
        }

        @Override
        public void join(String sessionId, String name, String token, String roomCode) {
            aufrufe.add("join(" + sessionId + ", " + name + ", " + token + ", " + roomCode + ")");
        }

        @Override
        public void openBet(String sessionId, String betId) {
            aufrufe.add("openBet(" + sessionId + ", " + betId + ")");
        }

        @Override
        public void placePick(String sessionId, String outcomeId, Integer stake) {
            aufrufe.add("placePick(" + sessionId + ", " + outcomeId + ", " + stake + ")");
        }

        @Override
        public void closeBet(String sessionId) {
            aufrufe.add("closeBet(" + sessionId + ")");
        }

        @Override
        public void resolve(String sessionId, String outcomeId) {
            aufrufe.add("resolve(" + sessionId + ", " + outcomeId + ")");
        }

        @Override
        public void annul(String sessionId) {
            aufrufe.add("annul(" + sessionId + ")");
        }

        @Override
        public void reset(String sessionId) {
            aufrufe.add("reset(" + sessionId + ")");
        }
    }
}
