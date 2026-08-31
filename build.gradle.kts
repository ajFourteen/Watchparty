import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.nullaway.nullaway
import com.tngtech.jgiven.gradle.JGivenTaskExtension
import com.tngtech.jgiven.gradle.JGivenReportTask
import java.io.File
import java.net.URLClassLoader
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    // Setzen JSpecify durch: ein @Nullable an der falschen Stelle ist ein
    // Compile-Fehler, keine Doku (ADR-026).
    id("net.ltgt.errorprone") version "5.1.0"
    id("net.ltgt.nullaway") version "3.1.0"
    // Erzeugt den JGiven-HTML-Report aus den JSON-Ergebnissen, die
    // jgiven-junit5 beim Testlauf schreibt (docs/teststrategie.md, Abschnitt 8).
    id("com.tngtech.jgiven.gradle-plugin") version "2.0.3"
    // Zeilen-/Zweigabdeckung je Ebene, erhoben statt als Zielgroesse
    // (docs/teststrategie.md, Abschnitt 7.3) -- und die Grundlage fuer die
    // Ebenen-Disjunktheit aus Abschnitt 7.4.
    jacoco
    // Mutationstests auf den HIGH-Klassen, Abschnitt 7.2.
    id("info.solidsoft.pitest") version "1.19.0"
    // Fuehrt Major-Versionsupdates als Rezept aus statt von Hand (ADR-042).
    // Haengt bewusst an KEINER Stelle an `check` -- rewriteRun/rewriteDryRun
    // sind Werkzeuge fuer den Dependabot-Lauf, keine Pruefung.
    id("org.openrewrite.rewrite") version "7.39.0"
}

group = "de.fourteen"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Mailversand des Tippspiels (ADR-036): Jakarta Mail ueber Spring, aber
    // ohne Spring Boots eigene MailSenderAutoConfiguration -- JavaMailSenderImpl
    // wird in config/league von Hand gebaut, wie DataSource fuer die Datenbank.
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Persistenz des Tippspiels (ADR-035): Standard-JDBC-Weg von Spring Boot,
    // kein Spring Data -- Repository-Adapter sprechen JdbcTemplate direkt,
    // damit die Zugriffe so explizit bleiben wie der Rest dieser Codebasis.
    // DataSource-/Flyway-Autoconfiguration wird in WatchpartyApplication
    // ausgeschaltet; das Wiring uebernimmt config/league von Hand (Stil wie
    // RoomConfig/SnapshotConfig).
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Die Annotationen selbst (ADR-026): @NullMarked, @Nullable. Reine
    // Deklarationen ohne Laufzeitverhalten -- die Durchsetzung macht NullAway.
    implementation("org.jspecify:jspecify:1.0.1")

    // DDD-Stereotypen (@AggregateRoot, @Entity, @ValueObject, @Identity,
    // @Service) und die Onion-Ring-Annotationen (ADR-027). Reine Marker ohne
    // Laufzeitverhalten, wie JSpecify -- die Durchsetzung macht ArchUnit ueber
    // jmolecules-archunit.
    implementation("org.jmolecules:jmolecules-ddd:2.0.1")
    implementation("org.jmolecules:jmolecules-onion-architecture:2.0.1")

    // Ohne Mockito: Test Doubles werden von Hand geschrieben (ADR-025). Der
    // Ausschluss macht daraus eine Regel statt einer Absprache -- ein
    // versehentliches mock(...) kompiliert gar nicht erst.
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }

    // Haelt die Ringregel aus ADR-024 als Test fest.
    //
    // jmolecules-archunit (die vorgefertigten Regeln aus dem jMolecules-
    // Projekt selbst) wurde bewusst NICHT eingebunden: Die neueste Version
    // (1.6.0, Stand 2022) ist gegen ArchUnit 0.23.1 gebaut.
    // JMoleculesArchitectureRules wirft mit dieser Version einen
    // NoSuchMethodError (Architectures.layeredArchitecture()-Signatur
    // geaendert), JMoleculesDddRules einen AbstractMethodError -- beides erst
    // beim Testlauf, nicht beim Kompilieren. Ein Downgrade auf ArchUnit
    // 0.23.1 selbst wurde probiert und verworfen: Die Klassenerkennung
    // (@AnalyzeClasses) fand in dieser Umgebung ueberhaupt keine Klassen mehr.
    // Die Stereotyp-Annotationen (org.jmolecules:jmolecules-ddd/
    // -onion-architecture) sind reine Marker ohne diese Abhaengigkeit;
    // geprueft werden sie unten mit denselben, stabilen ArchUnit-Bausteinen,
    // die der Rest dieser Klasse schon benutzt.
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")

    // Report- und Szenariowerkzeug der Teststrategie (docs/teststrategie.md).
    // jgiven-junit5 bringt die JUnit5-Erweiterung fuer ScenarioTest mit;
    // jqwik die Property-Tests (Abschnitt 4).
    testImplementation("com.tngtech.jgiven:jgiven-junit5:2.0.3")
    testImplementation("net.jqwik:jqwik:1.10.1")

    // Adapter-Tests gegen echtes Postgres statt einer Attrappe (Abschnitt
    // 2.3): derselbe SQL-Dialekt wie die Produktion, kein H2-Drift. Versionen
    // stammen aus Spring Boots eigenem Dependency-Management.
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    // Ab Gradle 9 liegt der Launcher nicht mehr automatisch auf dem
    // Test-Classpath; ohne ihn startet der Test-Executor gar nicht erst.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.14.0")

    // Die Rezeptsammlungen fuer ADR-042. Sie liegen auf einer eigenen
    // Konfiguration (`rewrite`) und damit weder auf dem Compile- noch auf dem
    // Test-Classpath -- ein Rezept kann nichts kompilieren, was sonst nicht
    // kompiliert.
    //
    // Bewusst nur diese drei: Sie beschreiben, was ein Versionssprung
    // *erzwingt*. rewrite-static-analysis waere die vierte naheliegende, ist
    // aber Geschmacksverbesserung -- und die Routine darf laut ihren eigenen
    // Grenzen nichts anfassen, was der Sprung nicht verlangt.
    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.37.0"))
    rewrite("org.openrewrite.recipe:rewrite-spring")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
}

// jgiven-junit5 und jqwik-engine haengen direkt (nicht nur ueber eine
// importierte BOM) hoehere org.junit.platform-Versionen an als Spring Boots
// eigenes Dependency-Management fuer die uebrigen JUnit-Module durchsetzt
// (5.12.2 / 1.12.2) -- fuer die meisten Module gewinnt Spring Boots
// Verwaltung den Versionskonflikt, aber junit-platform-launcher verwaltet
// Spring Boot ueberhaupt nicht selbst, dort gewinnt unwidersprochen die
// hoehere Anfrage. Ergebnis: launcher (1.13.x) und -engine/-commons (1.12.x)
// laufen auseinander -- ein NoSuchMethodError auf
// NamespacedHierarchicalStore$CloseAction beim Testlauf, gefunden beim
// Einbinden von PIT (Phase 4). eachDependency greift vor der
// Konfliktaufloesung selbst und erzwingt denselben Stand ueberall.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.junit.jupiter") {
            useVersion("5.12.2")
        }
        if (requested.group == "org.junit.platform") {
            useVersion("1.12.2")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// --- Ebenen als Gradle-Tasks (docs/teststrategie.md, Abschnitt 1) ----------
//
// Getrennt wird ueber JUnit-Tags, nicht ueber eigene Source Sets: Die
// handgeschriebenen Test Doubles bleiben in einem gemeinsamen Quellbaum
// (src/test/java), erreichbar von jeder Ebene. `test` ist bewusst der
// schnelle Lauf (unit, port); `adapterTest` und `apiTest` kommen extra dazu,
// weil sie Spring bzw. einen echten Socket brauchen (Phase 1 der
// Teststrategie-Umsetzung). `arch` laeuft NICHT hier mit, sondern in einem
// eigenen Task (`archTest`, unten) -- der Grund ist kein Stilentscheid,
// sondern ein Fund: archunit-junit5-engine:1.4.1 implementiert getTags() auf
// keinem seiner TestDescriptor-Knoten. Jeder JUnit-Platform-TagFilter,
// gleich welcher Tags, sortiert deshalb ALLE ArchUnit-Tests aus -- egal ob
// per Paket- oder per Klassenauswahl entdeckt (nachgestellt mit einem
// eigenstaendigen JUnit-Platform-Launcher-Aufruf, siehe
// docs/teststrategie-umsetzung.md). `ArchitectureTest` lief dadurch bislang
// bei keinem `test`/`check`-Lauf tatsaechlich mit, obwohl Build und Report
// unauffaellig gruen blieben -- ein durch Tag-Filterung stillschweigend
// leeres Ergebnis sieht identisch aus wie ein bestandener Lauf.
tasks.named<Test>("test") {
    useJUnitPlatform {
        includeTags("unit", "port")
    }
}

val adapterTest = tasks.register<Test>("adapterTest") {
    description = "Adapter-Ebene: kann der Adapter alles uebertragen, was der Port ausdrueckt? (Abschnitt 2.3)"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("adapter")
    }
    shouldRunAfter(tasks.test)
}

val apiTest = tasks.register<Test>("apiTest") {
    description = "API-Ebene: echter Server, echter Socket, echtes JSON (Abschnitt 2.4)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("api")
    }
    shouldRunAfter(adapterTest)
}

// Struktur (`arch`, Abschnitt 2.5): ueber die JUnit-Platform-Engine
// ausgewaehlt (`includeEngines("archunit")`), nicht ueber einen Tag -- siehe
// Begruendung oben. Ohne Tag-Filter funktioniert die Engine wie dokumentiert
// (mit einem eigenstaendigen Launcher-Aufruf verifiziert); die Auswahl nach
// Engine statt nach Layer ist inhaltlich sogar treffender, weil
// Architekturregeln nicht zu einer einzelnen Ebene gehoeren, sondern fuer
// alle gelten.
val archTest = tasks.register<Test>("archTest") {
    description = "Struktur: haelt der Bau die Invarianten? (Abschnitt 2.5)"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeEngines("archunit")
    }
    shouldRunAfter(tasks.test)
}

