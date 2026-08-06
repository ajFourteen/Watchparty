package de.fourteen.watchparty.teststrategy;

import com.tngtech.jgiven.annotation.IsTag;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API-Ebene (docs/teststrategie.md, Abschnitt 2.4): echter Server, echter
 * WebSocket, echtes JSON. Fuegt keine neue fachliche Abdeckung hinzu, prueft
 * nur Verdrahtung, Leck-Tests am serialisierten JSON und die hier
 * entscheidbaren nicht-funktionalen Anforderungen.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Tag("api")
@net.jqwik.api.Tag("api")
@IsTag(name = "API")
public @interface ApiTest {
}
