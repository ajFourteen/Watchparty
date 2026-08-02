package de.fourteen.watchparty;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
import org.jmolecules.architecture.onion.classical.DomainModelRing;
import org.jmolecules.architecture.onion.classical.DomainServiceRing;
import org.jmolecules.architecture.onion.classical.InfrastructureRing;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Service;
import org.jmolecules.ddd.annotation.ValueObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Haelt zwei zusammengehoerige Regelwerke nach: die Ringstruktur aus ADR-024
 * und die DDD-Bausteine aus ADR-025/ADR-027.
 *
 * Ohne diese Regeln waeren beide eine Absichtserklaerung — ein einziger
 * bequemer Import oder eine vergessene Annotation genuegt, um sie zu
 * durchloechern, und niemand merkt es. Der Umbau selbst hat mehrere solche
 * Verstoesse ans Licht gebracht (unter anderem hielt der {@code RoomActor}
 * {@code ClientSession}-Objekte, und {@code Room} rief einen Domain Service
 * direkt aus dem Modell auf).
 *
 * Geprueft wird auf dem Bytecode: Damit zaehlen auch Rueckgabetypen,
 * Feldtypen und Annotationen, nicht nur die Importzeile.
 */
@AnalyzeClasses(packages = "de.fourteen.watchparty", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // --- Ringe (ADR-024) ------------------------------------------------------

    /**
     * Die Ringe ueber die Paketnamen. {@code config} ist bewusst kein Ring,
     * sondern die Kompositionswurzel und darf deshalb alles kennen — sie
     * taucht hier nicht auf.
     *
     * Ergaenzt {@link #ringeTragenIhreAnnotation} von der anderen Seite: Diese
     * Regel faellt auf, wenn eine Klasse ins falsche Paket rutscht, auch ohne
     * fehlerhafte Annotation; jene faellt auf, wenn die Annotation fehlt oder
     * falsch ist, auch wenn das Paket zufaellig stimmt.
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
     * Dieselbe Ringstruktur, diesmal ueber die jMolecules-Annotationen
     * (ADR-027) statt ueber Paketnamen: {@code @DomainModelRing},
     * {@code @DomainServiceRing}, {@code @ApplicationServiceRing},
     * {@code @InfrastructureRing} auf den {@code package-info.java}.
     *
     * Absichtlich selbst geschrieben statt mit
     * {@code JMoleculesArchitectureRules.ensureOnionClassical()}: Die
     * vorgefertigten jMolecules-Regeln setzen eine ArchUnit-Version voraus,
     * die mit dieser Codebasis nicht zusammenpasst (siehe build.gradle.kts).
     * Die Annotationen selbst sind davon unberuehrt -- nur die Bibliothek,
     * die sie vorgefertigt pruefen wollte, ist es nicht.
     */
    @ArchTest
    static final ArchRule ringeTragenIhreAnnotation = classes()
            .that().haveSimpleName("package-info")
            .and().resideInAnyPackage(
                    "de.fourteen.watchparty.domain.model",
                    "de.fourteen.watchparty.domain.service",
                    "de.fourteen.watchparty.application..",
                    "de.fourteen.watchparty.adapter..",
                    "de.fourteen.watchparty.config")
            .should(tragenGenauDenErwartetenRing())
            .because("die Ring-Annotation muss zum Paket passen (ADR-027)");

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

    // --- DDD-Bausteine (ADR-025/ADR-027) ---------------------------------------

    /**
     * Jeder oeffentliche Domaenentyp traegt genau einen Baustein-Stereotyp.
     *
     * Ohne diese Regel bemerkt niemand, wenn ein neuer Domaenentyp ohne
     * Stereotyp dazukommt — die anderen Regeln hier pruefen nur, was
     * annotiert *ist*, nicht, ob *alles* annotiert ist.
     *
     * Zwei bewusste Ausnahmen: {@code RoomSnapshot} (und seine verschachtelten
     * Records) ist explizit KEIN Baustein des Modells, sondern das
     * Dateiformat fuer die Platte (ADR-023) — im selben Paket, weil
     * {@code Room} es spricht, aber semantisch aussen vor. {@code Bets} ist
     * ein statischer Katalog (ADR-017), kein Objekt mit Identitaet oder
     * Wert — dafuer gibt es in DDD keinen passenden Baustein.
     */
    @ArchTest
    static final ArchRule jederDomaenentypTraegtEinenBaustein = classes()
            .that().resideInAPackage("de.fourteen.watchparty.domain.model")
            .and().arePublic()
            .and(nichtVerschachtelt())
            .and().doNotHaveSimpleName("RoomSnapshot")
            .and().doNotHaveSimpleName("Bets")
            .and().doNotHaveSimpleName("package-info")
            .should().beAnnotatedWith(AggregateRoot.class)
            .orShould().beAnnotatedWith(Entity.class)
            .orShould().beAnnotatedWith(ValueObject.class)
            .because("jeder Domaenentyp ist ein benannter DDD-Baustein, keiner rutscht unbemerkt durch (ADR-027)");

    /**
     * Ein {@code @Service} ist eine reine Funktion (ADR-025): kein
     * Instanzzustand, der zwischen zwei Aufrufen ueberleben und die
     * Zustandslosigkeit unterlaufen koennte. Fuer {@code Settlement} steht das
     * im Javadoc; diese Regel haelt es zusaetzlich nach.
     */
    @ArchTest
    static final ArchRule domainServicesSindZustandslos = classes()
            .that().areAnnotatedWith(Service.class)
            .should().haveOnlyFinalFields()
            .andShould(keineInstanzfelderHaben())
            .because("ein Domain Service rechnet, er erinnert sich nicht (ADR-025)");

    /**
     * Entities aendern sich nur ueber benannte Uebergaenge, die das Aggregat
     * anbietet (etwa {@code Round.setPhase}, aufgerufen von {@code Room}),
     * nie ueber einen oeffentlichen Setter von aussen. Genau das ist die
     * Aggregatgrenze aus ADR-025 — hier als Regel statt nur als
     * paket-privater Modifier, den man leicht uebersieht.
     */
    @ArchTest
    static final ArchRule keineOeffentlichenSetterAufEntities = methods()
            .that().haveNameMatching("set[A-Z].*")
            .and().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
            .should().notBePublic()
            .because("Zustandsaenderungen laufen ueber benannte Uebergaenge, nicht ueber Setter (ADR-025)")
            .allowEmptyShould(true);

    /**
     * Dieselbe Regel fuer den Aggregate Root: {@code Room} bietet
     * {@code closeCurrentRound}, {@code annulCurrentRound},
     * {@code resolveCurrentRound}, {@code addPick} an -- keinen Setter.
     *
     * {@code allowEmptyShould(true)}: Zurzeit hat {@code Room} ueberhaupt
     * keine {@code set*}-Methode, die Regel prueft also "leer bleibt leer".
     * Ohne dieses Flag waere die Rule selbst ein Fehlschlag, sobald sie nichts
     * zum Beanstanden findet -- und "nichts zu beanstanden" ist hier der
     * Normalfall, keine Ausnahme.
     */
    @ArchTest
    static final ArchRule keineOeffentlichenSetterAufDemAggregateRoot = methods()
            .that().haveNameMatching("set[A-Z].*")
            .and().areDeclaredInClassesThat().areAnnotatedWith(AggregateRoot.class)
            .should().notBePublic()
            .because("Zustandsaenderungen laufen ueber benannte Uebergaenge, nicht ueber Setter (ADR-025)")
            .allowEmptyShould(true);

    /**
     * Fuer jede {@code package-info}-Klasse: welche der vier Ring-Annotationen
     * traegt sie tatsaechlich, welche haette sie nach ADR-024 tragen muessen
     * (Praefix-Match auf den Paketnamen -- der laengste Treffer gewinnt, damit
     * {@code adapter.in.ws} nicht faelschlich {@code adapter} selbst matcht,
     * gaebe es dort einen package-info)? Beides muss genau uebereinstimmen.
     */
    private static com.tngtech.archunit.lang.ArchCondition<JavaClass> tragenGenauDenErwartetenRing() {
        record RingePaket(String praefix, Class<? extends java.lang.annotation.Annotation> ring) {
        }
        List<RingePaket> zuordnung = List.of(
                new RingePaket("de.fourteen.watchparty.domain.model", DomainModelRing.class),
                new RingePaket("de.fourteen.watchparty.domain.service", DomainServiceRing.class),
                new RingePaket("de.fourteen.watchparty.application", ApplicationServiceRing.class),
                new RingePaket("de.fourteen.watchparty.adapter", InfrastructureRing.class),
                new RingePaket("de.fourteen.watchparty.config", InfrastructureRing.class));
        List<Class<? extends java.lang.annotation.Annotation>> alleRinge = List.of(
                DomainModelRing.class, DomainServiceRing.class, ApplicationServiceRing.class, InfrastructureRing.class);

        return new com.tngtech.archunit.lang.ArchCondition<>("genau den nach ADR-024 vorgesehenen Ring tragen") {
            @Override
            public void check(JavaClass javaClass, com.tngtech.archunit.lang.ConditionEvents events) {
                String paket = javaClass.getPackageName();
                Class<? extends java.lang.annotation.Annotation> erwartet = zuordnung.stream()
                        .filter(z -> paket.equals(z.praefix()) || paket.startsWith(z.praefix() + "."))
                        .max(java.util.Comparator.comparingInt(z -> z.praefix().length()))
                        .map(RingePaket::ring)
                        .orElse(null);
                if (erwartet == null) {
                    events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(
                            javaClass, "Paket " + paket + " ist keinem Ring aus ADR-024 zugeordnet"));
                    return;
                }
                for (Class<? extends java.lang.annotation.Annotation> ring : alleRinge) {
                    boolean traegt = javaClass.isAnnotatedWith(ring);
                    boolean sollTragen = ring.equals(erwartet);
                    if (traegt != sollTragen) {
                        events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(javaClass,
                                javaClass.getName() + (sollTragen
                                        ? " fehlt @" + ring.getSimpleName() + " (erwartet fuer " + paket + ")"
                                        : " traegt unerwartet @" + ring.getSimpleName())));
                    }
                }
            }
        };
    }

    private static DescribedPredicate<JavaClass> resideIn(String paket) {
        return DescribedPredicate.describe(
                "in " + paket,
                javaClass -> javaClass.getName().startsWith(paket));
    }

    private static DescribedPredicate<JavaClass> alwaysTrue() {
        return DescribedPredicate.describe("beliebig", javaClass -> true);
    }

    /**
     * Verschachtelte Typen (etwa {@code RoomSnapshot.PlayerSnapshot}) zaehlen
     * nicht als eigener Domaenentyp — ihre binaere Bezeichnung enthaelt ein
     * {@code $}, das reicht als Unterscheidung ohne eine ArchUnit-Methode zu
     * erraten, die es in dieser Version vielleicht gar nicht gibt.
     */
    private static DescribedPredicate<JavaClass> nichtVerschachtelt() {
        return DescribedPredicate.describe(
                "nicht verschachtelt",
                javaClass -> !javaClass.getName().contains("$"));
    }

    /**
     * {@code haveOnlyFinalFields()} allein reicht nicht: Ein {@code final}
     * Feld, das ein veraenderliches Objekt haelt (etwa eine {@code Map}),
     * waere immer noch Zustand. Fuer {@code Settlement} ist die einfachste,
     * ueberpruefbare Aussage "keine Instanzfelder ueberhaupt" — per
     * Reflection statt ArchUnit-Predicate, weil "hat keine Felder" kein
     * eigener ArchUnit-Baustein ist.
     */
    private static com.tngtech.archunit.lang.ArchCondition<JavaClass> keineInstanzfelderHaben() {
        return new com.tngtech.archunit.lang.ArchCondition<>("keine Instanzfelder haben") {
            @Override
            public void check(JavaClass javaClass, com.tngtech.archunit.lang.ConditionEvents events) {
                for (Field field : javaClass.reflect().getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                        events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(
                                javaClass, javaClass.getName() + " hat das Instanzfeld " + field.getName()));
                    }
                }
            }
        };
    }
}
