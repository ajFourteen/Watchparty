package de.fourteen.watchparty.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Ein Teilnehmer im Raum. <b>Entity</b> innerhalb des Aggregats
 * {@link Room}: Identitaet ueber {@link PlayerId}, nicht ueber die Werte —
 * derselbe Spieler bleibt derselbe, wenn sich Name und Punktestand aendern.
 *
 * Existiert unabhaengig von einer konkreten Verbindung: {@code connected}
 * spiegelt nur, ob gerade eine Sitzung auf diesen Spieler zeigt (Reconnect
 * via Token, ADR-014).
 *
 * Nur ueber das Aggregat erreichbar und nur vom Raum-Thread beruehrt
 * (Invariante 1), deshalb ohne jede Synchronisierung. Die Mutatoren sind
 * paket-privat: Wer einen Spieler aendern will, geht durch {@link Room}.
 */
public class Player {

    private final PlayerId id;
    private final Token token;
    private PlayerName name;
    private Points points;
    private boolean connected = true;

    /**
     * Zaehlt Runden, die getrennt am Stueck verpasst wurden (Anforderung 8.1).
     * Wird bei Reconnect auf 0 zurueckgesetzt; ab 2 gilt der Spieler als
     * pausiert und wird beim naechsten Oeffnen nicht mehr in den
     * Teilnehmerkreis eingefroren.
     */
    private int missedRounds;

    Player(PlayerId id, Token token, PlayerName name, Points points) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.points = points;
    }

    public PlayerId getId() {
        return id;
    }

    public Token getToken() {
        return token;
    }

    public PlayerName getName() {
        return name;
    }

    void setName(PlayerName name) {
        this.name = name;
    }

    public Points getPoints() {
        return points;
    }

    void setPoints(Points points) {
        this.points = points;
    }

    /** Verbucht das Ergebnis einer Runde. Wirft, wenn das Konto negativ wuerde (Invariante 5). */
    void credit(PointsDelta delta) {
        this.points = points.apply(delta);
    }

    public boolean isConnected() {
        return connected;
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getMissedRounds() {
        return missedRounds;
    }

    void incrementMissedRounds() {
        missedRounds++;
    }

    void resetMissedRounds() {
        missedRounds = 0;
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
     */
    public Points stakeFor(@Nullable Integer requestedStake, Params params) {
        Points minStake = params.minStake();
        if (points.isLessThan(minStake)) {
            return points;
        }
        Points wanted = requestedStake == null ? minStake : Points.of(Math.max(0, requestedStake));
        return wanted.isLessThan(minStake) ? minStake : wanted.min(points);
    }

    @Override
    public String toString() {
        return name + " (" + points + ")";
    }
}
