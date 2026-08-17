package de.fourteen.watchparty.application.league.port.out;

import java.time.Instant;

/**
 * Begrenzt, wie oft ein Schluessel in einem Zeitfenster zulaessig ist
 * (Kriterium 4: je E-Mail-Adresse und je Absender-IP, mit unterschiedlichem
 * Praefix auf demselben Port statt zweier Ports — die Regel ist fuer beide
 * dieselbe, nur der Schluessel unterscheidet sich).
 */
public interface RateLimiter {

    /** Zaehlt einen Versuch fuer {@code key} zum Zeitpunkt {@code now} und meldet, ob er noch erlaubt ist. */
    boolean allow(String key, Instant now);
}
