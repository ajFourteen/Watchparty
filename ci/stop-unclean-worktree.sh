#!/usr/bin/env bash
# Stop-Hook: erinnert nicht nur, sondern handelt -- wenn der Arbeitsbaum am
# Ende einer Antwort nicht sauber ist, wird der Agent gezwungen zu pruefen,
# ob die begonnene Aenderung abgeschlossen ist, und im Erfolgsfall selbst zu
# committen und zu pushen (Vorgabe des Projektinhabers, 2026-08-20).
#
# Blockiert nur EINMAL je Stop-Versuch (stop_hook_active) -- sonst haengt der
# Agent in einer Schleife, wenn er sich bewusst gegen ein Commit entscheidet,
# etwa weil die Arbeit erkennbar noch nicht fertig ist.
#
# Das eigentliche Commit/Push fuehrt der Agent im naechsten Zug selbst ueber
# die bestehenden Bash-Werkzeuge aus -- git-regeln-hook.sh (Pull-vor-Commit)
# und pipeline-beobachten.sh (Nachbeobachtung) greifen dabei wie gewohnt.

set -uo pipefail

eingabe="$(cat)"
aktiv="$(printf '%s' "$eingabe" | jq -r '.stop_hook_active // false')"
[ "$aktiv" = "true" ] && exit 0

cd "${CLAUDE_PROJECT_DIR:-$PWD}" 2>/dev/null || exit 0
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

[ -z "$(git status --porcelain)" ] && exit 0

jq -n '{
    decision: "block",
    reason: "Der Arbeitsbaum ist nicht sauber. Pruefe: Ist die begonnene Aenderung fachlich abgeschlossen? Wenn ja: fuehre `./gradlew check` vollstaendig aus; laeuft er gruen, committe die Aenderung mit einer Conventional-Commits-Message und pushe (git-regeln-hook.sh prueft dabei automatisch, dass zuvor gepullt wurde). Laeuft check rot, behebe die Ursache oder beende die Antwort ohne zu committen und nenne dem Nutzer den Grund. Ist die Arbeit erkennbar noch nicht fertig (Zwischenstand, naechster Schritt offen), committe NICHT und stoppe normal."
}'
