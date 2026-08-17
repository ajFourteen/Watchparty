/**
 * Eingangs-Ports des Tippspiels: was von aussen ausgeloest werden kann.
 *
 * Eigener Zweig neben {@code de.fourteen.watchparty.application.port}
 * (ADR-034): Ein Ligatyp importiert keinen Live-Wetten-Typ und umgekehrt
 * ({@code ArchitectureTest}) — die beiden Spielmodi teilen sich die
 * Anwendung und sonst nichts (CLAUDE.md).
 *
 * {@code @ApplicationServiceRing} (ADR-027): Derselbe Anwendungsring wie die
 * bestehenden Ports (ADR-024) — die Trennung verlaeuft nach Spielmodus,
 * nicht nach Ring.
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern
 * nicht ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das
 * beim Kompilieren.
 */
@NullMarked
@ApplicationServiceRing
package de.fourteen.watchparty.application.league.port.in;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
