#!/usr/bin/env bash
# Wendet auf einen Major-Versionssprung das passende OpenRewrite-Rezept an
# (ADR-042).
#
# Bis hierher hat die taegliche Dependabot-Routine einen Major-Sprung von Hand
# nachgezogen: Release Notes lesen, daraus schliessen, was sich geaendert hat,
# den Code entsprechend anfassen. Das ist genau die Stelle, an der ein
# Sprachmodell raet. OpenRewrite dreht die Richtung um -- der Hersteller der
# Bibliothek beschreibt den Umstieg einmal als Rezept, hier wird es nur noch
# ausgefuehrt. Was das Rezept aendert, ist damit reproduzierbar und nicht
# interpretiert.
#
# Der Katalog unten ist die einzige Stelle, die weiss, welcher Sprung welches
# Rezept braucht. Er ist bewusst kurz und ausdruecklich unvollstaendig: Fuer
# npm (frontend/, e2e/) und fuer GitHub-Actions-Tags gibt es keine
# vergleichbaren Rezepte, dort bleibt es bei der Handarbeit der Routine. Ein
# Sprung ohne Eintrag ist deshalb kein Fehler, sondern eine Auskunft (Exit 4).
#
# Aufruf:
#   ci/openrewrite-anwenden.sh [--trocken|--nur-rezepte] < spruenge.txt
#
# Eingabe auf stdin, eine Zeile je Versionssprung der Dependabot-PR:
#   <koordinate> <vonVersion> <nachVersion>
# Beispiel:
#   org.springframework.boot 3.5.16 4.0.1
#   org.testcontainers:postgresql 1.21.3 2.0.0
#
# Optionen:
#   --trocken       rewriteDryRun statt rewriteRun -- schreibt nichts, legt den
#                   Patch unter build/reports/rewrite/rewrite.patch ab
#   --nur-rezepte   nur die aufgeloesten Rezeptnamen ausgeben, kein Gradle-Lauf
#
# Exit-Codes:
#   0  Rezept(e) angewandt bzw. trocken durchgerechnet
#   4  kein Major-Sprung dabei oder kein Rezept im Katalog -- die Routine
#      macht dann weiter wie bisher (Release Notes lesen, von Hand anpassen)
#   1  Fehler (falscher Aufruf, roter Gradle-Lauf)

set -uo pipefail

modus="anwenden"
case "${1:-}" in
    --trocken) modus="trocken" ;;
    --nur-rezepte) modus="nur-rezepte" ;;
    "") ;;
    *) echo "Unbekannte Option: $1" >&2; exit 1 ;;
esac

cd "$(dirname "$0")/.."

# --- Der Katalog ----------------------------------------------------------
#
# Zugeordnet wird ueber die *Gruppe* (den Teil vor dem ersten Doppelpunkt) und
# den Ziel-Major. Das genuegt hier, weil Dependabot fuer das Gradle-Projekt
# entweder die Plugin-ID (org.springframework.boot) oder die volle Koordinate
# (org.springframework.boot:spring-boot-starter-web) meldet -- beide fuehren
# auf dieselbe Gruppe und dasselbe Rezept.
#
# Die Namen sind gegen die tatsaechlich veroeffentlichten Rezeptdateien
# geprueft (rewrite-recipe-bom 3.37.0, siehe build.gradle.kts), nicht aus der
# Dokumentation abgeschrieben. Ein Tippfehler faellt sonst erst im
# Gradle-Lauf auf -- und dank failOnInvalidActiveRecipes dort immerhin rot.
rezepte_fuer() {
    local gruppe="$1" ziel_major="$2"
    case "$gruppe:$ziel_major" in
        # Spring Boot 3 -> 4: das grosse Rezept, es zieht Spring Framework 7,
        # umbenannte Properties und die Jackson-3-Umstellung mit.
        org.springframework.boot:4)
            echo "org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0" ;;
        # Spring Framework einzeln -- nur falls es je ohne Boot springt.
        org.springframework:7)
            echo "org.openrewrite.java.spring.framework.UpgradeSpringFramework_7_0" ;;
        # JUnit 5 -> 6. Betrifft hier ausschliesslich Testcode; die
        # Ebenen-Tasks in build.gradle.kts bleiben unberuehrt.
        org.junit.jupiter:6|org.junit.platform:6|org.junit:6)
            echo "org.openrewrite.java.testing.junit6.JUnit5to6Migration" ;;
        # Testcontainers 1 -> 2 (Adapter-Tests gegen echtes Postgres,
        # docs/teststrategie.md Abschnitt 2.3).
        org.testcontainers:2)
            echo "org.openrewrite.java.testing.testcontainers.Testcontainers2Migration" ;;
        # ArchUnit 0 -> 1. Laengst hinter uns (1.5.0), steht hier trotzdem:
        # Der Katalog soll zeigen, wonach gesucht wird, nicht nur, was gerade
        # ansteht.
        com.tngtech.archunit:1)
            echo "org.openrewrite.java.testing.archunit.ArchUnit0to1Migration" ;;
        # Flyway 9 -> 10 (die Aufteilung in flyway-database-*-Module).
        org.flywaydb:10)
            echo "org.openrewrite.java.flyway.MigrateToFlyway10" ;;
        # Die Java-Version selbst. Kommt nicht von Dependabot, sondern von
        # Hand -- der Katalog kennt sie, damit ein Toolchain-Sprung denselben
        # Weg nimmt wie ein Bibliothekssprung.
        java:25)
            echo "org.openrewrite.java.migrate.UpgradeToJava25" ;;
        *)
            return 1 ;;
    esac
}