tasks.named("check") {
    dependsOn(adapterTest, apiTest, archTest)
}

// --- Ein JGiven-Report ueber alle Ebenen hinweg -----------------------------
//
// Das JGiven-Gradle-Plugin verdrahtet jeden Test-Task automatisch mit einem
// eigenen Ergebnisordner (build/<Taskname>/jgiven-results), haengt aber nur
// fuer den vorgefundenen Standard-Task `test` einen Report-Task ein -- die
// erst spaeter im Skript definierten `adapterTest`/`apiTest` kommen dabei zu
// spaet. Alle drei schreiben deshalb in denselben Ordner (Dateinamen sind je
// Testklasse eindeutig, Kollisionen also ausgeschlossen), und der
// vorhandene `jgivenTestReport`-Task liest von dort -- ein einziger Report
// mit allen Szenarien aus allen Ebenen (docs/teststrategie.md, Abschnitt 8).
//
// `archTest` bewusst aussen vor: ArchUnit-Regeln sind keine JGiven-Szenarien
// und schreiben nie in diesen Ordner -- eine Abhaengigkeit von `archTest`
// waere hier ein Validierungsfehler ohne Gegenwert (Gradle bemaengelt sonst
// eine "implicit dependency" auf ein Verzeichnis, das der Task nie befuellt).
val jgivenResultsDir = layout.buildDirectory.dir("jgiven-results/alle-ebenen")

listOf(tasks.test, adapterTest, apiTest).forEach { test ->
    test.configure {
        extensions.configure<JGivenTaskExtension> {
            resultsDir.set(jgivenResultsDir)
        }
    }
}

tasks.named<JGivenReportTask>("jgivenTestReport") {
    dependsOn(tasks.test, adapterTest, apiTest)
    results.set(jgivenResultsDir)
}

tasks.named("check") {
    dependsOn("jgivenTestReport")
}

