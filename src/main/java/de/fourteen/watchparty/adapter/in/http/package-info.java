/**
 * HTTP-Adapter des Tippspiels (ADR-039): REST statt WebSocket, weil beim
 * Tippen nichts in Sekunden geschieht. Anfrage/Antwort statt Push, ein
 * Sitzungscookie statt eines Tokens im Nachrichtenrumpf.
 *
 * {@code @InfrastructureRing} (ADR-027): Aeusserster Ring: HTTP-Adapter (ADR-024).
 *
 * {@code @NullMarked} (ADR-026): Jeder Verweistyp ist nicht-null, sofern nicht
 * ausdruecklich mit {@code @Nullable} versehen. NullAway prueft das beim
 * Kompilieren.
 */
@NullMarked
@InfrastructureRing
package de.fourteen.watchparty.adapter.in.http;

import org.jspecify.annotations.NullMarked;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
