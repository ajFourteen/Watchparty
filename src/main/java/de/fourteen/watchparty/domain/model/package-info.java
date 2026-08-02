/**
 * Das Domaenenmodell: Aggregate, Entities, Value Objects (ADR-025).
 *
 * {@code @DomainModelRing} (ADR-027): Der innerste Ring. Kennt keinen anderen Ring (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@DomainModelRing
package de.fourteen.watchparty.domain.model;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.DomainModelRing;
