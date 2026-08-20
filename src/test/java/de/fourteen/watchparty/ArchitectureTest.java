package de.fourteen.watchparty;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.teststrategy.AnhangA;
import org.junit.jupiter.api.Tag;
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
import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
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
@Tag("arch")
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
            .adapter("http", "de.fourteen.watchparty.adapter.in.http..")
            .adapter("file", "de.fourteen.watchparty.adapter.out.file..")
            .adapter("time", "de.fourteen.watchparty.adapter.out.time..")
            .adapter("db", "de.fourteen.watchparty.adapter.out.db..")
            .adapter("mail", "de.fourteen.watchparty.adapter.out.mail..")
            .adapter("feed", "de.fourteen.watchparty.adapter.out.feed..")
            .adapter("ratelimit", "de.fourteen.watchparty.adapter.out.ratelimit..")
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
                    "de.fourteen.watchparty.domain.model.league",
                    "de.fourteen.watchparty.domain.service",
                    "de.fourteen.watchparty.domain.service.league",
                    "de.fourteen.watchparty.application..",
                    "de.fourteen.watchparty.adapter..",
                    "de.fourteen.watchparty.config")
            .should(tragenGenauDenErwartetenRing())
            .because("die Ring-Annotation muss zum Paket passen (ADR-027)");

    /**
     * Die beiden Spielmodi teilen sich die Anwendung und sonst nichts
     * (CLAUDE.md, {@code docs/features/005-tippspiel-liga.md}): Kein
     * Ligacode fasst {@code Room}, {@code Player} oder einen anderen
     * Live-Wetten-Typ an, und umgekehrt fasst kein Live-Wetten-Typ einen
     * Ligatyp an. Ein Zugriff waere hier keine Wiederverwendung, sondern das
     * Datenrennen, gegen das Invariante 1 gebaut ist, sobald die Liga auf
     * Request-Threads laeuft statt auf dem Raum-Thread.
     *
     * Ausgenutzt wird dieselbe Unterscheidung wie bei
     * {@link #jederDomaenentypTraegtEinenBaustein}: {@code resideInAPackage}
     * ohne {@code ..} trifft nur das genannte Paket, nie sein
     * {@code .league}-Unterpaket — beide Richtungen brauchen deshalb keine
     * explizite Ausschlussregel.
     */
    @ArchTest
    static final ArchRule ligaUndRaumcodeKennenEinanderNicht = noClasses()
            .that().resideInAnyPackage(
                    "de.fourteen.watchparty.domain.model.league..",
                    "de.fourteen.watchparty.domain.service.league..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "de.fourteen.watchparty.domain.model",
                    "de.fourteen.watchparty.domain.service")
            .because("die Liga kennt keinen Raumcode-Typ (CLAUDE.md, Trennung der Spielmodi)");

    @ArchTest
    static final ArchRule raumcodeKenntKeineLiga = noClasses()
            .that().resideInAnyPackage(
                    "de.fourteen.watchparty.domain.model",
                    "de.fourteen.watchparty.domain.service")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "de.fourteen.watchparty.domain.model.league..",
                    "de.fourteen.watchparty.domain.service.league..")
            .because("der Raumcode kennt keinen Ligatyp (CLAUDE.md, Trennung der Spielmodi)");

    /**
     * Dieselbe Trennung, jetzt auf dem Anwendungsring: Seit
     * {@code application.league} entsteht (Stufe 2 von Feature 005) gilt
     * dieselbe Zusage wie fuer die Domaene — sonst waere ein Zugriff eines
     * Ligakommandos auf {@code RoomActor} oder umgekehrt ein Datenrennen, das
     * kein Test zufaellig findet (die Liga laeuft auf Request-Threads statt
     * auf dem Raum-Thread, CLAUDE.md, "Was mit den harten Invarianten
     * passiert").
     *
     * Eine bewusste, benannte Ausnahme (ADR-037): {@code Scheduler} ist eine
     * reine Zeitplanungs-Abstraktion ohne jeden Live-Wetten-Begriff — der
     * Nachfuehr-Job des Tippspiels nutzt denselben Port wie Auto-Close bei
     * den Live-Wetten, statt einen zweiten, gleichwertigen Port nachzubauen.
     * Alles andere aus {@code application}/{@code application.port.*} bleibt
     * gesperrt.
     */
    @ArchTest
    static final ArchRule ligaUndAnwendungskernKennenEinanderNicht = noClasses()
            .that().resideInAnyPackage("de.fourteen.watchparty.application.league..")
            .should().dependOnClassesThat(resideInAnyPackage(
                    "de.fourteen.watchparty.application",
                    "de.fourteen.watchparty.application.message..",
                    "de.fourteen.watchparty.application.port.in..",
                    "de.fourteen.watchparty.application.port.out..")
                    .and(DescribedPredicate.not(istDerGeteilteSchedulerPort())))
            .because("die Liga kennt keinen Anwendungskern-Typ der Live-Wetten (CLAUDE.md, Trennung der Spielmodi), ausser dem geteilten Scheduler-Port (ADR-037)");

    @ArchTest
    static final ArchRule anwendungskernKenntKeineLiga = noClasses()
            .that().resideInAnyPackage(
                    "de.fourteen.watchparty.application",
                    "de.fourteen.watchparty.application.message..",
                    "de.fourteen.watchparty.application.port.in..",
                    "de.fourteen.watchparty.application.port.out..")
            .should().dependOnClassesThat().resideInAnyPackage("de.fourteen.watchparty.application.league..")
            .because("der Anwendungskern der Live-Wetten kennt keinen Ligatyp (CLAUDE.md, Trennung der Spielmodi)");

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
     *
     * Deckt nur den Import ab, nicht das Schluesselwort selbst -- dafuer
     * {@link #domaeneOhneSynchronisierteMethoden} und
     * {@link #domaeneOhneVolatileFelder}, seit dem Prozess-Audit vom
     * 2026-08-20 ergaenzt: Der Javadoc hier nannte {@code synchronized}
     * schon immer, geprueft hat es bis dahin niemand.
     */
    @ArchTest
    static final ArchRule domaeneOhneNebenlaeufigkeit = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("java.util.concurrent..")
            .because("Invariante 1: der Raum-Thread ist die Synchronisierung, nicht die Datenstruktur");

    /**
     * Ergaenzt {@link #domaeneOhneNebenlaeufigkeit} um das Schluesselwort
     * selbst: {@code synchronized} ist kein Import und faellt durch eine
     * reine Abhaengigkeitsregel hindurch.
     */
    @ArchTest
    static final ArchRule domaeneOhneSynchronisierteMethoden = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().haveModifier(JavaModifier.SYNCHRONIZED)
            .because("Invariante 1: der Raum-Thread ist die Synchronisierung, kein synchronized im Kern");

    /**
     * Dasselbe fuer Felder: {@code volatile} waere ein zweiter, stillerer Weg,
     * Nebenlaeufigkeit in die Domaene zu tragen, ohne dass
     * {@link #domaeneOhneNebenlaeufigkeit} es saehe.
     */
    @ArchTest
    static final ArchRule domaeneOhneVolatileFelder = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().haveModifier(JavaModifier.VOLATILE)
            .because("Invariante 1: der Raum-Thread ist die Synchronisierung, kein volatile im Kern");

    /**
     * Der Anwendungsring blockiert nicht.
     *
     * Invariante 2 sagt, dass der Raum-Thread nie wartet: Er berechnet Zustand
     * und Nachrichten, das Schreiben auf Sockets laeuft ueber die Ausgangs-Queue
     * der {@code ClientSession}, das Schreiben des Snapshots ueber den eigenen
     * Thread in {@code SnapshotStore}. Ein eingeschlafenes Handy darf das Spiel
     * nicht anhalten.
     *
     * Anders als Invariante 1, die {@link #domaeneOhneNebenlaeufigkeit} in der
     * Domaene abdeckt, war Invariante 2 bislang strukturell ungeschuetzt: Der
     * {@code RoomActor} liegt im Anwendungsring, und genau dort -- nicht in der
     * Domaene -- kann sie gebrochen werden. Ein Pauschalverbot auf
     * {@code java.util.concurrent} scheidet hier aus, weil der Actor seinen
     * {@code ExecutorService} braucht; verboten sind deshalb gezielt die
     * blockierenden Aufrufe, nicht die Werkzeuge.
     *
     * Die eine Ausnahme ist {@code RoomActor.awaitIdle()} -- ein als solcher
     * dokumentierter Testzugang, der den *aufrufenden* Thread blockiert, nicht
     * den Raum-Thread. Dass ihn kein Produktivcode aufruft, sichert
     * {@link #awaitIdleNurAusTestcode} von der anderen Seite.
     */
    @ArchTest
    static final ArchRule anwendungsringBlockiertNicht = noClasses()
            .that().resideInAPackage("..application..")
            .should().callMethodWhere(blockierenderAufrufAusserhalbVonAwaitIdle())
            .because("Invariante 2: der Raum-Thread berechnet, er wartet nicht");

    /**
     * {@code RoomActor.awaitIdle()} wird von keinem Produktivcode aufgerufen.
     *
     * Die Methode ist {@code public} und blockiert den aufrufenden Thread, bis
     * der Raum-Thread leer ist -- noetig, damit Port-to-Port-Szenarien nicht
     * race-behaftet sind (die JGiven-Stufen liegen in einem anderen Paket).
     * Genau diese Sichtbarkeit macht sie aber auch aus einem WebSocket-Handler
     * oder einem Controller erreichbar, und dort wuerde sie einen Request-Thread
     * auf den Raum-Thread warten lassen -- Invariante 2 von aussen gebrochen.
     *
     * Testcode ist ueber {@code ImportOption.DoNotIncludeTests} gar nicht erst
     * Teil der geprueften Klassen; diese Regel bindet deshalb ausschliesslich
     * den Produktivcode, ohne den Testzugang selbst einzuschraenken.
     */
    @ArchTest
    static final ArchRule awaitIdleNurAusTestcode = noClasses()
            .should().callMethodWhere(zielIst("de.fourteen.watchparty.application.RoomActor", "awaitIdle"))
            .because("awaitIdle blockiert den Aufrufer und ist ein Testzugang, kein Produktivweg (Invariante 2)");

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
            .that().resideInAnyPackage(
                    "de.fourteen.watchparty.domain.model",
                    "de.fourteen.watchparty.domain.model.league")
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

    // --- Kritikalitaet (docs/teststrategie.md, Abschnitt 6.2, ADR-030) --------

    /**
     * Das Kritikalitaets-Paket ist ein reiner Marker ohne Laufzeitverhalten,
     * wie JSpecify (ADR-026) und jMolecules (ADR-027): nur die
     * {@code Criticality}-Annotation und ihr verschachtelter {@code Level}-Typ,
     * sonst nichts.
     */
    @ArchTest
    static final ArchRule kritikalitaetsPaketEnthaeltNurAnnotationen = classes()
            .that().resideInAPackage("de.fourteen.watchparty.criticality")
            .and().doNotHaveSimpleName("package-info")
            .should(einAnnotationstypOderEinVerschachtelterEnumSein())
            .because("das Kritikalitaets-Paket traegt keine Logik (Abschnitt 6.2)");

    /**
     * Eine erfundene oder verschriebene Anforderungs-ID in {@code @Criticality}
     * waere eine Begruendung, die niemand nachschlagen kann -- deshalb muss
     * jede genannte ID in Anhang A von {@code anforderungen.md} existieren
     * (Abschnitt 6.2, analog zu Abschnitt 5.2 fuer {@code @Anforderung}).
     */
    @ArchTest
    static final ArchRule jedeKritikalitaetsAnforderungExistiertInAnhangA = classes()
            .that().areAnnotatedWith(Criticality.class)
            .should(nurAnforderungsIdsAusAnhangATragen())
            .because("eine @Criticality-Anforderungs-ID ohne Beleg in Anhang A ist eine Begruendung ins Leere (Abschnitt 6.2)");

    // Frueher stand hier ein Test, der die @Criticality(HIGH)-Klassen gegen
    // eine handgepflegte Kopie von pitest.targetClasses abglich. Diese Kopie
    // gibt es nicht mehr: build.gradle.kts leitet targetClasses inzwischen per
    // Reflection aus der Annotation selbst ab. Damit ist der Test gegenstands-
    // los geworden -- er koennte die Liste nur noch ein drittes Mal aufschreiben
    // und genau die zweite Wahrheit wiederherstellen, die die Ableitung
    // beseitigt hat. Dass die abgeleitete Menge nicht still leer laufen kann,
    // sichert der Build selbst ab (GradleException bei leerer Zielmenge).

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
     * {@code Scheduler} selbst oder ein verschachtelter Typ darin (etwa
     * {@code Scheduler.ScheduledTask}, im Bytecode {@code Scheduler$ScheduledTask})
     * — die eine ADR-037-Ausnahme von der Anwendungsring-Trennung.
     */
    private static DescribedPredicate<JavaClass> istDerGeteilteSchedulerPort() {
        String scheduler = "de.fourteen.watchparty.application.port.out.Scheduler";
        return DescribedPredicate.describe(
                "der geteilte Scheduler-Port oder ein verschachtelter Typ darin",
                javaClass -> javaClass.getName().equals(scheduler) || javaClass.getName().startsWith(scheduler + "$"));
    }

    /**
     * {@code Criticality} selbst ist eine Annotation, ihr verschachtelter
     * {@code Level}-Typ ({@code Criticality$Level} im Bytecode, aber im
     * selben Paket) ein Enum -- beides ist im Kritikalitaets-Paket erlaubt,
     * alles andere waere Logik, die dort nicht hingehoert.
     */
    private static com.tngtech.archunit.lang.ArchCondition<JavaClass> einAnnotationstypOderEinVerschachtelterEnumSein() {
        return new com.tngtech.archunit.lang.ArchCondition<>("ein Annotationstyp oder ein verschachtelter Enum-Typ sein") {
            @Override
            public void check(JavaClass javaClass, com.tngtech.archunit.lang.ConditionEvents events) {
                if (!javaClass.isAnnotation() && !javaClass.isEnum()) {
                    events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(
                            javaClass, javaClass.getName() + " ist weder Annotation noch Enum"));
                }
            }
        };
    }

    /**
     * Liest {@code @Criticality(requirements = {...})} per Reflection zurueck
     * und prueft jede ID gegen {@link AnhangA#alleRegeln()}. Reflection statt
     * ArchUnit-Annotation-API, weil ArchUnit Array-Attribute nur als
     * {@code Object} liefert -- der direkte Weg ueber {@code javaClass.reflect()}
     * ist hier lesbarer als das Auspacken ueber die ArchUnit-eigene API.
     */
    private static com.tngtech.archunit.lang.ArchCondition<JavaClass> nurAnforderungsIdsAusAnhangATragen() {
        java.util.Set<String> bekannt = AnhangA.alleRegeln().keySet();
        return new com.tngtech.archunit.lang.ArchCondition<>("nur Anforderungs-IDs aus Anhang A tragen") {
            @Override
            public void check(JavaClass javaClass, com.tngtech.archunit.lang.ConditionEvents events) {
                Criticality annotation = javaClass.reflect().getAnnotation(Criticality.class);
                for (String id : annotation.requirements()) {
                    if (!bekannt.contains(id)) {
                        events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(javaClass,
                                javaClass.getName() + " nennt unbekannte Anforderungs-ID '" + id + "'"));
                    }
                }
            }
        };
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

    /**
     * Die blockierenden Aufrufe, die Invariante 2 brechen wuerden -- als Paare
     * aus Zieltyp und Methodenname. Aufgezaehlt statt ueber den Paketnamen
     * erschlagen, weil der Anwendungsring {@code java.util.concurrent}
     * durchaus benutzen darf: Der {@code RoomActor} lebt von seinem
     * {@code ExecutorService}. Verboten ist das Warten, nicht das Werkzeug.
     *
     * {@code RoomActor.awaitIdle()} ist ausgenommen: Der Aufruf blockiert dort
     * bewusst den aufrufenden Testthread, nicht den Raum-Thread (siehe
     * {@link #awaitIdleNurAusTestcode}).
     */
    private static DescribedPredicate<JavaMethodCall> blockierenderAufrufAusserhalbVonAwaitIdle() {
        record BlockierenderAufruf(String typ, String methode) {
        }
        Set<BlockierenderAufruf> verboten = Set.of(
                new BlockierenderAufruf("java.util.concurrent.Future", "get"),
                new BlockierenderAufruf("java.util.concurrent.CompletableFuture", "get"),
                new BlockierenderAufruf("java.util.concurrent.CompletableFuture", "join"),
                new BlockierenderAufruf("java.util.concurrent.CountDownLatch", "await"),
                new BlockierenderAufruf("java.util.concurrent.CyclicBarrier", "await"),
                new BlockierenderAufruf("java.util.concurrent.Semaphore", "acquire"),
                new BlockierenderAufruf("java.util.concurrent.BlockingQueue", "take"),
                new BlockierenderAufruf("java.util.concurrent.BlockingQueue", "put"),
                new BlockierenderAufruf("java.util.concurrent.ExecutorService", "awaitTermination"),
                new BlockierenderAufruf("java.util.concurrent.ExecutorService", "invokeAll"),
                new BlockierenderAufruf("java.util.concurrent.ExecutorService", "invokeAny"),
                new BlockierenderAufruf("java.lang.Thread", "sleep"),
                new BlockierenderAufruf("java.lang.Thread", "join"),
                new BlockierenderAufruf("java.lang.Object", "wait"));

        String awaitIdle = "de.fourteen.watchparty.application.RoomActor.awaitIdle";

        return DescribedPredicate.describe(
                "ein blockierender Aufruf ausserhalb von RoomActor.awaitIdle",
                aufruf -> {
                    String herkunft = aufruf.getOrigin().getOwner().getName() + "." + aufruf.getOrigin().getName();
                    if (herkunft.equals(awaitIdle)) {
                        return false;
                    }
                    return verboten.contains(new BlockierenderAufruf(
                            aufruf.getTargetOwner().getName(), aufruf.getName()));
                });
    }

    /** Ein Methodenaufruf auf genau diesen Typ mit genau diesem Namen. */
    private static DescribedPredicate<JavaMethodCall> zielIst(String typ, String methode) {
        return DescribedPredicate.describe(
                typ + "." + methode + " aufrufen",
                aufruf -> aufruf.getTargetOwner().getName().equals(typ) && aufruf.getName().equals(methode));
    }
}
