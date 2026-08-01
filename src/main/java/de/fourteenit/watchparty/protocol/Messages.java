package de.fourteenit.watchparty.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Nachrichten Server -> Client (README, Abschnitt Protokoll).
 *
 * Jede Nachricht traegt ihren {@code type} als String-Literal, damit das
 * Frontend ohne weiteres Schema per {@code message.type} unterscheiden kann.
 */
public final class Messages {

    private Messages() {
    }

    public record Welcome(String playerId, String token) {
        @JsonProperty("type")
        public String type() {
            return "WELCOME";
        }
    }

    public record State(List<PlayerView> players, String hostPlayerId, int hostActionCount) {
        @JsonProperty("type")
        public String type() {
            return "STATE";
        }
    }

    public record PlayerView(String id, String name, int points, boolean connected, boolean host) {
    }

    public record Error(String message) {
        @JsonProperty("type")
        public String type() {
            return "ERROR";
        }
    }
}
