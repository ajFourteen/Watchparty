/**
 * Zeit-Adapter: verzoegerte Ausfuehrung.
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: Zeit-Adapter (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty.adapter.out.time;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
