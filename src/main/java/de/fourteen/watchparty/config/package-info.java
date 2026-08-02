/**
 * Kompositionswurzel: saemtliche Spring-Beans.
 *
 * @NullMarked (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
package de.fourteen.watchparty.config;

import org.jspecify.annotations.NullMarked;
