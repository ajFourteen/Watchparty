package de.fourteen.watchparty;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Haelt die Ringregel aus ADR-024 nach: Abhaengigkeiten zeigen nur nach innen.
 *
 * Ohne diese Regeln waere die Struktur eine Absichtserklaerung — ein einziger
 * bequemer Import genuegt, um sie zu durchloechern, und niemand merkt es. Der
 * Umbau selbst hat drei solche Verstoesse ans Licht gebracht (unter anderem
 * hielt der {@code RoomActor} {@code ClientSession}-Objekte).
 *
 * Geprueft wird auf dem Bytecode: Damit zaehlen auch Rueckgabetypen,
 * Feldtypen und Annotationen, nicht nur die Importzeile.
 */
@AnalyzeClasses(packages = "de.fourteen.watchparty", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * Die Ringe selbst. {@code config} ist bewusst kein Ring, sondern die
     * Kompositionswurzel und darf deshalb alles kennen — sie taucht hier
     * nicht auf.
     */
    @ArchTest
    static final ArchRule ringeZeigenNachInnen = onionArchitecture()
            .domainModels("de.fourteen.watchparty.domain.model..")
            .domainServices("de.fourteen.watchparty.domain.service..")
            .applicationServices("de.fourteen.watchparty.application..")
            .adapter("ws", "de.fourteen.watchparty.adapter.in.ws..")
            .adapter("file", "de.fourteen.watchparty.adapter.out.file..")
            .adapter("time", "de.fourteen.watchparty.adapter.out.time..")
            .ignoreDependency(
                    resideIn("de.fourteen.watchparty.config"),
                    alwaysTrue())
            .ignoreDependency(
                    resideIn("de.fourteen.watchparty.WatchpartyApplication"),
                    alwaysTrue());

    /**
     * Der Kern bleibt framework-frei: Spring wird ausschliesslich in
     * {@code config} und in den Adaptern verdrahtet. Sonst waere der
     * {@code RoomActor} ohne Spring-Kontext nicht mehr zu instanziieren — und
     * genau das machen die Actor-Tests.
     */
    @ArchTest
    static final ArchRule kernOhneSpring = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..")
            .because("Domaene und Anwendungsring muessen ohne Framework instanziierbar bleiben (ADR-024)");

    /**
     * Jackson darf ausschliesslich an den Nachrichtentypen vorkommen.
     *
     * Das ist die eine bewusst zugelassene Ausnahme: Die Nachrichtentypen
     * muessen im Anwendungsring liegen, weil {@code RoomView} sie erzeugt,
     * tragen aber {@code @JsonInclude}/{@code @JsonProperty}. Sie in den
     * Adapter zu schieben hiesse, Invariante 4 (verdeckte Tipps) dorthin zu
     * verlegen; sie ueber Mixins zu entkoppeln waere fuer fuenf Records mehr
     * Zeremonie als Gewinn. Annotationen sind Metadaten — serialisiert wird
     * allein im Adapter.
     */
    @ArchTest
    static final ArchRule jacksonNurInDenNachrichtentypen = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .and().resideOutsideOfPackage("..application.message..")
            .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..")
            .because("serialisiert wird im Adapter, nicht im Kern (ADR-024)");

    /**
     * Die Domaene kennt keine Nebenlaeufigkeits-Werkzeuge. Invariante 1 sagt,
     * dass aller Zustand auf dem Raum-Thread liegt; ein {@code synchronized}
     * oder eine Concurrent-Collection im Kern wuerde diese Regel verschleiern
     * statt sie zu stuetzen (CLAUDE.md, ADR-009).
     */
    @ArchTest
    static final ArchRule domaeneOhneNebenlaeufigkeit = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("java.util.concurrent..")
            .because("Invariante 1: der Raum-Thread ist die Synchronisierung, nicht die Datenstruktur");

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass>
            resideIn(String paket) {
        return com.tngtech.archunit.base.DescribedPredicate.describe(
                "in " + paket,
                javaClass -> javaClass.getName().startsWith(paket));
    }

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass>
            alwaysTrue() {
        return com.tngtech.archunit.base.DescribedPredicate.describe("beliebig", javaClass -> true);
    }
}
