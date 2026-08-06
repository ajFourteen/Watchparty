package de.fourteen.watchparty.teststrategy;

import com.tngtech.jgiven.annotation.IsTag;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2): Regeln, die ueber
 * Zeit, Phasen oder mehrere Beteiligte spannen. Eingang ist
 * {@link de.fourteen.watchparty.application.port.in.RoomCommands}, Ausgang
 * der {@code RecordingClientGateway}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Tag("port")
@net.jqwik.api.Tag("port")
@IsTag(name = "Port-to-Port")
public @interface PortTest {
}
