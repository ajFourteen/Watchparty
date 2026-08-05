package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.application.port.out.ClientGateway;
import de.fourteen.watchparty.domain.model.PlayerId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Der {@link ClientGateway} fuer Tests: schreibt nichts, merkt sich alles.
 *
 * Ersetzt den frueheren Aufbau aus gemockter {@code WebSocketSession} und
 * echter {@code ClientSession} — seit der Actor nur noch Sitzungs-IDs und
 * Nachrichtenobjekte kennt, braucht ein Actor-Test weder WebSockets noch
 * JSON. Nebenbei sind die Zusicherungen schaerfer: Statt "irgendetwas wurde
 * gesendet" laesst sich jetzt pruefen, <em>was</em> bei wem ankam.
 *
 * Geschrieben wird vom Raum-Thread, gelesen vom Test-Thread — daher
 * nebenlaeufigkeitsfeste Sammlungen. Gelesen wird nach {@code awaitIdle()}.
 *
 * Oeffentlich, damit auch die JGiven-Stufen im Stufen-Paket
 * (docs/teststrategie.md, Abschnitt 8) sie fuer Port-to-Port-Szenarien
 * verwenden koennen -- derselbe gemeinsame Quellbaum, den die Teststrategie
 * fuer alle Ebenen vorsieht (Abschnitt 1).
 */
public class RecordingClientGateway implements ClientGateway {

    private final Map<String, List<Object>> bySession = new ConcurrentHashMap<>();

    @Override
    public void send(String sessionId, Object message) {
        record(sessionId, message);
    }

    @Override
    public void sendToAll(Collection<String> sessionIds, Object message) {
        for (String sessionId : sessionIds) {
            record(sessionId, message);
        }
    }

    private void record(String sessionId, Object message) {
        bySession.computeIfAbsent(sessionId, id -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(message);
    }

    public List<Object> messagesFor(String sessionId) {
        return new ArrayList<>(bySession.getOrDefault(sessionId, List.of()));
    }

    /** Die Spieler-ID aus dem WELCOME — der Weg, auf dem ein echter Client sie erfaehrt. */
    public PlayerId playerIdOf(String sessionId) {
        return messagesFor(sessionId).stream()
                .filter(Messages.Welcome.class::isInstance)
                .map(Messages.Welcome.class::cast)
                .reduce((first, second) -> second)
                .map(Messages.Welcome::playerId)
                .map(PlayerId::of)
                .orElseThrow(() -> new AssertionError("Kein WELCOME fuer Sitzung " + sessionId));
    }

    /** Der zuletzt an diese Sitzung geschickte Zustand. */
    public Messages.State lastStateFor(String sessionId) {
        return messagesFor(sessionId).stream()
                .filter(Messages.State.class::isInstance)
                .map(Messages.State.class::cast)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("Kein STATE fuer Sitzung " + sessionId));
    }

    /** Alle Fehlermeldungen an diese Sitzung, in Reihenfolge. */
    public List<String> errorsFor(String sessionId) {
        return messagesFor(sessionId).stream()
                .filter(Messages.Error.class::isInstance)
                .map(Messages.Error.class::cast)
                .map(Messages.Error::message)
                .toList();
    }
}
