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
 * auseinanderlaufen kann (Abschnitt 1).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Tag("unit")
@IsTag(name = "Domaene")
public @interface UnitTest {
}
