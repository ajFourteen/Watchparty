package de.fourteen.watchparty.teststrategy;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.jgiven.Stage;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Haelt die strukturelle Grenze der Sprachausnahme aus ADR-030 nach: Eine
 * JGiven-{@code Stage} ist Reporttext in Java-Syntax und darf deshalb
 * deutsche Bezeichner tragen (docs/teststrategie.md, Abschnitt 8) -- aber nur
 * innerhalb des dafuer vorgesehenen Stufen-Pakets. "Deutsche Bezeichner"
 * bleibt damit eine Frage des Pakets, nicht des Augenmasses.
 *
 * Eigene Klasse statt eine weitere Regel in {@link de.fourteen.watchparty.ArchitectureTest}:
 * Jene analysiert bewusst nur Produktivcode ({@code DoNotIncludeTests}), diese
 * Regel betrifft ausschliesslich Testcode.
 */
@Tag("arch")
@AnalyzeClasses(packages = "de.fourteen.watchparty", importOptions = ImportOption.OnlyIncludeTests.class)
class TeststrategyArchitectureTest {

    @ArchTest
    static final ArchRule jedeJGivenStageLiegtImStufenPaket = classes()
            .that().areAssignableTo(Stage.class)
            .and().doNotHaveSimpleName("Stage")
            .should().resideInAPackage("de.fourteen.watchparty.teststrategy.stufen")
            .because("die Sprachausnahme fuer deutsche Bezeichner ist strukturell auf das Stufen-Paket begrenzt (ADR-030)");
}
