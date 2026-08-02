/**
 * Ausgangs-Ports: Clients, Persistenz, Zeitplanung.
 *
 * @NullMarked (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
package de.fourteen.watchparty.application.port.out;

import org.jspecify.annotations.NullMarked;
