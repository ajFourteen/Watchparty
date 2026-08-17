/**
 * Domain Services des Tippspiels: reine Funktionen ueber
 * {@code domain.model.league} (ADR-034/ADR-038).
 *
 * {@code @DomainServiceRing} (ADR-027): Kennt nur das Tippspiel-Domaenenmodell,
 * sonst nichts (ADR-024) — insbesondere keinen Live-Wetten-Typ
 * ({@code ArchitectureTest}).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern
 * nicht ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das
 * beim Kompilieren.
 */
@NullMarked
@DomainServiceRing
package de.fourteen.watchparty.domain.service.league;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.DomainServiceRing;
