/**
 * Feed-Adapter des Tippspiels: ESPN hinter dem Port {@code ScheduleFeed}
 * (ADR-037). Das Mapping von ESPNs Antwortformat auf die eigenen Typen des
 * Domänenmodells steckt vollständig hier und nirgends sonst.
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: Feed-Adapter (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty.adapter.out.feed;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
