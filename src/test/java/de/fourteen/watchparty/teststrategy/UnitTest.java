package de.fourteen.watchparty.teststrategy;

import com.tngtech.jgiven.annotation.IsTag;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Domaene-Ebene (docs/teststrategie.md, Abschnitt 2.1): Regeln, die aus dem
 * Zustand eines Objekts oder aus einer reinen Funktion entscheidbar sind.
 *
 * Traegt den JUnit-Tag und den JGiven-Report-Tag zusammen, damit beides nicht
 * auseinanderlaufen kann (Abschnitt 1). Dazu den jqwik-eigenen
 * {@code net.jqwik.api.Tag}: jqwiks Engine kennt nur ihren eigenen
 * Tag-Typ, nicht den von JUnit Jupiter -- ohne ihn wuerden Property-Tests
 * (Abschnitt 4) vom Tag-Filter des `test`-Tasks unsichtbar aussortiert, ohne
 * dass das irgendwo auffiele.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Tag("unit")
@net.jqwik.api.Tag("unit")
@IsTag(name = "Domaene")
public @interface UnitTest {
}
