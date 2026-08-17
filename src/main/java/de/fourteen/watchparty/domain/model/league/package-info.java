/**
 * Das Domaenenmodell des Tippspiels (ADR-034, {@code docs/features/005-tippspiel-liga.md}).
 *
 * Eigener Zweig neben {@code de.fourteen.watchparty.domain.model}, mit
 * Absicht: Ein Tippspiel-Typ importiert keinen Live-Wetten-Typ und
 * umgekehrt ({@code ArchitectureTest}) — die beiden Spielmodi teilen sich
 * die Anwendung und sonst nichts (CLAUDE.md).
 *
 * {@code @DomainModelRing} (ADR-027): Derselbe innerste Ring wie das
 * bestehende Domaenenmodell (ADR-024) — die Trennung verlaeuft nach
 * Spielmodus, nicht nach Ring.
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern
 * nicht ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das
 * beim Kompilieren.
 */
@NullMarked
@DomainModelRing
package de.fourteen.watchparty.domain.model.league;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.DomainModelRing;
