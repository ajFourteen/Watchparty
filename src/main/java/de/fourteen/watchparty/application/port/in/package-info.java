/**
 * Eingangs-Port: was von aussen am Raum ausgeloest werden kann.
 *
 * @NullMarked (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
package de.fourteen.watchparty.application.port.in;

import org.jspecify.annotations.NullMarked;
