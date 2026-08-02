package de.fourteen.watchparty.application.port.in;

/**
 * Eingangs-Port: alles, was von aussen am Raum ausgeloest werden kann
 * (ADR-020). Der WebSocket-Adapter kennt nur dieses Interface, nicht den
 * {@code RoomActor} dahinter.
 *
 * <b>Jede Methode reiht nur ein und kehrt sofort zurueck</b> (Invariante 1).
 * Kein Aufrufer bekommt ein Ergebnis: Was aus einem Kommando folgt, erfahren
 * die Clients ueber den {@code ClientGateway}. Diese Asymmetrie ist Absicht
 * und der Grund, warum die Raum-Logik ohne Synchronisierung auskommt.
 *
 * Sitzungen heissen hier {@code sessionId}, nicht {@code ClientSession} — der
 * Anwendungsring kennt keine Verbindungen, nur ihre Namen.
 */
public interface RoomCommands {

    void connected(String sessionId);

    void disconnected(String sessionId);

    void join(String sessionId, String name, String token);

    void openBet(String sessionId, String betId);

    void placePick(String sessionId, String outcomeId, Integer stake);

    void closeBet(String sessionId);

    void resolve(String sessionId, String outcomeId);

    void annul(String sessionId);

    void reset(String sessionId);
}
