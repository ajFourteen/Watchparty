package de.fourteen.watchparty.teststrategy;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.jgiven.Stage;
import org.junit.jupiter.api.Tag;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

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

    /**
     * Eine ID in {@code @Anforderung}, die es in Anhang A nicht gibt, ist ein
     * Fehler (docs/teststrategie.md, Abschnitt 5.2) -- sonst zeigt die
     * Rueckverfolgbarkeit auf eine Regel, die niemand nachschlagen kann.
     */
    @ArchTest
    static final ArchRule jedeAnforderungExistiertInAnhangA = methods()
            .that().areAnnotatedWith(Anforderung.class)
            .should(nurAnforderungsIdsAusAnhangATragen())
            .because("eine @Anforderung-ID ohne Beleg in Anhang A ist eine Begruendung ins Leere (Abschnitt 5.2)");

    private static ArchCondition<JavaMethod> nurAnforderungsIdsAusAnhangATragen() {
        Set<String> bekannt = AnhangA.alleRegeln().keySet();
        return new ArchCondition<>("nur Anforderungs-IDs aus Anhang A tragen") {
            @Override
            public void check(JavaMethod javaMethod, ConditionEvents events) {
                Anforderung annotation = javaMethod.reflect().getAnnotation(Anforderung.class);
                for (String id : annotation.value()) {
                    if (!bekannt.contains(id)) {
                        events.add(SimpleConditionEvent.violated(javaMethod,
                                javaMethod.getFullName() + " nennt unbekannte Anforderungs-ID '" + id + "'"));
                    }
                }
            }
        };
    }
}
