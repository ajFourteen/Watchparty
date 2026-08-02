/**
 * Datei-Adapter: Snapshot auf Platte (ADR-023).
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: Datei-Adapter (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty.adapter.out.file;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
