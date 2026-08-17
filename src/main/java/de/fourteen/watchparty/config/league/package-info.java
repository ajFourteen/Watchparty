/**
 * Kompositionswurzel des Tippspiels: Datenbankanbindung.
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: Kompositionswurzel, verdrahtet alle anderen Ringe (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty.config.league;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
