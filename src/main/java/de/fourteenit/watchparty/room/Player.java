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

    /**
     * Zaehlt Runden, die getrennt am Stueck verpasst wurden (Anforderung 8.1).
     * Wird bei Reconnect auf 0 zurueckgesetzt; ab 2 gilt der Spieler als
     * pausiert und wird beim naechsten {@code OPEN_MARKET} nicht mehr in den
     * Teilnehmerkreis eingefroren.
     */
    private int missedRounds;

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

    public int getMissedRounds() {
        return missedRounds;
    }

    public void incrementMissedRounds() {
        missedRounds++;
    }

    public void resetMissedRounds() {
        missedRounds = 0;
    }

    /** Nur für den Wiederaufbau aus einem Snapshot (ADR-023), sonst zählt nur increment/reset. */
    void restoreMissedRounds(int missedRounds) {
        this.missedRounds = missedRounds;
    }

    /** Getrennt und schon zwei Runden am Stueck verpasst (Anforderung 8.1). */
    public boolean isPaused() {
        return !connected && missedRounds >= 2;
    }

    /**
     * Der Einsatz, mit dem dieser Spieler tatsaechlich tippt (Anforderung
     * 6/8.3). Der Mindesteinsatz ist der Standard, wer nichts angibt, setzt
     * ihn. Nach oben begrenzt der Kontostand.
     *
     * Wer weniger als den Mindesteinsatz besitzt, geht zwangsweise All-in --
     * auch mit 0 Punkten und unabhaengig davon, was angefragt wurde. Ohne
     * diese Ausnahme waere die Null ein absorbierender Zustand: Wer einmal
     * pleite ist, koennte nie wieder mitspielen (8.3).
     *
     * Haengt nur am Kontostand und den Parametern, nicht am Raum -- deshalb
     * hier und nicht im {@link RoomActor}.
     */
    public int stakeFor(Integer requestedStake, Params params) {
        int minStake = params.minStake();
        if (points < minStake) {
            return points;
        }
        int wanted = requestedStake == null ? minStake : requestedStake;
        return Math.max(minStake, Math.min(wanted, points));
    }
}
