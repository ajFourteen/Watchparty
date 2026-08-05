package de.fourteen.watchparty.teststrategy;

import com.tngtech.jgiven.annotation.IsTag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Zeigt von einem Szenario auf eine Regel-ID aus Anhang A von
 * {@code anforderungen.md} (docs/teststrategie.md, Abschnitt 5.1). Trägt den
 * Report-Tag <em>und</em> die Feature-Abdeckung in einem: Der `abdeckung`-Task
 * bildet die Differenz zwischen den hier verwendeten IDs und den
 * `backend`-markierten Regeln aus Anhang A.
 *
 * {@code explodeArray}: Ein Szenario darf mehrere Regeln belegen
 * ({@code @Anforderung({"7.1", "7.1-a"})}) — jede IDs wird dann ein eigener
 * Tag im Report, statt eines zusammengesetzten.
 *
 * {@code TeststrategyArchitectureTest} prüft, dass jede hier verwendete ID in
 * Anhang A tatsächlich existiert (Abschnitt 5.2).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@IsTag(name = "Anforderung", explodeArray = true)
public @interface Anforderung {
    String[] value();
}
