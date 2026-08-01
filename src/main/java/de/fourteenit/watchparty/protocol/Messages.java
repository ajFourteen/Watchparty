package de.fourteenit.watchparty.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fourteenit.watchparty.room.Outcome;

import java.util.List;
import java.util.Map;

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

    /**
     * Ein vollstaendiger Zustand statt vieler Deltas (Etappe 4), weil bei
     * Reconnect ohnehin alles neu geschickt wird (Invariante 3). Der Inhalt
     * haengt an der Phase — in OPEN nur der Tipp-Zaehler (Invariante 4,
     * ADR-013), in CLOSED zusaetzlich alle Tipps offen, in RESOLVED
     * zusaetzlich Ergebnis, Pool und Deltas. Ungenutzte Felder bleiben null
     * und werden nicht serialisiert.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record State(
            List<PlayerView> players,
            String hostPlayerId,
            String phase,
            Long roundId,
            MarketView market,
            Long closesAt,
            long serverNow,
            Integer betCount,
            Integer participantCount,
            List<RevealedBet> revealedBets,
            String winningOutcomeId,
            Integer pool,
            Boolean annulled,
            Map<String, Integer> deltas) {
        @JsonProperty("type")
        public String type() {
            return "STATE";
        }
    }

    public record PlayerView(String id, String name, int points, boolean connected, boolean paused, boolean host) {
    }

    public record MarketView(String id, String question, List<Outcome> outcomes) {
    }

    public record RevealedBet(String playerId, String outcomeId, int stake) {
    }

    /**
     * Geht nur an die eine Session, die den Tipp abgegeben hat — bei Annahme
     * und erneut beim Join/Reconnect mitten in OPEN. So sieht ein Spieler
     * seinen eigenen Tipp, ohne dass STATE je einzelne Tipps waehrend des
     * offenen Fensters an alle verteilt (ADR-013).
     */
    public record YourBet(String outcomeId, int stake) {
        @JsonProperty("type")
        public String type() {
            return "YOUR_BET";
        }
    }

    public record Error(String message) {
        @JsonProperty("type")
        public String type() {
            return "ERROR";
        }
    }
}
