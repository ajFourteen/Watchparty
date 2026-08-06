package de.fourteen.watchparty.teststrategy;

import com.tngtech.jgiven.annotation.IsTag;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adapter-Ebene (docs/teststrategie.md, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was der Port ausdruecken kann? Keine neue fachliche
 * Abdeckung, das hat die Ebene darunter schon entschieden.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Tag("adapter")
@net.jqwik.api.Tag("adapter")
@IsTag(name = "Adapter")
public @interface AdapterTest {
}
