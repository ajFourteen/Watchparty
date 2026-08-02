package de.fourteen.watchparty.application.port.out;

import java.util.Collection;

/**
 * Ausgangs-Port zu den Clients. Der Anwendungsring entscheidet <em>wer was</em>
 * bekommt; wie daraus ein WebSocket-Frame wird, weiss allein der Adapter.
 *
 * Der Port spricht Sitzungs-IDs, keine Verbindungsobjekte. Nur deshalb kommt
 * der {@code RoomActor} ohne jede Kenntnis von WebSockets aus — vorher hielt
 * er {@code ClientSession}-Objekte selbst.
 *
 * Beide Methoden muessen sofort zurueckkehren (Invariante 2): Sie werden vom
 * Raum-Thread aufgerufen, und ein eingeschlafenes Handy darf das Spiel nicht
 * anhalten. Das eigentliche Schreiben gehoert in den Adapter.
 */
public interface ClientGateway {

    /** An genau eine Sitzung — Fehlermeldungen, WELCOME, YOUR_PICK. */
    void send(String sessionId, Object message);

    /**
     * An alle genannten Sitzungen. Der Empfaengerkreis kommt vom Raum-Thread
     * und nicht aus dem Adapter, damit eine gerade erst verbundene, aber noch
     * nicht eingereihte Sitzung nichts bekommt (Invariante 1). Der Adapter
     * darf die Nachricht dafuer einmal serialisieren statt je Empfaenger.
     */
    void sendToAll(Collection<String> sessionIds, Object message);
}
