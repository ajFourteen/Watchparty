package de.fourteen.watchparty.criticality;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Die Einstufung eines Features nach Eintrittswahrscheinlichkeit mal
 * Schadensausmaß (docs/teststrategie.md, Abschnitt 6). Steht am
 * Produktivcode, weil sie dort gilt -- die Begründung steht im
 * Feature-Dokument (Abschnitt 9.1).
 *
 * Anwendbar auf Typ <b>und</b> Methode: Eine Klasse kann Features
 * unterschiedlicher Kritikalität bedienen, die Einstufung gehört an das
 * Feature, nicht pauschal an die Datei.
 *
 * {@code requirements} verweist auf IDs aus Anhang A von
 * {@code anforderungen.md} -- eine ArchUnit-Regel prüft, dass jede genannte
 * ID dort tatsächlich existiert (Abschnitt 6.2).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Criticality {

    /** LOW/MEDIUM/HIGH als verschachtelter Typ, damit dieses Paket ausschließlich Annotationen enthält. */
    enum Level {
        LOW, MEDIUM, HIGH
    }

    Level level();

    String[] requirements();
}
