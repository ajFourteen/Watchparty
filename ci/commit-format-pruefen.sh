#!/usr/bin/env bash
# Prueft Commit-Betreffzeilen gegen Conventional Commits -- und, seit dem
# Prozess-Audit vom 2026-08-21, gegen eine engere Fassung von /freigabe:
# Ein releasender Typ ohne jede Anwendungsdatei im Commit ist mit hoher
# Sicherheit falsch getippt.
#
# Der Grund ist kein Stil, sondern das Deployment: Semantic Release (ADR-019)
# leitet aus dem Typ ab, ob ueberhaupt ein Release entsteht -- und damit, ob
# deployed wird. Ein "fixed:" statt "fix:" wird stillschweigend ignoriert: kein
# Release, kein Deploy, keine Fehlermeldung. Der Fix liegt auf main und geht
# nie live. Ein "feat:"/"fix:"/"perf:" auf eine reine Doku-/Tooling-Aenderung
# ist der umgekehrte Fehler: ein Release und ein Deploy, die inhaltlich nichts
# an der laufenden Anwendung aendern -- genau der Fehler, der in der Sitzung
# vom 2026-08-21 zweimal passiert ist (Commits f6e7fcf, cb3e298). Beides faengt
# dieses Skript ab.
#
# Die erlaubten Typen sind die des Angular-Presets, das
# @semantic-release/commit-analyzer per Default verwendet -- die Liste hier ist
# also keine eigene Konvention, sondern die Menge, die das Release-Werkzeug
# tatsaechlich versteht. Von denen loesen nur feat/fix/perf ueberhaupt einen
# Release aus; das ist die "releasende" Teilmenge unten.
#
# Die Anwendungspfade sind bewusst eng: alles, dessen Aenderung sich auf das
# tatsaechlich deployte Artefakt auswirken kann. GitHub-Actions-Workflows
# gehoeren nicht dazu -- eine Aenderung dort, die einen Release verdient,
# heisst treffender "ci:" (ein eigener, nicht-releasender Typ) oder traegt
# zusaetzlich eine echte Anwendungsdatei.
#
# Aufruf: ci/commit-format-pruefen.sh <basis-ref> <kopf-ref>
#         ci/commit-format-pruefen.sh              (ohne Argumente: HEAD allein)
#         ci/commit-format-pruefen.sh <nachrichtendatei>   (commit-msg-Hook)
#
# Der dritte Aufruf ist fuer .githooks/commit-msg: Bislang fiel ein falscher
# Typ erst nach dem Push auf, wenn build.yml schon rot lief -- derselbe
# Regelkern greift jetzt schon lokal, bevor der Commit ueberhaupt entsteht.

set -euo pipefail

typen="build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test"
muster="^(${typen})(\([a-z0-9./-]+\))?!?: .+"
releasende_typen="feat|fix|perf"
anwendungspfade='^(src/main/|frontend/src/|frontend/package(-lock)?\.json$|build\.gradle\.kts$|settings\.gradle\.kts$|Dockerfile$|fly\.toml$)'

# Wahr (Exit 0), wenn der Typ releasend ist, aber keine der uebergebenen
# Dateien eine Anwendungsdatei ist -- der Fall, den /freigabe von Hand
# abfangen sollte und der zweimal durchgerutscht ist.
releasender_typ_ohne_anwendungsaenderung() {
    local typ="$1"
    shift
    echo "$typ" | grep -Eq "^(${releasende_typen})\$" || return 1
    local datei
    for datei in "$@"; do
        echo "$datei" | grep -Eq "$anwendungspfade" && return 1
    done
    return 0
}