// --- Zeilen-/Zweigabdeckung je Ebene (docs/teststrategie.md, Abschnitt 7.3) -
//
// Erhoben und als Artefakt abgelegt, aber ohne Prozentschranke: Als Zielgroesse
// erzeugt Abdeckung Tests, die fuer die Zahl geschrieben werden. Als Suchhilfe
// ist sie nuetzlich -- deshalb ein Report je Test-Task (test/adapterTest/
// apiTest), nicht nur einer fuer alle zusammen. Dieselben drei Ausfuehrungsdaten
// sind auch die Grundlage der Ebenen-Disjunktheit weiter unten.
jacoco {
    toolVersion = "0.8.15"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    executionData(tasks.test.get())
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoAdapterTestReport = tasks.register<JacocoReport>("jacocoAdapterTestReport") {
    dependsOn(adapterTest)
    executionData(adapterTest.get())
    sourceSets(sourceSets.main.get())
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoApiTestReport = tasks.register<JacocoReport>("jacocoApiTestReport") {
    dependsOn(apiTest)
    executionData(apiTest.get())
    sourceSets(sourceSets.main.get())
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named("check") {
    dependsOn("jacocoTestReport", jacocoAdapterTestReport, jacocoApiTestReport)
}

// --- Ebenen-Disjunktheit (docs/teststrategie.md, Abschnitt 7.4) ------------
//
// Deckt ein Adapter- oder API-Test eine Domaenenzeile ab, die kein
// Port-to-Port- und kein Domaenentest abdeckt, ist das eine Luecke weiter
// innen -- nicht ein Verdienst der aeusseren Ebene. Lief laut Strategie von
// Anfang an automatisiert, deshalb hier ein Gate und kein reiner Bericht
// (anders als die JaCoCo-Zahlen selbst, die bewusst ohne Schranke bleiben).
tasks.register("ebenenDisjunktheit") {
    group = "verification"
    description = "Prueft, dass Adapter/API keine Domaenenzeile abdecken, die unit/port nicht selbst erreichen."
    dependsOn("jacocoTestReport", jacocoAdapterTestReport, jacocoApiTestReport)

    val innerReport = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
    val adapterReport = layout.buildDirectory.file("reports/jacoco/jacocoAdapterTestReport/jacocoAdapterTestReport.xml")
    val apiReport = layout.buildDirectory.file("reports/jacoco/jacocoApiTestReport/jacocoApiTestReport.xml")
    val berichtsDatei = layout.buildDirectory.file("reports/ebenen-disjunktheit.txt")

    inputs.file(innerReport)
    inputs.file(adapterReport)
    inputs.file(apiReport)
    outputs.file(berichtsDatei)

    doLast {
        val domaenenPraefix = "de/fourteen/watchparty/domain"

        // Das JaCoCo-Schema referenziert eine externe DTD (report.dtd), die
        // dem Report nicht beiliegt -- ohne diese Feature-Abschaltung sucht
        // der Parser sie relativ zur XML-Datei und scheitert dort.
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

        fun gedeckteDomaenenzeilen(reportDatei: java.io.File): Set<String> {
            val dokument = docBuilderFactory.newDocumentBuilder().parse(reportDatei)
            val zeilen = mutableSetOf<String>()
            val pakete = dokument.getElementsByTagName("package")
            for (p in 0 until pakete.length) {
                val paket = pakete.item(p) as Element
                val paketname = paket.getAttribute("name")
                if (!paketname.startsWith(domaenenPraefix)) continue
                val quelldateien = paket.getElementsByTagName("sourcefile")
                for (s in 0 until quelldateien.length) {
                    val quelldatei = quelldateien.item(s) as Element
                    val dateiname = quelldatei.getAttribute("name")
                    val zeilenknoten = quelldatei.getElementsByTagName("line")
                    for (z in 0 until zeilenknoten.length) {
                        val zeile = zeilenknoten.item(z) as Element
                        if (zeile.getAttribute("ci").toInt() > 0) {
                            zeilen += "$paketname/$dateiname:${zeile.getAttribute("nr")}"
                        }
                    }
                }
            }
            return zeilen
        }

        val innen = gedeckteDomaenenzeilen(innerReport.get().asFile)
        val aussen = gedeckteDomaenenzeilen(adapterReport.get().asFile) + gedeckteDomaenenzeilen(apiReport.get().asFile)
        val luecken = (aussen - innen).sorted()

        val bericht = buildString {
            appendLine("Ebenen-Disjunktheit: ${luecken.size} Domaenenzeile(n) nur von Adapter/API gedeckt.")
            luecken.forEach { appendLine("  - $it") }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        if (luecken.isNotEmpty()) {
            throw GradleException(
                "Ebenen-Disjunktheit verletzt: ${luecken.size} Domaenenzeile(n) nur von Adapter/API " +
                    "gedeckt, nicht von unit/port -- Luecke weiter innen: ${luecken.joinToString(", ")}")
        }
    }
}

tasks.named("check") {
    dependsOn("ebenenDisjunktheit")
}

// --- Mutationstests auf den HIGH-Klassen (docs/teststrategie.md, Abschnitt 7.2) --
//
// Nur die als HIGH eingestuften Klassen (Abschnitt 6.4) und nur die Tags
// unit und port als Testmenge -- kein Spring, kein Socket, kein
// Reportschreiben, sonst wird der Lauf unbenutzbar (PIT wiederholt Tests je
// Mutant). includedGroups reicht bis zur JUnit5Configuration des
// PIT-Plugins durch und filtert dort per Tag, genau wie unser eigener
// test-Task.
//
// Welche Klassen HIGH sind, steht seit ADR-030/ADR-031 als @Criticality am
// Code selbst. Diese Liste wird deshalb daraus *abgeleitet* und nicht daneben
// gefuehrt: Eine zweite, handgepflegte Aufzaehlung waere genau die zweite
// Wahrheit, die still veraltet -- eine neu als HIGH eingestufte Klasse bliebe
// unmutiert, und der Mutation Score bliebe gruen, obwohl er sie nie angefasst
// hat. Dasselbe Verfahren wie beim `abdeckung`-Task weiter oben: Reflection
// ueber die kompilierten Klassen, keine Textsuche im Quelltext.
val highKritikalitaetsKlassen = provider {
    val klassenVerzeichnisse = sourceSets.main.get().output.classesDirs
    val urls = sourceSets.main.get().runtimeClasspath.files.map { it.toURI().toURL() }.toTypedArray()
    val classLoader = URLClassLoader(urls, javaClass.classLoader)
    val criticalityKlasse = classLoader.loadClass("de.fourteen.watchparty.criticality.Criticality")
    @Suppress("UNCHECKED_CAST")
    val annotationKlasse = criticalityKlasse as Class<out Annotation>
    val levelMethode = criticalityKlasse.getMethod("level")

    val gefunden = sortedSetOf<String>()
    klassenVerzeichnisse.forEach { wurzelVerzeichnis ->
        wurzelVerzeichnis.walkTopDown()
            .filter { it.isFile && it.extension == "class" && !it.name.contains("$") }
            .forEach { classFile ->
                val klassenname = classFile.relativeTo(wurzelVerzeichnis).path
                    .removeSuffix(".class").replace(File.separatorChar, '.')
                val klasse = try {
                    classLoader.loadClass(klassenname)
                } catch (e: Throwable) {
                    return@forEach
                }
                val annotation = klasse.getAnnotation(annotationKlasse) ?: return@forEach
                if (levelMethode.invoke(annotation).toString() == "HIGH") {
                    gefunden += klassenname
                }
            }
    }

    // Eine leere Zielmenge waere der gefaehrlichste Ausgang: PIT liefe durch,
    // mutierte nichts und meldete Erfolg -- ununterscheidbar von einem echten
    // Lauf. Genau dieser Fehlermodus hat schon einmal die ArchUnit-Regeln
    // stillgelegt (siehe Kommentar am archTest-Task).
    if (gefunden.isEmpty()) {
        throw GradleException(
            "Keine @Criticality(HIGH)-Klasse gefunden -- die Mutationstests haetten kein Ziel. " +
                "Entweder ist die Einstufung verschwunden oder das Einsammeln ist kaputt.")
    }
    logger.lifecycle("Mutationstests auf ${gefunden.size} HIGH-Klasse(n): ${gefunden.joinToString(", ")}")
    gefunden.toSet()
}

pitest {
    targetClasses.set(highKritikalitaetsKlassen)
    targetTests.set(setOf("de.fourteen.watchparty.*"))
    includedGroups.set(setOf("unit", "port"))
    testPlugin.set("junit5")
    junit5PluginVersion.set("1.2.3")
    mutationThreshold.set(99)
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
    // Ausnahmenregister (docs/test-ausnahmen.md, Abschnitt 7.2/10): das
    // eingebaute FANN-Plugin (an, per Default schon auf Generated/
    // DoNotMutate/CoverageIgnore) schliesst annotierte Klassen/Methoden von
    // der Mutation aus -- die eigene Annotation ergaenzt die drei
    // Standardnamen, statt sie zu ersetzen (ein konfigurierter Wert
    // ueberschreibt sonst die eingebaute Liste komplett).
    features.set(listOf(
            "+FANN(annotation[Generated]annotation[DoNotMutate]annotation[CoverageIgnore]annotation[AequivalenterMutant])"))
}

// Der Schwellwert oben (99 %) galt bislang nur fuer den, der `gradle pitest`
// von Hand aufrief: `check` hing nicht daran, und kein Workflow rief ihn auf.
// Damit war die Metrik zwar konfiguriert, aber in keinem CI-Lauf wirksam --
// dieselbe Sorte stiller Ausfall wie eine leere Zielmenge. Abschnitt 10 der
// Teststrategie budgetiert ausdruecklich zehn Minuten "einschliesslich
// Mutationstests"; genau so ist es gemeint.
tasks.named("check") {
    dependsOn("pitest")
}

// --- Feature-Abdeckung (docs/teststrategie.md, Abschnitt 5.2) --------------
//
// Liest Anhang A aus derselben Datei wie die Anforderungen selbst -- eine
// zweite Datei waere eine zweite Wahrheit, die still veraltet (Abschnitt
// 5.2). Bewusst ein nachgelagerter Task und kein Test: Gezaehlt wird, was
// *gruen gelaufen* ist, nicht was annotiert ist -- sonst belegt ein
// fehlschlagendes Szenario weiterhin seine Regel.
//
// Geht bewusst NICHT ueber die JGiven-JSON-Ergebnisse: Property-Tests
// (Abschnitt 4) tragen zwar @Anforderung, erscheinen aber nie als
// JGiven-Szenario im Report (das ist so gewollt, Abschnitt 2.1) -- ueber die
// JSON-Ergebnisse waeren sie fuer diesen Task unsichtbar, obwohl sie gruen
// gelaufen und korrekt verknuepft sind. Stattdessen: @Anforderung-Methoden
// per Reflection aus den kompilierten Testklassen einsammeln und mit den
// JUnit-XML-Berichten aller drei Ebenen abgleichen -- das erfasst JGiven-
// Szenarien und jqwik-Properties einheitlich ueber denselben Mechanismus.
//
// Scharf gestellt seit Phase 3 (docs/teststrategie-umsetzung.md): 60 von 60
// backend-Regeln sind belegt, `check` haengt jetzt von diesem Task ab. Der
// Uebergang war terminiert, nicht optional -- vorher lief der Task nur als
// Bericht, sonst waere der Build ab Tag eins rot gewesen.
tasks.register("abdeckung") {
    group = "verification"
    description = "Vergleicht die backend-Regeln aus Anhang A mit den gruen gelaufenen @Anforderung-Testmethoden."
    dependsOn(tasks.test, adapterTest, apiTest)

    val anforderungenDatei = layout.projectDirectory.file("docs/anforderungen.md")
    val testKlassenVerzeichnisse = sourceSets.test.get().output.classesDirs
    val testKlassenpfad = sourceSets.test.get().runtimeClasspath
    val testErgebnisVerzeichnisse = listOf(
        layout.buildDirectory.dir("test-results/test"),
        layout.buildDirectory.dir("test-results/adapterTest"),
        layout.buildDirectory.dir("test-results/apiTest"))
    val berichtsDatei = layout.buildDirectory.file("reports/abdeckung.txt")

    inputs.file(anforderungenDatei)
    inputs.files(testKlassenVerzeichnisse)
    testErgebnisVerzeichnisse.forEach { inputs.dir(it) }
    outputs.file(berichtsDatei)

    doLast {
        val zeilePattern = Regex(
            """^\|\s*([0-9]+(?:\.[0-9]+)?(?:-[a-z])?)\s*\|.*\|\s*(backend|frontend|organisatorisch|beobachtung|gestaltung)\s*\|\s*$""")
        var inAnhangA = false
        val backendRegeln = linkedSetOf<String>()
        anforderungenDatei.asFile.forEachLine { zeile ->
            if (zeile.startsWith("## Anhang A")) {
                inAnhangA = true
            } else if (inAnhangA) {
                val treffer = zeilePattern.matchEntire(zeile)
                if (treffer != null && treffer.groupValues[2] == "backend") {
                    backendRegeln += treffer.groupValues[1]
                }
            }
        }

        // Gruen gelaufene Testmethoden aus den JUnit-XML-Berichten aller drei
        // Ebenen, als "vollqualifizierterKlassenname#methodenname". Parametrisierte
        // Namen wie "schreibenUndLadenErgibtDenselbenStand(Path)" werden auf den
        // reinen Methodennamen gekuerzt, damit sie zur Reflection-Signatur passen.
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        val gruen = mutableSetOf<String>()
        for (verzeichnisProvider in testErgebnisVerzeichnisse) {
            val verzeichnis = verzeichnisProvider.get().asFile
            verzeichnis.listFiles { f -> f.extension == "xml" }?.forEach { xmlDatei ->
                val dokument = docBuilderFactory.newDocumentBuilder().parse(xmlDatei)
                val testcases = dokument.getElementsByTagName("testcase")
                for (i in 0 until testcases.length) {
                    val testcase = testcases.item(i) as Element
                    val hatFehlschlag = testcase.getElementsByTagName("failure").length > 0 ||
                            testcase.getElementsByTagName("error").length > 0
                    if (hatFehlschlag) continue
                    val klasse = testcase.getAttribute("classname")
                    val methode = testcase.getAttribute("name").substringBefore("(")
                    gruen += "$klasse#$methode"
                }
            }
        }

        // @Anforderung-annotierte Methoden per Reflection einsammeln -- derselbe
        // Weg fuer JGiven-Szenarien (@Test @Anforderung(...)) und jqwik-Properties
        // (@Property @Anforderung(...)), keine Fallunterscheidung noetig.
        val klassenpfadUrls = testKlassenpfad.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(klassenpfadUrls, javaClass.classLoader)
        val anforderungKlasse = classLoader.loadClass("de.fourteen.watchparty.teststrategy.Anforderung")
        @Suppress("UNCHECKED_CAST")
        val anforderungAnnotationKlasse = anforderungKlasse as Class<out Annotation>
        val valueMethode = anforderungKlasse.getMethod("value")

        val belegteRegeln = mutableSetOf<String>()
        testKlassenVerzeichnisse.forEach { wurzelVerzeichnis ->
            wurzelVerzeichnis.walkTopDown()
                .filter { it.isFile && it.extension == "class" && !it.name.contains("$") }
                .forEach { classFile ->
                    val klassenname = classFile.relativeTo(wurzelVerzeichnis).path
                        .removeSuffix(".class").replace(File.separatorChar, '.')
                    val klasse = try {
                        classLoader.loadClass(klassenname)
                    } catch (e: Throwable) {
                        return@forEach
                    }
                    for (methode in klasse.declaredMethods) {
                        val annotation = methode.getAnnotation(anforderungAnnotationKlasse) ?: continue
                        if ("$klassenname#${methode.name}" !in gruen) continue
                        @Suppress("UNCHECKED_CAST")
                        val ids = valueMethode.invoke(annotation) as Array<String>
                        belegteRegeln += ids
                    }
                }
        }

        val fehlend = (backendRegeln - belegteRegeln).sorted()
        val bericht = buildString {
            appendLine("Feature-Abdeckung: ${backendRegeln.size - fehlend.size} von ${backendRegeln.size} backend-Regeln belegt.")
            if (fehlend.isEmpty()) {
                appendLine("Keine offenen backend-Regeln.")
            } else {
                appendLine("Offen (${fehlend.size}):")
                fehlend.forEach { appendLine("  - $it") }
            }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        if (fehlend.isNotEmpty()) {
            throw GradleException("Feature-Abdeckung unvollstaendig: ${fehlend.size} offene backend-Regel(n) -- ${fehlend.joinToString(", ")}")
        }
    }
}

tasks.named("check") {
    dependsOn("abdeckung")
}

// --- CLAUDE.md gegen den Baum halten --------------------------------------
//
// Der Aufbau-Abschnitt in CLAUDE.md beschreibt rund 120 Zeilen Struktur und
// wird bei jeder Sitzung gelesen. Geprueft hat ihn nichts -- er war damit die
// Stelle im Projekt, die am sichersten als Erste veraltet, und zwar
// unbemerkt: Eine geloeschte Klasse steht dort einfach weiter.
//
// Gebunden wird nur, was sich eindeutig binden laesst. Die Prosa daneben
// ("Der Kern. Kein Spring, kein Jackson") bleibt handgeschrieben; sie ist der
// Grund, warum dieser Abschnitt ueberhaupt existiert, und liesse sich nicht
// erzeugen, ohne ihren Wert zu verlieren.
tasks.register("aufbaudoku") {
    group = "verification"
    description = "Prueft, dass CLAUDE.md keine verschwundenen Dateien nennt und keinen Domaenentyp verschweigt."

    val claudeDatei = layout.projectDirectory.file("CLAUDE.md")
    val modellVerzeichnis = layout.projectDirectory.dir("src/main/java/de/fourteen/watchparty/domain/model")
    val projektWurzel = layout.projectDirectory.asFile
    val berichtsDatei = layout.buildDirectory.file("reports/aufbaudoku.txt")

    inputs.file(claudeDatei)
    inputs.dir(modellVerzeichnis)
    outputs.file(berichtsDatei)

    doLast {
        val text = claudeDatei.asFile.readText()

        // Zwei Nennungen sind absichtlich ohne Datei dahinter. Sie stehen hier
        // und nicht als Sonderfall im Text, damit jede weitere Ausnahme im Diff
        // auffaellt statt sich stillschweigend anzusammeln.
        val bewussteNennungenOhneDatei = setOf(
            // Der Platzhalter der Feature-Vorlage, kein Dateiname.
            "NNN-kurzname.md",
            // Der Arbeitsplan der Teststrategie-Umsetzung: ausdruecklich
            // voruebergehend und mit Abschluss der letzten Phase geloescht.
            // CLAUDE.md nennt ihn genau deshalb -- als Hinweis, dass er weg ist.
            "teststrategie-umsetzung.md")

        val genannteDateien = Regex("""\b(\w[\w.-]*\.(?:java|jsx|js|kts|md|toml|yml))\b""")
            .findAll(text).map { it.groupValues[1] }.toSortedSet()

        val vorhandeneNamen = projektWurzel.walkTopDown()
            .onEnter { verzeichnis ->
                verzeichnis.name !in setOf("node_modules", "build", ".git", ".gradle", "bin")
            }
            .filter { it.isFile }
            .map { it.name }
            .toSet()

        val verschwunden = (genannteDateien - vorhandeneNamen - bewussteNennungenOhneDatei).sorted()

        // Umgekehrte Richtung: Jeder Domaenentyp steht im Baum. Das ist die
        // Konvention "ein neuer Domaenentyp bekommt sofort seinen Platz",
        // nur eben geprueft statt verabredet.
        val domaenentypen = modellVerzeichnis.asFile.walkTopDown()
            .filter { it.isFile && it.extension == "java" && it.nameWithoutExtension != "package-info" }
            .map { it.nameWithoutExtension }
            .toSortedSet()
        val verschwiegen = domaenentypen.filter { typ ->
            !Regex("\\b${Regex.escape(typ)}\\b").containsMatchIn(text)
        }.sorted()

        val bericht = buildString {
            appendLine("Aufbaudoku: ${genannteDateien.size} Datei(en) in CLAUDE.md genannt, " +
                "${domaenentypen.size} Typ(en) in domain/model.")
            if (verschwunden.isEmpty() && verschwiegen.isEmpty()) {
                appendLine("CLAUDE.md und der Baum stimmen ueberein.")
            }
            if (verschwunden.isNotEmpty()) {
                appendLine("In CLAUDE.md genannt, im Projekt nicht vorhanden (${verschwunden.size}):")
                verschwunden.forEach { appendLine("  - $it") }
            }
            if (verschwiegen.isNotEmpty()) {
                appendLine("Domaenentyp ohne Erwaehnung in CLAUDE.md (${verschwiegen.size}):")
                verschwiegen.forEach { appendLine("  - $it") }
            }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        val fehler = mutableListOf<String>()
        if (verschwunden.isNotEmpty()) {
            fehler += "CLAUDE.md nennt Dateien, die es nicht gibt: ${verschwunden.joinToString(", ")}"
        }
        if (verschwiegen.isNotEmpty()) {
            fehler += "Domaenentyp fehlt im Aufbau-Abschnitt: ${verschwiegen.joinToString(", ")}"
        }
        if (fehler.isNotEmpty()) {
            throw GradleException(fehler.joinToString("; "))
        }
    }
}

tasks.named("check") {
    dependsOn("aufbaudoku")
}

// --- Feature-Dokumente gegen die Vorlage halten ---------------------------
//
// docs/features/_vorlage.md nennt sieben Pflichtabschnitte (Teststrategie
// Abschnitt 9.1). Bislang stand das nur in der Vorlage selbst -- ein
// Feature-Dokument, dem ein Abschnitt fehlt, fiel niemandem auf, der es
// nicht von Hand neben die Vorlage legt.
//
// Seit 2026-08-21 prueft der Task ausser der Vollstaendigkeit vier weitere
// Dinge, die alle denselben Zweck haben: dass ein Feature-Dokument ein
// Feature beschreibt und nicht acht. Ausloeser war Feature 005 (38
// Akzeptanzkriterien, acht Kritikalitaetsbereiche, eine eigene
// Neun-Stufen-Bautabelle) -- die Teilung hatte stattgefunden, nur eben im
// Dokument statt in mehreren Dokumenten.
//
//  1. Pflichtabschnitte -- unveraendert.
//  2. Genau eine Kritikalitaetsstufe, als maschinenlesbare Zeile
//     "**Stufe:** LOW|MEDIUM|HIGH". Zwei Stufen heissen: zwei Features.
//  3. "Betroffene Anforderungen" traegt die Pflichttabelle
//     "| ID | Bezug | Anmerkung |". Eine ID je Zeile, Bezug aus vier festen
//     Woertern; jede ID mit Bezug != neu muss in Anhang A existieren.
//     Fliesstext darunter bleibt unangetastet -- gerade weil ein Regex auf
//     Prosa die Fehlalarme erzeugt, an denen dieser Check im Audit vom
//     2026-08-20 zunaechst gescheitert war (Kapitelverweise wie
//     "11 (out of scope)", Platzhalter-Ranges, "Invariante 4").
//  4. Hoechstens zwoelf Akzeptanzkriterien.
//  5. Keine "Reihenfolge des Baus"-Tabelle mit mehr als einer Datenzeile.
//     Wer im Dokument Baustufen aufzaehlt, hat mehrere Features vor sich.
//
// Was weiterhin NICHT geprueft wird: ob "Umgesetzt in" existierende Klassen
// nennt (zu viele andere Backtick-Woerter im Fliesstext) und ob der Schnitt
// vertikal ist (ein horizontaler Schnitt besteht alle fuenf Pruefungen --
// dafuer ist der Skill `schneiden` da, nicht dieser Task). Zusaetzliche,
// ueber die Vorlage hinausgehende Abschnitte sind ausdruecklich erlaubt.
//
// Bestandsschutz: Die drei Dokumente unten sind vor der Regel entstanden und
// werden nach ihrer Umsetzung nicht mehr weitergepflegt (Skill `feature`,
// Schritt 7). Sie hier zu nennen ist Absicht -- eine Ausnahme, die im Build
// steht, ist etwas anderes als eine, die stillschweigend durchrutscht
// (dasselbe Verfahren wie bei den DDD-Stereotyp-Ausnahmen in
// ArchitectureTest).
tasks.register("featuredoku") {
    group = "verification"
    description = "Prueft, dass jedes Feature-Dokument die Vorlage einhaelt und genau ein Feature beschreibt."

    val featuresVerzeichnis = layout.projectDirectory.dir("docs/features")
    val anforderungenDatei = layout.projectDirectory.file("docs/anforderungen.md")
    val berichtsDatei = layout.buildDirectory.file("reports/featuredoku.txt")

    inputs.dir(featuresVerzeichnis)
    inputs.file(anforderungenDatei)
    outputs.file(berichtsDatei)

    doLast {
        val pflichtabschnitte = listOf(
            "Anlass", "Betroffene Anforderungen", "Akzeptanzkriterien",
            "Szenarien", "Kritikalität", "Umgesetzt in", "Offene Fragen")
        val hoechstzahlKriterien = 12
        val erlaubteBezuege = setOf("bestehend", "geändert", "neu", "zurückgenommen")

        // Datei -> Pruefungen, die fuer sie ausgesetzt sind. Grund und Zahl
        // stehen dabei, damit ein spaeterer Leser nicht raten muss.
        val bestandsschutz = mapOf(
            "002-ui-ueberarbeitung.md" to setOf("kriterienzahl"),          // 16 Kriterien
            "004-mehrere-watchpartys.md" to setOf("kriterienzahl"),        // 21 Kriterien
            "005-tippspiel-liga.md" to setOf("kriterienzahl", "stufe", "bautabelle"))

        // Alle IDs aus Anhang A -- dieselbe Quelle, die `abdeckung` liest.
        val anhangA = anforderungenDatei.asFile.readText()
            .substringAfter("## Anhang A")
            .lines()
            .mapNotNull { Regex("""^\|\s*([0-9]+(?:\.[0-9]+)?(?:-[a-z]+)?)\s*\|""").find(it)?.groupValues?.get(1) }
            .toSet()
        if (anhangA.isEmpty()) {
            throw GradleException("Anhang A in docs/anforderungen.md lieferte keine einzige ID -- Format geaendert?")
        }

        fun abschnitt(zeilen: List<String>, name: String): List<String> {
            val von = zeilen.indexOfFirst { it.trim() == "## $name" }
            if (von < 0) return emptyList()
            val bis = zeilen.drop(von + 1).indexOfFirst { it.startsWith("## ") }
            return if (bis < 0) zeilen.drop(von + 1) else zeilen.subList(von + 1, von + 1 + bis)
        }

        val dateien = featuresVerzeichnis.asFile.listFiles { f ->
            f.isFile && f.extension == "md" && f.name != "_vorlage.md"
        }?.sortedBy { it.name } ?: emptyList()

        val maengel = linkedMapOf<String, MutableList<String>>()
        dateien.forEach { datei ->
            val zeilen = datei.readLines()
            val ausgesetzt = bestandsschutz[datei.name] ?: emptySet()
            val fehler = mutableListOf<String>()

            // 1. Pflichtabschnitte
            val ueberschriften = zeilen.filter { it.startsWith("## ") }
                .map { it.removePrefix("## ").trim() }.toSet()
            val fehlendeAbschnitte = pflichtabschnitte.filter { it !in ueberschriften }
            if (fehlendeAbschnitte.isNotEmpty()) {
                fehler += "fehlende Abschnitte: ${fehlendeAbschnitte.joinToString(", ")}"
            }

            // 2. Genau eine Kritikalitaetsstufe
            if ("stufe" !in ausgesetzt) {
                val stufen = abschnitt(zeilen, "Kritikalität")
                    .mapNotNull { Regex("""^\*\*Stufe:\*\*\s+(LOW|MEDIUM|HIGH)\s*$""").find(it.trim())?.groupValues?.get(1) }
                when (stufen.size) {
                    1 -> Unit
                    0 -> fehler += "keine Zeile \"**Stufe:** LOW|MEDIUM|HIGH\" im Abschnitt Kritikalität"
                    else -> fehler += "${stufen.size} Kritikalitätsstufen (${stufen.joinToString("/")}) -- das sind ${stufen.size} Features"
                }
            }

            // 3. Pflichttabelle in "Betroffene Anforderungen"
            val bezugsZeilen = abschnitt(zeilen, "Betroffene Anforderungen")
                .map { it.trim() }
                .filter { it.startsWith("|") && !it.startsWith("|---") && !it.startsWith("| ID ") }
            if (bezugsZeilen.isEmpty()) {
                fehler += "Abschnitt \"Betroffene Anforderungen\" ohne Pflichttabelle | ID | Bezug | Anmerkung |"
            }
            bezugsZeilen.forEach { zeile ->
                val spalten = zeile.trim('|').split("|").map { it.trim() }
                if (spalten.size < 2) {
                    fehler += "Tabellenzeile ohne Bezug-Spalte: $zeile"
                    return@forEach
                }
                val (id, bezug) = spalten[0] to spalten[1]
                if (bezug !in erlaubteBezuege) {
                    fehler += "unbekannter Bezug \"$bezug\" bei $id (erlaubt: ${erlaubteBezuege.joinToString(", ")})"
                }
                if (!Regex("""^[0-9]+(\.[0-9]+)?(-[a-z]+)?$""").matches(id)) {
                    fehler += "\"$id\" ist keine Anhang-A-ID -- Bereiche, Kapitelverweise und Invarianten gehören unter die Tabelle"
                } else if (bezug != "neu" && id !in anhangA) {
                    fehler += "$id ($bezug) steht nicht in Anhang A"
                }
            }

            // 4. Zahl der Akzeptanzkriterien
            if ("kriterienzahl" !in ausgesetzt) {
                val anzahl = abschnitt(zeilen, "Akzeptanzkriterien").count { Regex("""^[0-9]+\.\s""").containsMatchIn(it) }
                if (anzahl > hoechstzahlKriterien) {
                    fehler += "$anzahl Akzeptanzkriterien (höchstens $hoechstzahlKriterien) -- der Schnitt ist zu breit, siehe Skill `schneiden`"
                }
            }

            // 5. Keine eigene Bautabelle
            if ("bautabelle" !in ausgesetzt) {
                val stufenZeilen = abschnitt(zeilen, "Reihenfolge des Baus")
                    .map { it.trim() }
                    .filter { it.startsWith("|") && !it.startsWith("|---") && !it.startsWith("| # ") }
                if (stufenZeilen.size > 1) {
                    fehler += "\"Reihenfolge des Baus\" mit ${stufenZeilen.size} Stufen -- das sind ${stufenZeilen.size} Features, siehe Skill `schneiden`"
                }
            }

            if (fehler.isNotEmpty()) maengel[datei.name] = fehler
        }

        val bericht = buildString {
            appendLine("Feature-Dokumente: ${dateien.size} geprueft, ${bestandsschutz.size} mit Bestandsschutz.")
            if (maengel.isEmpty()) {
                appendLine("Jedes Feature-Dokument haelt die Vorlage ein und beschreibt genau ein Feature.")
            } else {
                appendLine("Beanstandet (${maengel.size}):")
                maengel.forEach { (name, liste) ->
                    appendLine("  - $name:")
                    liste.forEach { appendLine("      $it") }
                }
            }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        if (maengel.isNotEmpty()) {
            throw GradleException("Feature-Dokument(e) beanstandet: ${maengel.keys.joinToString(", ")}")
        }
    }
}

tasks.named("check") {
    dependsOn("featuredoku")
}

// --- Protokollvertrag Frontend <-> Backend --------------------------------
//
// Abschnitt 11 der Teststrategie nennt diese Luecke beim Namen: "Die
// Vertraeglichkeit zwischen Frontend und Protokoll. Das Frontend wird getrennt
// gebaut; eine Aenderung am Frame-Format ist ein Bruch, den kein Backend-Test
// sieht." Genau dieser Bruch wird hier gefangen -- nicht durch Frontend-Tests
// (das waere ein eigenes Programm), sondern durch den Abgleich der *einen*
// Grenze, an der beide Seiten sich auf dieselben Namen einigen muessen.
//
// Geprueft wird nur die Live-Wetten-App (frontend/src/*.jsx|js). Das Tippspiel
// unter frontend/src/league spricht REST statt WebSocket (ADR-039) und hat
// einen eigenen Vertrag; seine Literale ("POST", "DELETE") sind HTTP-Verben
// und gehoeren nicht in dieses Vokabular.
tasks.register("protokollvertrag") {
    group = "verification"
    description = "Gleicht Frame-Typen und Feldnamen des WebSocket-Protokolls mit der Live-Wetten-App ab."
    dependsOn(tasks.named("classes"))

    val messagesQuelle = layout.projectDirectory.file("src/main/java/de/fourteen/watchparty/application/message/Messages.java")
    val handlerQuelle = layout.projectDirectory.file("src/main/java/de/fourteen/watchparty/adapter/in/ws/GameWebSocketHandler.java")
    val roomViewQuelle = layout.projectDirectory.file("src/main/java/de/fourteen/watchparty/application/RoomView.java")
    val phaseQuelle = layout.projectDirectory.file("src/main/java/de/fourteen/watchparty/domain/model/Phase.java")
    val frontendVerzeichnis = layout.projectDirectory.dir("frontend/src")
    val klassenpfad = sourceSets.main.get().runtimeClasspath
    val berichtsDatei = layout.buildDirectory.file("reports/protokollvertrag.txt")

    inputs.file(messagesQuelle)
    inputs.file(handlerQuelle)
    inputs.file(roomViewQuelle)
    inputs.file(phaseQuelle)
    inputs.dir(frontendVerzeichnis)
    outputs.file(berichtsDatei)

    doLast {
        // Ein Frame-Typ oder Phasenwert sieht so aus: mindestens drei Zeichen,
        // Grossbuchstaben und Unterstriche. Das trennt "WELCOME" und
        // "PLACE_PICK" zuverlaessig von gewoehnlichen Texten.
        val protokollToken = Regex("\"([A-Z][A-Z_]{2,})\"")

        fun literaleAus(datei: java.io.File): Set<String> =
            protokollToken.findAll(datei.readText()).map { it.groupValues[1] }.toSet()

        // Server -> Client: die type()-Literale der Nachrichtentypen.
        val ausgehendeFrames = Regex("""return "([A-Z][A-Z_]{2,})";""")
            .findAll(messagesQuelle.asFile.readText()).map { it.groupValues[1] }.toSet()

        // Client -> Server: die Faelle des Handler-switch.
        val eingehendeFrames = Regex("""case "([A-Z][A-Z_]{2,})"""")
            .findAll(handlerQuelle.asFile.readText()).map { it.groupValues[1] }.toSet()

        // Werte, die zwar keine Frame-Typen sind, aber ueber die Leitung gehen:
        // die Phasen und die Annullierungsgruende aus RoomView ("HOST",
        // "NO_PICKS").
        val phasen = Regex("""\b(IDLE|OPEN|CLOSED|RESOLVED)\b""")
            .findAll(phaseQuelle.asFile.readText()).map { it.groupValues[1] }.toSet()
        val weitereWerte = literaleAus(roomViewQuelle.asFile)

        val serverVokabular = ausgehendeFrames + eingehendeFrames + phasen + weitereWerte

        // Nur die Live-Wetten-App, ohne league/ und legal/ (siehe Kommentar oben).
        val liveWettenDateien = frontendVerzeichnis.asFile.listFiles { f ->
            f.isFile && (f.extension == "js" || f.extension == "jsx")
        }?.toList() ?: emptyList()
        val frontendText = liveWettenDateien.joinToString("\n") { it.readText() }
        val frontendLiterale = protokollToken.findAll(frontendText).map { it.groupValues[1] }.toSet()

        // Feldnamen: die Record-Komponenten der Nachrichtentypen, ueber
        // Reflection statt per Regex -- ein Record kennt seine Komponenten
        // selbst, das ist belastbarer als das Auseinandernehmen einer
        // Parameterliste mit Javadoc darin.
        val urls = klassenpfad.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, javaClass.classLoader)
        val messagesKlasse = classLoader.loadClass("de.fourteen.watchparty.application.message.Messages")
        val felder = sortedSetOf<String>()
        fun sammleFelder(klasse: Class<*>) {
            if (klasse.isRecord) {
                klasse.recordComponents.forEach { felder += it.name }
            }
            klasse.declaredClasses.forEach { sammleFelder(it) }
        }
        sammleFelder(messagesKlasse)

        // --- Die drei Pruefungen ------------------------------------------
        //
        // Ausnahmen stehen hier, nicht in einer Datei nebenan: Sie sind heute
        // leer, und eine leere Liste im Build ist ehrlicher als eine leere
        // Datei, die niemand findet. Wer eine Ausnahme braucht, traegt sie hier
        // mit Begruendung ein -- sichtbar im Diff, nicht stillschweigend.
        val bekannteNichtProtokollLiterale = emptySet<String>()
        val felderOhneFrontend = emptySet<String>()

        val unbekannteLiterale = (frontendLiterale - serverVokabular - bekannteNichtProtokollLiterale).sorted()
        val frames = ausgehendeFrames + eingehendeFrames
        val ungenutzteFrames = (frames - frontendLiterale).sorted()
        val unbekannteFelder = felder.filter { feld ->
            feld !in felderOhneFrontend && !Regex("\\b${Regex.escape(feld)}\\b").containsMatchIn(frontendText)
        }.sorted()

        val bericht = buildString {
            appendLine("Protokollvertrag: ${frames.size} Frame-Typ(en), ${felder.size} Feld(er), " +
                "${frontendLiterale.size} Literal(e) in der Live-Wetten-App.")
            if (unbekannteLiterale.isEmpty() && ungenutzteFrames.isEmpty() && unbekannteFelder.isEmpty()) {
                appendLine("Beide Seiten sprechen dasselbe Protokoll.")
            }
            if (unbekannteLiterale.isNotEmpty()) {
                appendLine("Frontend nennt Token, die der Server nicht kennt (${unbekannteLiterale.size}):")
                unbekannteLiterale.forEach { appendLine("  - $it") }
            }
            if (ungenutzteFrames.isNotEmpty()) {
                appendLine("Frame-Typen, die im Frontend nicht vorkommen (${ungenutzteFrames.size}):")
                ungenutzteFrames.forEach { appendLine("  - $it") }
            }
            if (unbekannteFelder.isNotEmpty()) {
                appendLine("Nachrichtenfelder, die im Frontend nicht vorkommen (${unbekannteFelder.size}):")
                unbekannteFelder.forEach { appendLine("  - $it") }
            }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        val fehler = mutableListOf<String>()
        if (unbekannteLiterale.isNotEmpty()) {
            fehler += "Frontend nennt unbekannte Token: ${unbekannteLiterale.joinToString(", ")} " +
                "(Tippfehler, oder serverseitig umbenannt)"
        }
        if (ungenutzteFrames.isNotEmpty()) {
            fehler += "Frame-Typen ohne Entsprechung im Frontend: ${ungenutzteFrames.joinToString(", ")} " +
                "(umbenannt, ohne das Frontend nachzuziehen)"
        }
        if (unbekannteFelder.isNotEmpty()) {
            fehler += "Nachrichtenfelder ohne Entsprechung im Frontend: ${unbekannteFelder.joinToString(", ")} " +
                "(umbenannt, oder das Frontend liest sie noch nicht)"
        }
        if (fehler.isNotEmpty()) {
            throw GradleException("Protokollvertrag verletzt -- " + fehler.joinToString("; "))
        }
    }
}

tasks.named("check") {
    dependsOn("protokollvertrag")
}

// --- Protokollvertrag Tippspiel (docs/teststrategie.md, Abschnitt 11) ------
//
// Der Task oben prueft die WebSocket-Grenze der Live-Wetten und sagt selbst,
// dass er das Tippspiel bewusst auslaesst. Damit stand dieselbe Fehlerart
// dort ungeprueft: Ein umbenanntes DTO-Feld oder ein verschobener Pfad faellt
// im Backend nicht auf -- die Tests kennen ja beide Seiten nicht zugleich --
// und im Frontend erst zur Laufzeit, als leeres Feld oder als 404.
//
// REST hat gegenueber dem WebSocket-Protokoll eine zweite Grenze: nicht nur
// Feldnamen, auch Pfade. Beide werden hier abgeglichen, und die Pfade in
// beide Richtungen -- ein Frontend, das einen Pfad ruft, den es nicht gibt,
// ist der teurere Fall von beiden.
tasks.register("protokollvertragLiga") {
    group = "verification"
    description = "Gleicht Pfade und Feldnamen der REST-Schnittstelle mit der Tippspiel-App ab."
    dependsOn(tasks.named("classes"))

    val httpVerzeichnis = layout.projectDirectory.dir("src/main/java/de/fourteen/watchparty/adapter/in/http")
    val ligaFrontend = layout.projectDirectory.dir("frontend/src/league")
    val klassenpfad = sourceSets.main.get().runtimeClasspath
    val berichtsDatei = layout.buildDirectory.file("reports/protokollvertrag-liga.txt")

    inputs.dir(httpVerzeichnis)
    inputs.dir(ligaFrontend)
    outputs.file(berichtsDatei)

    doLast {
        val httpDateien = httpVerzeichnis.asFile.walkTopDown().filter { it.extension == "java" }.toList()
        val serverText = httpDateien.joinToString("\n") { it.readText() }
        val frontendDateien = ligaFrontend.asFile.walkTopDown()
            .filter { it.isFile && (it.extension == "js" || it.extension == "jsx") }.toList()
        val frontendText = frontendDateien.joinToString("\n") { it.readText() }
        val apiText = ligaFrontend.file("api.js").asFile.readText()

        // --- Pfade -------------------------------------------------------
        //
        // Beide Seiten werden auf dieselbe Form gebracht: fuehrendes
        // /api/league weg, jeder Platzhalter zu einem *. Aus
        // "/api/league/leagues/{leagueId}/standings/matchday/{week}" und aus
        // `/leagues/${leagueId}/standings/matchday/${week}` wird damit
        // dasselbe "/leagues/*/standings/matchday/*".
        fun vereinheitliche(pfad: String): String =
            pfad.removePrefix("/api/league")
                // Erst die JS-Interpolation ${...}, dann die Spring-Vorlage
                // {...} -- andersherum bliebe von ${leagueId} ein "$*" uebrig.
                .replace(Regex("""\$\{[^}]*\}"""), "*")
                .replace(Regex("""\{[^}]*\}"""), "*")
                .removeSuffix("/")

        val serverPfade = Regex("""@(?:Get|Post|Put|Delete|Patch)Mapping\("([^"]+)"\)""")
            .findAll(serverText).map { vereinheitliche(it.groupValues[1]) }.toSet()

        // Der erste Parameter jedes request(...)-Aufrufs in api.js, in beiden
        // Schreibweisen: "..." und `...`.
        val frontendPfade = Regex("""request\(\s*(?:"([^"]+)"|`([^`]+)`)""")
            .findAll(apiText)
            .map { vereinheitliche(it.groupValues[1].ifEmpty { it.groupValues[2] }) }
            .toSet()

        // Der Feed-Relay wird nicht vom Browser gerufen, sondern vom
        // GitHub-Actions-Workflow (ADR-037-Nachtrag) -- er hat im Frontend
        // bewusst keine Entsprechung. Ebenso der Handeintrag, der ueber
        // curl bedient wird (Kriterium 14).
        val pfadeOhneFrontend = setOf("/feed-relay/*/*", "/admin/games/*/result")

        val frontendPfadeOhneServer = (frontendPfade - serverPfade).sorted()
        val serverPfadeOhneFrontend = (serverPfade - frontendPfade - pfadeOhneFrontend).sorted()

        // --- Feldnamen ---------------------------------------------------
        //
        // Wie beim WebSocket-Vertrag ueber Reflection statt per Regex: Ein
        // Record kennt seine Komponenten selbst. Gesammelt wird ueber die
        // Controller-DTOs und die beiden Sichten, die sie zurueckgeben.
        val urls = klassenpfad.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, javaClass.classLoader)
        val wurzeln = listOf(
            "de.fourteen.watchparty.adapter.in.http.LoginController",
            "de.fourteen.watchparty.adapter.in.http.LeagueController",
            "de.fourteen.watchparty.adapter.in.http.PredictionController",
            "de.fourteen.watchparty.application.league.view.PredictionView",
            "de.fourteen.watchparty.application.league.view.ReportView")
        val felder = sortedSetOf<String>()
        fun sammleFelder(klasse: Class<*>) {
            if (klasse.isRecord) {
                klasse.recordComponents.forEach { felder += it.name }
            }
            klasse.declaredClasses.forEach { sammleFelder(it) }
        }
        wurzeln.forEach { sammleFelder(classLoader.loadClass(it)) }

        // Felder, die das Frontend nicht liest, mit Begruendung -- sichtbar
        // im Diff statt in einer Datei nebenan (dieselbe Regel wie oben).
        //
        // correctTendencyCount: die dritte Stufe der Gleichstandsregel
        // (13.6-g). Der Server liefert sie, damit die Rangfolge vollstaendig
        // begruendet ist; die Tabelle zeigt bewusst nur Platz, Name und
        // Punktzahl. Beim ersten Fund am 2026-08-21 eingetragen -- die Regel
        // selbst ist backend-markiert und in Standings geprueft, die Anzeige
        // waere eine eigene, frontend-markierte Entscheidung.
        val felderOhneFrontend = setOf("correctTendencyCount")

        val unbekannteFelder = felder.filter { feld ->
            feld !in felderOhneFrontend && !Regex("\\b${Regex.escape(feld)}\\b").containsMatchIn(frontendText)
        }.sorted()

        val bericht = buildString {
            appendLine("Protokollvertrag Tippspiel: ${serverPfade.size} Serverpfad(e), " +
                "${frontendPfade.size} Frontendpfad(e), ${felder.size} Feld(er).")
            if (frontendPfadeOhneServer.isEmpty() && serverPfadeOhneFrontend.isEmpty() && unbekannteFelder.isEmpty()) {
                appendLine("Beide Seiten sprechen dieselbe Schnittstelle.")
            }
            if (frontendPfadeOhneServer.isNotEmpty()) {
                appendLine("Frontend ruft Pfade, die der Server nicht anbietet (${frontendPfadeOhneServer.size}):")
                frontendPfadeOhneServer.forEach { appendLine("  - $it") }
            }
            if (serverPfadeOhneFrontend.isNotEmpty()) {
                appendLine("Serverpfade ohne Entsprechung im Frontend (${serverPfadeOhneFrontend.size}):")
                serverPfadeOhneFrontend.forEach { appendLine("  - $it") }
            }
            if (unbekannteFelder.isNotEmpty()) {
                appendLine("Antwortfelder, die im Frontend nicht vorkommen (${unbekannteFelder.size}):")
                unbekannteFelder.forEach { appendLine("  - $it") }
            }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        val fehler = mutableListOf<String>()
        if (frontendPfadeOhneServer.isNotEmpty()) {
            fehler += "Frontend ruft unbekannte Pfade: ${frontendPfadeOhneServer.joinToString(", ")} " +
                "(serverseitig umbenannt oder Tippfehler -- zur Laufzeit ein 404)"
        }
        if (serverPfadeOhneFrontend.isNotEmpty()) {
            fehler += "Serverpfade ohne Aufrufer: ${serverPfadeOhneFrontend.joinToString(", ")} " +
                "(umbenannt, ohne das Frontend nachzuziehen -- oder eine bewusste Ausnahme fehlt)"
        }
        if (unbekannteFelder.isNotEmpty()) {
            fehler += "Antwortfelder ohne Entsprechung im Frontend: ${unbekannteFelder.joinToString(", ")} " +
                "(umbenannt, oder das Frontend liest sie noch nicht)"
        }
        if (fehler.isNotEmpty()) {
            throw GradleException("Protokollvertrag Tippspiel verletzt -- " + fehler.joinToString("; "))
        }
    }
}

tasks.named("check") {
    dependsOn("protokollvertragLiga")
}

// --- Ausnahmenregister (docs/test-ausnahmen.md, Abschnitt 10) -------------
//
// "Eine Unterdrueckung ohne Eintrag ist ein Fehler" -- so steht die Regel in
// der Teststrategie, geprueft hat sie bisher niemand. Die Datei wurde von
// keinem Task gelesen, sie stand nur in Kommentaren.
//
// Erfasst werden beide Formen der Unterdrueckung: @AequivalenterMutant
// (ein Mutant, den PIT gar nicht erst erzeugt) und @Disabled (ein Test, der
// nicht laeuft). Beides entzieht dem Bau etwas, ohne dass der Bau rot wird --
// genau deshalb braucht es den Eintrag mit Begruendung und Datum.
//
// Der Abgleich laeuft in beide Richtungen: eine Unterdrueckung ohne Eintrag
// ist ein Fehler, ein Eintrag ohne Unterdrueckung ebenso. Ein Register, in dem
// Karteileichen stehen bleiben, verliert seinen Zweck genauso wie eines, in
// dem Eintraege fehlen.
tasks.register("ausnahmenregister") {
    group = "verification"
    description = "Prueft, dass jede @AequivalenterMutant- und @Disabled-Unterdrueckung in docs/test-ausnahmen.md steht."
    dependsOn(tasks.named("classes"), tasks.named("testClasses"))

    val registerDatei = layout.projectDirectory.file("docs/test-ausnahmen.md")
    val hauptKlassen = sourceSets.main.get().output.classesDirs
    val testKlassen = sourceSets.test.get().output.classesDirs
    val klassenpfad = sourceSets.test.get().runtimeClasspath
    val berichtsDatei = layout.buildDirectory.file("reports/ausnahmenregister.txt")

    inputs.file(registerDatei)
    inputs.files(hauptKlassen)
    inputs.files(testKlassen)
    outputs.file(berichtsDatei)

    doLast {
        val urls = klassenpfad.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, javaClass.classLoader)

        fun annotationsKlasse(name: String): Class<out Annotation>? {
            @Suppress("UNCHECKED_CAST")
            return try {
                classLoader.loadClass(name) as Class<out Annotation>
            } catch (e: Throwable) {
                null
            }
        }

        val aequivalenterMutant = annotationsKlasse("de.fourteen.watchparty.mutationtest.AequivalenterMutant")
        val disabled = annotationsKlasse("org.junit.jupiter.api.Disabled")

        // Bezeichner wie in der Doku: einfacher Klassenname, bei einer Methode
        // zusaetzlich ".methodenname". Voll qualifiziert waere eindeutiger,
        // aber die Tabelle soll lesbar bleiben -- sie wird von Menschen
        // gepflegt, nicht von einem Werkzeug.
        fun sammle(verzeichnisse: FileCollection, annotation: Class<out Annotation>?): Set<String> {
            if (annotation == null) return emptySet()
            val gefunden = sortedSetOf<String>()
            verzeichnisse.forEach { wurzelVerzeichnis ->
                wurzelVerzeichnis.walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .forEach { classFile ->
                        val klassenname = classFile.relativeTo(wurzelVerzeichnis).path
                            .removeSuffix(".class").replace(File.separatorChar, '.')
                        val klasse = try {
                            classLoader.loadClass(klassenname)
                        } catch (e: Throwable) {
                            return@forEach
                        }
                        val einfacherName = klasse.simpleName
                        if (klasse.getAnnotation(annotation) != null) {
                            gefunden += einfacherName
                        }
                        for (methode in klasse.declaredMethods) {
                            if (methode.getAnnotation(annotation) != null) {
                                gefunden += "$einfacherName.${methode.name}"
                            }
                        }
                    }
            }
            return gefunden
        }

        val unterdrueckungen = sammle(hauptKlassen, aequivalenterMutant) + sammle(testKlassen, disabled)

        // Erste Spalte jeder Tabellenzeile, ohne Backticks und ohne die
        // Platzhalterzeile "(keine Eintraege)".
        val eintraege = sortedSetOf<String>()
        registerDatei.asFile.forEachLine { zeile ->
            val getrimmt = zeile.trim()
            if (!getrimmt.startsWith("|")) return@forEachLine
            val ersteSpalte = getrimmt.trim('|').split("|").firstOrNull()?.trim()?.trim('`') ?: return@forEachLine
            if (ersteSpalte.isEmpty()) return@forEachLine
            if (ersteSpalte.startsWith("---")) return@forEachLine
            if (ersteSpalte == "Klasse/Methode" || ersteSpalte == "Test") return@forEachLine
            if (ersteSpalte.startsWith("_(")) return@forEachLine
            eintraege += ersteSpalte
        }

        val ohneEintrag = (unterdrueckungen - eintraege).sorted()
        val ohneUnterdrueckung = (eintraege - unterdrueckungen).sorted()

        val bericht = buildString {
            appendLine("Ausnahmenregister: ${unterdrueckungen.size} Unterdrueckung(en) im Code, ${eintraege.size} Eintrag/Eintraege in docs/test-ausnahmen.md.")
            if (ohneEintrag.isEmpty() && ohneUnterdrueckung.isEmpty()) {
                appendLine("Code und Register stimmen ueberein.")
            }
            if (ohneEintrag.isNotEmpty()) {
                appendLine("Ohne Eintrag im Register (${ohneEintrag.size}):")
                ohneEintrag.forEach { appendLine("  - $it") }
            }
            if (ohneUnterdrueckung.isNotEmpty()) {
                appendLine("Eintrag ohne Entsprechung im Code (${ohneUnterdrueckung.size}):")
                ohneUnterdrueckung.forEach { appendLine("  - $it") }
            }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        if (ohneEintrag.isNotEmpty()) {
            throw GradleException(
                "Unterdrueckung ohne Eintrag in docs/test-ausnahmen.md: ${ohneEintrag.joinToString(", ")} " +
                    "-- jede Ausnahme wird dort mit Begruendung und Datum benannt (Teststrategie, Abschnitt 10).")
        }
        if (ohneUnterdrueckung.isNotEmpty()) {
            throw GradleException(
                "Karteileiche in docs/test-ausnahmen.md: ${ohneUnterdrueckung.joinToString(", ")} " +
                    "-- im Code gibt es dazu keine Unterdrueckung mehr, der Eintrag gehoert entfernt.")
        }
    }
}

tasks.named("check") {
    dependsOn("ausnahmenregister")
}

// --- Null-Sicherheit (ADR-026) ---------------------------------------------
//
// NullAway prueft im JSpecify-Modus nur Code, der explizit @NullMarked ist
// (package-info.java in domain, application, adapter, config) -- alles
// andere (Spring/Jackson/JDK) bleibt "legacy" und wird nicht mitgepruegelt.
// Innerhalb der markierten Pakete ist jeder Verweistyp nicht-null, ausser er
// traegt @Nullable; ein Verstoss ist ein Compile-Fehler.
//
// Nur NullAway laeuft, keine der uebrigen Error-Prone-Pruefungen -- diese
// Einrichtung soll Null-Sicherheit durchsetzen, keinen Stilkatalog.
tasks.withType<JavaCompile>().configureEach {
    // Von Error Prone selbst verlangt, sobald der Compiler Typannotationen
    // (wie @Nullable auf Feldern/Parametern) an Symbole binden soll.
    options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
    options.errorprone {
        disableAllChecks.set(true)
        // Die getypte NullAway-DSL statt roher -Xep-Optionen: Wer die Severity
        // per option("NullAway:...") von Hand setzt, konkurriert mit der
        // Konfiguration, die dieses Plugin selbst schon einhaengt, und die
        // zuletzt geschriebene Xep-Flagge gewinnt -- verwirrend und zerbrechlich.
        nullaway {
            severity.set(CheckSeverity.ERROR)
            onlyNullMarked.set(true)
            jspecifyMode.set(true)
        }
    }
}

// Testcode ist bewusst nicht @NullMarked (siehe unten) -- NullAway liesse ihn
// deshalb ohnehin durch, aber ohne Error Prone ueberhaupt erst anzuwerfen
// spart das Zeit und macht die Abgrenzung im Build sichtbar.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.enabled.set(false)
}

// Die Wett-Texte und Fehlermeldungen gehen unveraendert in die Oberflaeche,
// deshalb muss die Quelltext-Kodierung festgelegt sein statt von der
// Plattform-Voreinstellung abzuhaengen — sonst zerfallen Umlaute im Jar.
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// --- Frontend-Build -------------------------------------------------------
// Baut die React-App und legt das Ergebnis in build/frontend ab, von wo es
// als statische Ressource in das Jar wandert. Braucht npm auf dem PATH.
// Im Docker-Build wird das Frontend separat gebaut (siehe Dockerfile), daher
// laesst sich dieser Schritt mit -PskipFrontend ueberspringen.

val frontendDir = layout.projectDirectory.dir("frontend")
val frontendOut = layout.buildDirectory.dir("frontend/static")
val skipFrontend = providers.gradleProperty("skipFrontend").isPresent
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val npm = if (isWindows) "npm.cmd" else "npm"

val npmInstall = tasks.register<Exec>("npmInstall") {
    workingDir = frontendDir.asFile
    commandLine(npm, "install")
    inputs.file(frontendDir.file("package.json"))
    outputs.dir(frontendDir.dir("node_modules"))
}

val npmBuild = tasks.register<Exec>("npmBuild") {
    dependsOn(npmInstall)
    workingDir = frontendDir.asFile
    commandLine(npm, "run", "build", "--", "--outDir", frontendOut.get().asFile.absolutePath, "--emptyOutDir")
    inputs.dir(frontendDir.dir("src"))
    inputs.file(frontendDir.file("index.html"))
    outputs.dir(frontendOut)
}

tasks.named<ProcessResources>("processResources") {
    if (!skipFrontend) {
        dependsOn(npmBuild)
        from(frontendOut) {
            into("static")
        }
    }
}

// --- Frontend-Ebene: Testlauf und Abdeckung (Abschnitt 2.6/7.5) -----------
//
// Bis 2026-08-21 sagte die Teststrategie in Abschnitt 11 selbst: "Das
// Frontend ist ausserhalb." Das betraf echte Anforderungen -- dass das
// Leaderboard die Kontostaende zeigt (3-d), dass die Anmerkungen sichtbar
// sind (4-f), die ganze Hoehepunkt-Bildung im Spieltags-Report (13.9-f..m).
// Sie trugen die Marke `frontend` und zaehlten damit zu gar keiner Zahl.
//
// Jetzt sind sie eine eigene Ebene mit eigener Abdeckung. Der Grundsatz aus
// Abschnitt 1 gilt dort unveraendert: Geprueft wird nur die Projektion
// Serverdaten -> sichtbare Ausgabe, keine fachliche Regel ein zweites Mal.
val npmTest = tasks.register<Exec>("npmTest") {
    group = "verification"
    description = "Frontend-Ebene: Vitest ueber frontend/tests (Abschnitt 2.6)."
    dependsOn(npmInstall)
    workingDir = frontendDir.asFile
    commandLine(npm, "run", "test")
    inputs.dir(frontendDir.dir("src"))
    inputs.dir(frontendDir.dir("tests"))
    inputs.file(frontendDir.file("package.json"))
    inputs.file(frontendDir.file("vite.config.js"))
    outputs.file(layout.buildDirectory.file("reports/frontend-tests.json"))
}

tasks.named("check") {
    dependsOn(npmTest)
}

// Dieselbe Messung wie `abdeckung`, nur fuer die andere Haelfte von Anhang A:
// Jede Regel mit der Marke `frontend` braucht mindestens ein Szenario, das
// ihre ID traegt -- auf der Frontend-Ebene (frontend/tests) oder in E2E
// (e2e/tests). Beide Orte werden gelesen, weil beide Frontend-Verhalten
// pruefen; welcher der richtige ist, entscheidet die Regel, nicht der Task.
//
// Die Gegenrichtung wird mitgeprueft: Eine ID im Test, die es in Anhang A
// nicht gibt, ist ein Tippfehler oder eine geloeschte Regel -- beides soll
// auffallen, nicht stillschweigend als Abdeckung zaehlen.
tasks.register("abdeckungFrontend") {
    group = "verification"
    description = "Vergleicht die frontend-Regeln aus Anhang A mit den anforderung()-Szenarien."
    dependsOn(npmTest)

    val anforderungenDatei = layout.projectDirectory.file("docs/anforderungen.md")
    val frontendTests = layout.projectDirectory.dir("frontend/tests")
    val e2eTests = layout.projectDirectory.dir("e2e/tests")
    val berichtsDatei = layout.buildDirectory.file("reports/abdeckung-frontend.txt")

    inputs.file(anforderungenDatei)
    inputs.dir(frontendTests)
    outputs.file(berichtsDatei)

    doLast {
        val zeilePattern = Regex(
            """^\|\s*([0-9]+(?:\.[0-9]+)?(?:-[a-z])?)\s*\|.*\|\s*(backend|frontend|organisatorisch|beobachtung|gestaltung)\s*\|\s*$""")
        var inAnhangA = false
        val frontendRegeln = linkedSetOf<String>()
        val alleRegeln = linkedSetOf<String>()
        anforderungenDatei.asFile.forEachLine { zeile ->
            if (zeile.startsWith("## Anhang A")) {
                inAnhangA = true
            } else if (inAnhangA) {
                val treffer = zeilePattern.matchEntire(zeile)
                if (treffer != null) {
                    alleRegeln += treffer.groupValues[1]
                    if (treffer.groupValues[2] == "frontend") {
                        frontendRegeln += treffer.groupValues[1]
                    }
                }
            }
        }

        // Die IDs aus den anforderung(...)-Aufrufen -- erstes Argument, als
        // Zeichenkette oder als Liste von Zeichenketten.
        val getaggt = linkedSetOf<String>()
        val aufruf = Regex("""anforderung\(\s*(\[[^\]]*\]|"[^"]*")""")
        val einzelneId = Regex(""""([^"]+)"""")
        listOf(frontendTests.asFile, e2eTests.asFile)
            .filter { it.isDirectory }
            .forEach { verzeichnis ->
                verzeichnis.walkTopDown()
                    .filter { it.isFile && (it.extension == "js" || it.extension == "jsx") }
                    .forEach { datei ->
                        aufruf.findAll(datei.readText()).forEach { treffer ->
                            einzelneId.findAll(treffer.groupValues[1]).forEach { getaggt += it.groupValues[1] }
                        }
                    }
            }

        val ohneSzenario = (frontendRegeln - getaggt).sorted()
        val unbekannteIds = (getaggt - alleRegeln).sorted()

        val bericht = buildString {
            appendLine("Frontend-Abdeckung: ${frontendRegeln.size - ohneSzenario.size} von " +
                "${frontendRegeln.size} frontend-Regel(n) haben ein Szenario.")
            if (ohneSzenario.isEmpty() && unbekannteIds.isEmpty()) {
                appendLine("Jede frontend-Regel aus Anhang A ist geprueft.")
            }
            if (ohneSzenario.isNotEmpty()) {
                appendLine("Ohne Szenario (${ohneSzenario.size}):")
                ohneSzenario.forEach { appendLine("  - $it") }
            }
            if (unbekannteIds.isNotEmpty()) {
                appendLine("IDs in Tests, die Anhang A nicht kennt (${unbekannteIds.size}):")
                unbekannteIds.forEach { appendLine("  - $it") }
            }
        }
        println(bericht)
        val datei = berichtsDatei.get().asFile
        datei.parentFile.mkdirs()
        datei.writeText(bericht)

        val fehler = mutableListOf<String>()
        if (ohneSzenario.isNotEmpty()) {
            fehler += "frontend-Regeln ohne Szenario: ${ohneSzenario.joinToString(", ")}"
        }
        if (unbekannteIds.isNotEmpty()) {
            fehler += "unbekannte Anforderungs-IDs in Tests: ${unbekannteIds.joinToString(", ")}"
        }
        if (fehler.isNotEmpty()) {
            throw GradleException("Frontend-Abdeckung unvollstaendig -- " + fehler.joinToString("; "))
        }
    }
}

tasks.named("check") {
    dependsOn("abdeckungFrontend")
}

// --- Major-Versionsupdates als Rezept (ADR-042) ---------------------------
//
// Ein Major-Sprung bringt brechende Aenderungen mit; bis hierher hat die
// taegliche Dependabot-Routine sie von Hand nachgezogen -- aus den Release
// Notes gelesen und interpretiert. OpenRewrite macht daraus eine
// Programmausfuehrung: Der Hersteller der Bibliothek beschreibt den Umstieg
// einmal als Rezept, die Routine wendet es an. Was das Rezept nicht abdeckt,
// bleibt Handarbeit -- aber es ist danach sichtbar weniger.
//
// Die Rezepte kommen je Lauf von aussen und stehen bewusst nicht fest im
// Build: Welches Rezept gilt, haengt am konkreten Sprung der jeweiligen
// Dependabot-PR. Die Zuordnung Sprung -> Rezept steht in
// ci/openrewrite-anwenden.sh, nicht hier.
//
//   ./gradlew rewriteDryRun -PrewriteRezepte=org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0
//   ./gradlew rewriteRun    -PrewriteRezepte=rezept.eins,rezept.zwei
//
// Ohne -PrewriteRezepte ist kein Rezept aktiv; rewriteRun aendert dann
// nichts. Das ist Absicht: ein versehentlicher Aufruf schreibt nicht um.
rewrite {
    val rezepte = (project.findProperty("rewriteRezepte") as String?)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    rezepte.forEach { activeRecipe(it) }

    // Ein Rezeptname, den es nicht gibt, bricht den Lauf ab statt ihn
    // stillschweigend auszulassen. Ohne das meldete rewriteRun Erfolg,
    // haette aber nichts getan -- und ein Tippfehler im Katalog von
    // ci/openrewrite-anwenden.sh saehe genauso aus wie "dieser Sprung
    // brauchte keine Aenderung".
    failOnInvalidActiveRecipes = true
}

// --- E2E-Ebene (docs/teststrategie.md, Abschnitt 2.7) ---------------------
//
// Bewusst **nicht** an `check`: Das Zeitbudget dort sind 10 Minuten
// einschliesslich Mutationstests (Abschnitt 10), und ein Durchlauf durch
// einen echten Browser kostet Minuten. Die E2E-Ebene laeuft in der Pipeline
// als eigene Stufe vor dem Deploy -- der Ort, an dem ihre Frage
// ("traegt das gebaute Jar?") ueberhaupt erst sinnvoll ist.
//
// Die Datenbank kommt aus Testcontainers, wie ueberall in diesem Projekt
// (Abschnitt 10); der globale Aufbau in e2e/tests/umgebung.js startet sie
// und die Anwendung davor.
val e2eInstall = tasks.register<Exec>("e2eInstall") {
    workingDir = layout.projectDirectory.dir("e2e").asFile
    commandLine(npm, "install")
    inputs.file(layout.projectDirectory.file("e2e/package.json"))
    outputs.dir(layout.projectDirectory.dir("e2e/node_modules"))
}

tasks.register<Exec>("e2eTest") {
    group = "verification"
    description = "E2E-Ebene: gebautes Jar, echter Browser, zwei Geraete (Abschnitt 2.7)."
    dependsOn(e2eInstall, tasks.named("bootJar"))
    workingDir = layout.projectDirectory.dir("e2e").asFile
    commandLine(npm, "run", "test")
}
