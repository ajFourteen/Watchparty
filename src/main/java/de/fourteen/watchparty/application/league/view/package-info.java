/**
 * Projektionen fuer die Antworten des Tippspiels — dieselbe Rolle wie
 * {@code RoomView} fuer die Live-Wetten.
 *
 * Eigener Zweig neben {@code de.fourteen.watchparty.application} (ADR-034):
 * Ein Ligatyp importiert keinen Live-Wetten-Typ und umgekehrt
 * ({@code ArchitectureTest}).
 *
 * {@code @ApplicationServiceRing} (ADR-027): Derselbe Anwendungsring wie
 * {@code RoomView} (ADR-024) — die Trennung verlaeuft nach Spielmodus,
 * nicht nach Ring.
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern
 * nicht ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das
 * beim Kompilieren.
 */
@NullMarked
@ApplicationServiceRing
package de.fourteen.watchparty.application.league.view;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
