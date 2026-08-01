package de.fourteenit.watchparty.room;

/**
 * Ein Teilnehmer im Raum. Existiert unabhaengig von einer konkreten
 * Verbindung: {@code connected} spiegelt nur, ob gerade eine Session auf
 * diesen Spieler zeigt (Reconnect via Token, ADR-014).
 */
public class Player {

    private final String id;
    private final String token;
    private String name;
    private int points;
    private boolean connected = true;

    public Player(String id, String token, String name, int points) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
