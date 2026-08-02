/**
 * Der Anwendungsring: Orchestrierung, Projektion, Eingangs-Port (ADR-024).
 *
 * @NullMarked (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
package de.fourteen.watchparty.application;

import org.jspecify.annotations.NullMarked;