# Erste Zahlenkomponente einer Version. Vertraegt fuehrendes "v" (Actions-Tags
# heissen v7, nicht 7) und Zusaetze wie "4.0.0-RC1".
major_von() {
    printf '%s' "$1" | sed -E 's/^[vV]//' | sed -E 's/[^0-9].*$//'
}

rezepte=()
ohne_rezept=()
kein_major=()

while read -r koordinate von nach _rest; do
    [ -z "${koordinate:-}" ] && continue
    case "$koordinate" in \#*) continue ;; esac
    if [ -z "${von:-}" ] || [ -z "${nach:-}" ]; then
        echo "Zeile ohne beide Versionen: $koordinate ${von:-} ${nach:-}" >&2
        exit 1
    fi

    major_von="$(major_von "$von")"
    major_nach="$(major_von "$nach")"
    if [ -z "$major_von" ] || [ -z "$major_nach" ]; then
        echo "Version nicht lesbar: $koordinate $von -> $nach" >&2
        exit 1
    fi

    if [ "$major_von" = "$major_nach" ]; then
        kein_major+=("$koordinate $von -> $nach")
        continue
    fi

    gruppe="${koordinate%%:*}"
    if gefunden="$(rezepte_fuer "$gruppe" "$major_nach")"; then
        while read -r rezept; do
            [ -z "$rezept" ] && continue
            # Doppelte Nennung vermeiden: zwei Artefakte derselben Gruppe in
            # einer PR ergeben ein Rezept, nicht zwei.
            schon_da=""
            for vorhanden in ${rezepte[@]+"${rezepte[@]}"}; do
                [ "$vorhanden" = "$rezept" ] && schon_da="ja"
            done
            [ -z "$schon_da" ] && rezepte+=("$rezept")
        done <<< "$gefunden"
    else
        ohne_rezept+=("$koordinate $von -> $nach (Gruppe $gruppe, Ziel-Major $major_nach)")
    fi
done

if [ ${#kein_major[@]} -gt 0 ]; then
    echo "Kein Major-Sprung, nichts umzuschreiben:"
    printf '  - %s\n' "${kein_major[@]}"
fi

if [ ${#ohne_rezept[@]} -gt 0 ]; then
    echo "Major-Sprung ohne Eintrag im Katalog (bleibt Handarbeit):"
    printf '  - %s\n' "${ohne_rezept[@]}"
fi

if [ ${#rezepte[@]} -eq 0 ]; then
    echo "Kein Rezept anzuwenden."
    exit 4
fi

echo "Rezepte:"
printf '  - %s\n' "${rezepte[@]}"

if [ "$modus" = "nur-rezepte" ]; then
    exit 0
fi

liste="$(IFS=,; printf '%s' "${rezepte[*]}")"
aufgabe="rewriteRun"
[ "$modus" = "trocken" ] && aufgabe="rewriteDryRun"

# -PskipFrontend: Das Rezept fasst Java-, Gradle- und Property-Dateien an, nie
# das Frontend. Der npm-Build waere hier reine Wartezeit.
#
# -x compileJava -x compileTestJava ist keine Bequemlichkeit, sondern die
# Bedingung dafuer, dass dieses Skript ueberhaupt hilft. Die Rezeptaufgabe
# haengt von Haus aus am Kompilieren -- und ein Major-Sprung, der eine
# Anpassung erzwingt, hat den Compiler in aller Regel schon rot gemacht. Ohne
# das Ueberspringen bricht der Lauf mit genau den Fehlern ab, die das Rezept
# beheben soll (belegt am 2026-08-31 an Spring Boot 3.5.16 -> 4.1.1: 31
# Compile-Fehler, Rezeptlauf kam gar nicht erst zum Zug).
#
# Die Typinformationen holt OpenRewrite aus den Jars des Compile-Classpath,
# nicht aus den eigenen .class-Dateien; das Ueberspringen kostet also nichts,
# was das Rezept braucht.
echo
echo "./gradlew $aufgabe -PrewriteRezepte=$liste -PskipFrontend -x compileJava -x compileTestJava"
if ! ./gradlew "$aufgabe" "-PrewriteRezepte=$liste" -PskipFrontend -x compileJava -x compileTestJava; then
    echo "Gradle-Lauf rot -- kein Rezept angewandt." >&2
    exit 1
fi

if [ "$modus" = "trocken" ]; then
    echo
    echo "Trockenlauf. Patch (falls vorhanden): build/reports/rewrite/rewrite.patch"
    exit 0
fi

echo
echo "Vom Rezept geaendert:"
if ! git diff --stat; then
    exit 1
fi
