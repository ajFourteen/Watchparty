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
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    // Setzen JSpecify durch: ein @Nullable an der falschen Stelle ist ein
    // Compile-Fehler, keine Doku (ADR-026).
    id("net.ltgt.errorprone") version "4.1.0"
    id("net.ltgt.nullaway") version "3.1.0"
    // Erzeugt den JGiven-HTML-Report aus den JSON-Ergebnissen, die
    // jgiven-junit5 beim Testlauf schreibt (docs/teststrategie.md, Abschnitt 8).
    id("com.tngtech.jgiven.gradle-plugin") version "2.0.3"
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

    // Die Annotationen selbst (ADR-026): @NullMarked, @Nullable. Reine
    // Deklarationen ohne Laufzeitverhalten -- die Durchsetzung macht NullAway.
    implementation("org.jspecify:jspecify:1.0.0")

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
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    // Report- und Szenariowerkzeug der Teststrategie (docs/teststrategie.md).
    // jgiven-junit5 bringt die JUnit5-Erweiterung fuer ScenarioTest mit;
    // jqwik die Property-Tests (Abschnitt 4).
    testImplementation("com.tngtech.jgiven:jgiven-junit5:2.0.3")
    testImplementation("net.jqwik:jqwik:1.9.3")

    // Ab Gradle 9 liegt der Launcher nicht mehr automatisch auf dem
    // Test-Classpath; ohne ihn startet der Test-Executor gar nicht erst.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.13.8")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// --- Ebenen als Gradle-Tasks (docs/teststrategie.md, Abschnitt 1) ----------
//
// Getrennt wird ueber JUnit-Tags, nicht ueber eigene Source Sets: Die
// handgeschriebenen Test Doubles bleiben in einem gemeinsamen Quellbaum
// (src/test/java), erreichbar von jeder Ebene. `test` ist bewusst der
// schnelle Lauf (unit, port, arch); `adapterTest` und `apiTest` kommen extra
// dazu, weil sie Spring bzw. einen echten Socket brauchen (Phase 1 der
// Teststrategie-Umsetzung).
tasks.named<Test>("test") {
    useJUnitPlatform {
        includeTags("unit", "port", "arch")
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

tasks.named("check") {
    dependsOn(adapterTest, apiTest)
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
val jgivenResultsDir = layout.buildDirectory.dir("jgiven-results/alle-ebenen")

tasks.withType<Test>().configureEach {
    extensions.configure<JGivenTaskExtension> {
        resultsDir.set(jgivenResultsDir)
    }
}

tasks.named<JGivenReportTask>("jgivenTestReport") {
    dependsOn(tasks.test, adapterTest, apiTest)
    results.set(jgivenResultsDir)
}

tasks.named("check") {
    dependsOn("jgivenTestReport")
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
// Bewusst (noch) kein Gate: Bei so vielen offenen Regeln waere der Build ab
// Tag eins rot. Scharf gestellt wird das am Ende von Phase 3 der
// Teststrategie-Umsetzung (docs/teststrategie-umsetzung.md, Phase 2).
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
            """^\|\s*([0-9]+(?:\.[0-9]+)?(?:-[a-z])?)\s*\|.*\|\s*(backend|frontend|organisatorisch|beobachtung)\s*\|\s*$""")
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
    }
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
    options.errorprone.isEnabled.set(false)
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
