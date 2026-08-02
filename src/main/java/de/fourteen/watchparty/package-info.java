/**
 * Wurzelpaket: der Startpunkt der Anwendung.
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: der Bootstrap (WatchpartyApplication) (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
