package de.fourteenit.watchparty.room;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /**
     * Nur fuer das Skeleton: beweist, dass eine Host-Aktion serverseitig
     * verarbeitet und an alle verteilt wird. Faellt weg, sobald der echte
     * Zustandsautomat (IDLE/OPEN/CLOSED/RESOLVED) einzieht.
     */
    private int hostActionCount;

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
     * Uebergibt die Host-Rolle an den naechsten verbundenen Spieler.
     * Ohne das waere der Raum steuerlos, sobald der erste Joiner den Browser
     * schliesst (siehe Anmerkung zur Host-Regel).
     */
    public void reassignHostIfNeeded() {
        Player host = hostPlayerId == null ? null : playersById.get(hostPlayerId);
        if (host != null && host.isConnected()) {
            return;
        }
        hostPlayerId = players().stream()
                .filter(Player::isConnected)
                .map(Player::getId)
                .findFirst()
                .orElse(null);
    }

    public int getHostActionCount() {
        return hostActionCount;
    }

    public int bumpHostActionCount() {
        return ++hostActionCount;
    }
}
