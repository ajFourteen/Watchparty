package de.fourteen.watchparty.application.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

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

    /**
     * Der Wettkatalog haengt hier und nicht an STATE: Er ist ueber den ganzen
     * Abend unveraendert, muesste sonst aber bei jedem Zustandswechsel
     * mitgeschickt werden. Beim Reconnect kommt WELCOME erneut, der Client
     * hat ihn also immer (Invariante 3).
     */
    public record Welcome(String playerId, String token, List<BetView> catalog) {
        @JsonProperty("type")
        public String type() {
            return "WELCOME";
        }
    }

    /**
     * Ein vollstaendiger Zustand statt vieler Deltas, weil bei
     * Reconnect ohnehin alles neu geschickt wird (Invariante 3). Der Inhalt
     * haengt an der Phase — in OPEN nur der Pick-Zaehler (Invariante 4,
     * ADR-013), in CLOSED zusaetzlich alle Tipps offen, in RESOLVED
     * zusaetzlich Ergebnis, Pool und Deltas. Ungenutzte Felder bleiben null
     * und werden nicht serialisiert.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record State(
            List<PlayerView> players,
            @Nullable String hostPlayerId,
            String phase,
            @Nullable Long roundId,
            @Nullable BetView bet,
            @Nullable Long closesAt,
            long serverNow,
            @Nullable Integer pickCount,
            @Nullable Integer participantCount,
            @Nullable List<RevealedPick> revealedPicks,
            @Nullable String winningOutcomeId,
            @Nullable Integer pool,
            @Nullable Boolean annulled,
            /** {@code NO_PICKS} (Anforderung 8.4) oder {@code HOST} (8.6). */
            @Nullable String annulReason,
            @Nullable Map<String, Integer> deltas) {
        @JsonProperty("type")
        public String type() {
            return "STATE";
        }
    }

    public record PlayerView(String id, String name, int points, boolean connected, boolean paused, boolean host) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BetView(String id, String question, @Nullable String note, List<OutcomeView> outcomes) {
    }

    /**
     * Eigener Typ statt des Domaenen-{@code Outcome}. Seit dessen ID ein
     * Value Object ist, wuerde eine direkte Serialisierung
     * {@code {"id":{"value":"punt"}}} ergeben und damit das Protokoll
     * aendern. Bewusst ohne {@code NON_NULL}: {@code note} war schon immer
     * auch als {@code null} im Frame, das bleibt so.
     */
    public record OutcomeView(String id, String label, @Nullable String note) {
    }

    public record RevealedPick(String playerId, String outcomeId, int stake) {
    }

    /**
     * Geht nur an die eine Session, die den Tipp abgegeben hat — bei Annahme
     * und erneut beim Join/Reconnect mitten in OPEN. So sieht ein Spieler
     * seinen eigenen Tipp, ohne dass STATE je einzelne Tipps waehrend des
     * offenen Fensters an alle verteilt (ADR-013).
     */
    public record YourPick(String outcomeId, int stake) {
        @JsonProperty("type")
        public String type() {
            return "YOUR_PICK";
        }
    }

    public record Error(String message) {
        @JsonProperty("type")
        public String type() {
            return "ERROR";
        }
    }
}
