/**
 * Adapter des Tippspiels fuer das Rate Limit der Anmeldung (Kriterium 4).
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: Rate-Limit-Adapter (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty.adapter.out.ratelimit;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
