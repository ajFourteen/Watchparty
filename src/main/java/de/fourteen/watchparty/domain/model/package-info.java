/**
 * Das Domaenenmodell: Aggregate, Entities, Value Objects (ADR-025).
 *
 * @NullMarked (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
package de.fourteen.watchparty.domain.model;

import org.jspecify.annotations.NullMarked;
