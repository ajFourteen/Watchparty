package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.RateLimiter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Handgeschriebenes Test Double (ADR-025, kein Mockito): erlaubt per
 * Voreinstellung alles, ausser ein Schluessel wurde ausdruecklich gesperrt —
 * deterministisch statt auf echte Zeitfenster angewiesen.
 */
public class FakeRateLimiter implements RateLimiter {

    private final Set<String> gesperrt = new HashSet<>();

    public void sperre(String key) {
        gesperrt.add(key);
    }

    @Override
    public boolean allow(String key, Instant now) {
        return !gesperrt.contains(key);
    }
}
