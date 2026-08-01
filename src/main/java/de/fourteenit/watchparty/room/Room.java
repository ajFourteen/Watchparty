package de.fourteenit.watchparty.room;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Der eine Raum, komplett im Arbeitsspeicher (ADR-004).
 *
 * Kein Zugriff von aussen ausser ueber den {@link RoomActor}; alle Methoden
 * laufen auf dem Raum-Thread und sind deshalb ohne Synchronisierung geschrieben
 * (ADR-009).
 */
public class Room {

    /** Startguthaben. Konkreter Wert wird spaeter am Spielgefuehl kalibriert. */
    public static final int STARTING_POINTS = 1000;

    /** Einfuegereihenfolge zaehlt: der erste Joiner wird Host. */
    private final Map<String, Player> playersById = new LinkedHashMap<>();
    private final Map<String, String> playerIdByToken = new LinkedHashMap<>();

    private String hostPlayerId;

    private Round currentRound;
    private long nextRoundId = 1;

    public Player addPlayer(String id, String token, String name) {
        Player player = new Player(id, token, name, STARTING_POINTS);
        playersById.put(id, player);
        playerIdByToken.put(token, id);
        if (hostPlayerId == null) {
            hostPlayerId = id;
        }
        return player;
    }

    public Player byToken(String token) {
        if (token == null) {
            return null;
        }
        String id = playerIdByToken.get(token);
        return id == null ? null : playersById.get(id);
    }

    public Player byId(String id) {
        return playersById.get(id);
    }

    public Collection<Player> players() {
        return playersById.values();
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    public boolean isHost(String playerId) {
        return hostPlayerId != null && hostPlayerId.equals(playerId);
    }

    /**
     * Uebergibt die Host-Rolle an den am fruehesten beigetretenen verbundenen
     * Spieler (ADR-021). Verlieren wirkt immer sofort, sonst waere der Raum
     * mitten im offenen Fenster steuerlos. Zurueckholen — ein frueher
     * beigetretener Spieler kommt zurueck und soll die Rolle reklamieren —
     * wirkt nur, wenn {@code allowPickup} gilt, also in IDLE oder RESOLVED;
     * sonst rutschen dem Vertreter die Steuerknoepfe mitten in der Runde weg.
     */
    public void reassignHostIfNeeded(boolean allowPickup) {
        Player host = hostPlayerId == null ? null : playersById.get(hostPlayerId);
        boolean hostLost = host == null || !host.isConnected();
        if (!hostLost && !allowPickup) {
            return;
        }
        hostPlayerId = players().stream()
                .filter(Player::isConnected)
                .map(Player::getId)
                .findFirst()
                .orElse(null);
    }

    // --- Zustandsautomat der aktuellen Runde (ADR-020) ------------------------

    public Phase getPhase() {
        return currentRound == null ? Phase.IDLE : currentRound.getPhase();
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    /**
     * Oeffnet eine neue Runde. Der Teilnehmerkreis wird jetzt eingefroren
     * (Anforderung 8.1): dabei, wer verbunden ist, plus wer getrennt ist aber
     * noch keine zwei Runden am Stueck verpasst hat — ab der dritten
     * verpassten Runde pausiert ein getrennter Spieler und zahlt nicht mehr.
     */
    public Round openBet(Bet bet, Instant now, Duration window) {
        Set<String> participants = new LinkedHashSet<>();
        for (Player player : players()) {
            if (player.isConnected() || player.getMissedRounds() < 2) {
                participants.add(player.getId());
            }
        }
        currentRound = new Round(nextRoundId++, bet, now.plus(window), participants);
        return currentRound;
    }
}
