#!/usr/bin/env bash
# Prueft Commit-Betreffzeilen gegen Conventional Commits.
#
# Der Grund ist kein Stil, sondern das Deployment: Semantic Release (ADR-019)
# leitet aus dem Typ ab, ob ueberhaupt ein Release entsteht -- und damit, ob
# deployed wird. Ein "fixed:" statt "fix:" wird stillschweigend ignoriert: kein
# Release, kein Deploy, keine Fehlermeldung. Der Fix liegt auf main und geht
# nie live. Genau diesen stillen Ausfall faengt dieses Skript ab.
#
# Die erlaubten Typen sind die des Angular-Presets, das
# @semantic-release/commit-analyzer per Default verwendet -- die Liste hier ist
# also keine eigene Konvention, sondern die Menge, die das Release-Werkzeug
# tatsaechlich versteht.
#
# Aufruf: ci/commit-format-pruefen.sh <basis-ref> <kopf-ref>
#         ci/commit-format-pruefen.sh              (ohne Argumente: HEAD allein)

set -euo pipefail

typen="build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test"
muster="^(${typen})(\([a-z0-9./-]+\))?!?: .+"

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
    fi
done < <(git log --format='%H%x09%P%x09%s' $bereich)

if [ "$fehler" -gt 0 ]; then
    echo
    echo "${fehler} Commit-Betreff(e) folgen nicht Conventional Commits."
    echo "Erlaubte Typen: ${typen//|/, }"
    echo "Beispiel: fix: Verbindungsstatus ganz unten platzieren"
    echo
    echo "Ein nicht erkannter Typ bedeutet: kein Release und kein Deploy (ADR-019)."
    exit 1
fi

echo "Commit-Format: alle geprueften Betreffzeilen sind gueltig."
