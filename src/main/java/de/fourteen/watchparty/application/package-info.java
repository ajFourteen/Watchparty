/**
 * Der Anwendungsring: Orchestrierung, Projektion, Eingangs-Port (ADR-024).
 *
 * {@code @ApplicationServiceRing} (ADR-027): Kennt Domaene und Ports, keine Adapter (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@ApplicationServiceRing
package de.fourteen.watchparty.application;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
