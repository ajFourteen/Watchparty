/**
 * Eingangs-Port: was von aussen am Raum ausgeloest werden kann.
 *
 * {@code @ApplicationServiceRing} (ADR-027): Teil des Anwendungsrings: der Eingangs-Port (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@ApplicationServiceRing
package de.fourteen.watchparty.application.port.in;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
