package de.fourteen.watchparty.mutationtest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markiert eine Methode oder Klasse, deren Mutanten PIT bewusst nicht zu
 * töten braucht (docs/teststrategie.md, Abschnitt 7.2/10) -- ein
 * äquivalenter Mutant oder ein bewusst nicht abgedeckter Fall. PIT schließt
 * annotierte Elemente über das FANN-Plugin von der Mutation aus
 * (build.gradle.kts, {@code pitest.features}).
 *
 * Der Wert steht doppelt: hier im Code, sichtbar an der Stelle, die er
 * betrifft, und mit Datum in {@code docs/test-ausnahmen.md} -- eine
 * Unterdrückung ohne Eintrag dort ist eine Ausnahme, die niemand
 * nachvollziehen kann.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface AequivalenterMutant {

    /** Kurze Begründung, warum kein Test diesen Mutanten sinnvoll töten kann. */
    String value();
}
