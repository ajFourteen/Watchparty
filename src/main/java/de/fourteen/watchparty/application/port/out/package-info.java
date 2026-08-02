/**
 * Ausgangs-Ports: Clients, Persistenz, Zeitplanung.
 *
 * {@code @ApplicationServiceRing} (ADR-027): Teil des Anwendungsrings: die Ausgangs-Ports (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@ApplicationServiceRing
package de.fourteen.watchparty.application.port.out;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
