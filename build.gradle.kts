import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.nullaway.nullaway

plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    // Setzen JSpecify durch: ein @Nullable an der falschen Stelle ist ein
    // Compile-Fehler, keine Doku (ADR-026).
    id("net.ltgt.errorprone") version "4.1.0"
    id("net.ltgt.nullaway") version "3.1.0"
}

group = "de.fourteen"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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

    // Ohne Mockito: Test Doubles werden von Hand geschrieben (ADR-025). Der
    // Ausschluss macht daraus eine Regel statt einer Absprache -- ein
    // versehentliches mock(...) kompiliert gar nicht erst.
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }

    // Haelt die Ringregel aus ADR-024 als Test fest.
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // Ab Gradle 9 liegt der Launcher nicht mehr automatisch auf dem
    // Test-Classpath; ohne ihn startet der Test-Executor gar nicht erst.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.13.8")
}

tasks.withType<Test> {
    useJUnitPlatform()
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

val npmInstall by tasks.registering(Exec::class) {
    workingDir = frontendDir.asFile
    commandLine(npm, "install")
    inputs.file(frontendDir.file("package.json"))
    outputs.dir(frontendDir.dir("node_modules"))
}

val npmBuild by tasks.registering(Exec::class) {
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
