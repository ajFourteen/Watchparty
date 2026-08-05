/**
 * Enthält ausschließlich die {@link de.fourteen.watchparty.criticality.Criticality}-Annotation
 * und ihren verschachtelten {@code Level}-Typ -- reiner Marker ohne
 * Laufzeitverhalten, wie JSpecify (ADR-026) und jMolecules (ADR-027).
 *
 * Bewusst <b>kein</b> Ring aus ADR-024: {@code onionArchitecture()} schränkt
 * ein, wer auf einen Ring zugreifen darf; ein ringloses Annotationspaket ist
 * kein Ring und wird deshalb in {@code ArchitectureTest} explizit von der
 * Ringprüfung ausgenommen (docs/teststrategie.md, Abschnitt 6.2).
 */
package de.fourteen.watchparty.criticality;
