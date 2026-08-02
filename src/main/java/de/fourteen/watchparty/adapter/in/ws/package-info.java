/**
 * WebSocket-Adapter: uebersetzt Frames in Kommandos.
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: WebSocket-Adapter (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty.adapter.in.ws;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