# commit-msg-Hook-Modus: einziges Argument ist eine vorhandene Datei -- die
# noch nicht erstellte Commit-Nachricht, kein Git-Ref, den git log verstehen
# wuerde. git-log-Modus (unten) bekommt nie ein Argument, das eine Datei ist.
if [ $# -eq 1 ] && [ -f "$1" ]; then
    betreff="$(head -n1 "$1")"
    if ! echo "$betreff" | grep -Eq "$muster"; then
        echo "  ✗ ${betreff}"
        echo
        echo "Commit-Betreff folgt nicht Conventional Commits."
        echo "Erlaubte Typen: ${typen//|/, }"
        echo "Beispiel: fix: Verbindungsstatus ganz unten platzieren"
        echo
        echo "Ein nicht erkannter Typ bedeutet: kein Release und kein Deploy (ADR-019)."
        exit 1
    fi

    typ="$(echo "$betreff" | grep -Eo '^[a-z]+')"
    dateien=()
    while IFS= read -r datei; do
        dateien+=("$datei")
    done < <(git diff --cached --name-only)

    if releasender_typ_ohne_anwendungsaenderung "$typ" "${dateien[@]}"; then
        echo "  ✗ ${betreff}"
        echo
        echo "Typ \"${typ}:\" loest laut Semantic Release einen Release und ein"
        echo "Deploy aus -- keine der geaenderten Dateien wirkt sich aber auf die"
        echo "Anwendung aus (src/main, frontend/src, build.gradle.kts, Dockerfile,"
        echo "fly.toml, ...). Vermutlich der falsche Typ: docs:/chore:/test:/"
        echo "refactor: pruefen (Skill /freigabe)."
        exit 1
    fi

    echo "Commit-Format: gueltig."
    exit 0
fi

# Loest sich die Basis nicht auf (flacher Klon, erster Push eines Branches,
# ein Force-Push, der den alten Stand entfernt hat), wird nur HEAD geprueft --
# lieber weniger pruefen als den Build an einer Referenz scheitern lassen, die
# mit dem Commit-Format nichts zu tun hat.
if [ $# -eq 2 ] && git rev-parse --verify --quiet "$1^{commit}" >/dev/null && [ "$1" != "0000000000000000000000000000000000000000" ]; then
    bereich="$1..$2"
else
    bereich="-1 HEAD"
fi

# %s ist die Betreffzeile, %P die Eltern-Commits. Merge-Commits (mehr als ein
# Elternteil) bleiben aussen vor: Ihre Betreffzeile erzeugt "Merge branch ..."
# und wird von Semantic Release ohnehin nicht als Release-ausloesend gewertet.
fehler=0
while IFS=$'\t' read -r hash eltern betreff; do
    [ -z "$hash" ] && continue
    if [ "$(echo "$eltern" | wc -w)" -gt 1 ]; then
        continue
    fi
    if ! echo "$betreff" | grep -Eq "$muster"; then
        echo "  ✗ ${hash:0:8}  ${betreff}"
        fehler=$((fehler + 1))
        continue
    fi

    typ="$(echo "$betreff" | grep -Eo '^[a-z]+')"
    dateien=()
    while IFS= read -r datei; do
        dateien+=("$datei")
    done < <(git diff-tree --no-commit-id --name-only -r "$hash")

    if releasender_typ_ohne_anwendungsaenderung "$typ" "${dateien[@]}"; then
        echo "  ✗ ${hash:0:8}  ${betreff}  (releasender Typ, keine Anwendungsdatei geaendert)"
        fehler=$((fehler + 1))
    fi
done < <(git log --format='%H%x09%P%x09%s' $bereich)

if [ "$fehler" -gt 0 ]; then
    echo
    echo "${fehler} Commit(s) verletzen die Commit-Konventionen dieses Projekts."
    echo "Erlaubte Typen: ${typen//|/, }"
    echo "Beispiel: fix: Verbindungsstatus ganz unten platzieren"
    echo
    echo "Ein nicht erkannter Typ, oder ein releasender Typ ohne Anwendungsdatei,"
    echo "bedeutet: falsches Release-/Deploy-Verhalten (ADR-019, Skill /freigabe)."
    exit 1
fi

echo "Commit-Format: alle geprueften Betreffzeilen sind gueltig."
