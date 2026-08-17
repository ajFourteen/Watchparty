package de.fourteen.watchparty.adapter.out.ratelimit;

import de.fourteen.watchparty.application.league.port.out.RateLimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Gleitendes Zeitfenster im Arbeitsspeicher (Kriterium 4): je Schluessel die
 * Zeitpunkte der letzten Versuche, alles ausserhalb des Fensters faellt beim
 * naechsten Zugriff weg. Reicht fuer eine Handvoll gleichzeitiger Watchpartys
 * (ADR-005, ADR-018) und braucht keine Datenbank, waechst also nicht mit der
 * Saison mit.
 *
 * {@code synchronized} statt einer Concurrent-Collection: Anders als die
 * Domaene (Invariante 1) kennt der Anwendungs-/Adapterring des Tippspiels
 * durchaus Nebenlaeufigkeits-Werkzeuge — mehrere Request-Threads koennen
 * gleichzeitig ankommen, und bei dieser Groessenordnung ist ein einzelnes
 * Lock einfacher richtig zu halten als Feingranulareres.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Deque<Instant>> attempts = new HashMap<>();

    public InMemoryRateLimiter(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    @Override
    public synchronized boolean allow(String key, Instant now) {
        Deque<Instant> history = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        Instant cutoff = now.minus(window);
        while (!history.isEmpty() && history.peekFirst().isBefore(cutoff)) {
            history.pollFirst();
        }
        if (history.size() >= maxAttempts) {
            return false;
        }
        history.addLast(now);
        return true;
    }
}
