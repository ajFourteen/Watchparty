/**
 * Orchestrierung des Tippspiels (ADR-034): {@link
 * de.fourteen.watchparty.application.league.LoginService} setzt {@link
 * de.fourteen.watchparty.application.league.port.in.LoginCommands} um, im
 * selben Sinn wie {@code RoomActor} fuer {@code RoomCommands} — nur auf
 * Request-Threads statt auf dem Raum-Thread (CLAUDE.md, "Was mit den harten
 * Invarianten passiert").
 *
 * Eigener Zweig neben {@code de.fourteen.watchparty.application}: Ein
 * Ligatyp importiert keinen Live-Wetten-Typ und umgekehrt
 * ({@code ArchitectureTest}) — die beiden Spielmodi teilen sich die
 * Anwendung und sonst nichts (CLAUDE.md).
 *
 * {@code @ApplicationServiceRing} (ADR-027): Derselbe Anwendungsring wie
 * {@code RoomActor} (ADR-024) — die Trennung verlaeuft nach Spielmodus,
 * nicht nach Ring.
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern
 * nicht ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das
 * beim Kompilieren.
 */
@NullMarked
@ApplicationServiceRing
package de.fourteen.watchparty.application.league;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
