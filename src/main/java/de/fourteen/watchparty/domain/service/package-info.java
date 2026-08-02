/**
 * Domain Services: reine Funktionen ueber dem Domaenenmodell (ADR-025).
 *
 * {@code @DomainServiceRing} (ADR-027): Kennt nur das Domaenenmodell, sonst nichts (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@DomainServiceRing
package de.fourteen.watchparty.domain.service;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.DomainServiceRing;
