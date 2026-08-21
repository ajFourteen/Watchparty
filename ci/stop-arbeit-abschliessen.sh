#!/usr/bin/env bash
# Stop-Hook: erinnert nicht nur, sondern handelt -- am Ende einer Antwort wird
# der Agent gezwungen zu pruefen, ob die begonnene Aenderung abgeschlossen
# ist, und im Erfolgsfall selbst zu committen UND ZU PUSHEN (Vorgabe des
# Projektinhabers, 2026-08-20).
#
# Zwei Faelle, weil die Regel zwei Schritte hat:
#
#   1. Arbeitsbaum nicht sauber -> Aenderung pruefen, ggf. committen, pushen.
#   2. Arbeitsbaum sauber, aber Commits liegen vor dem Upstream -> pushen.
#
# Fall 2 ist am 2026-08-21 dazugekommen. Bis dahin pruefte der Hook nur den
# Arbeitsbaum und stieg bei einem sauberen sofort aus -- acht fertige,
# ungepushte Commits waren fuer ihn unsichtbar, obwohl genau das die Regel
# verletzt, die er durchsetzen soll. Derselbe Fehlerschnitt wie beim
# commit-msg-Hook einen Tag zuvor: Die Regel war weiter formuliert als ihre
# Pruefung. Der Dateiname hiess deshalb frueher stop-unclean-worktree.sh und
# war nach dem Umbau nur noch die halbe Wahrheit.
#
# Blockiert nur EINMAL je Stop-Versuch (stop_hook_active) -- sonst haengt der
# Agent in einer Schleife, wenn er sich bewusst gegen Commit oder Push
# entscheidet, etwa weil die Arbeit erkennbar noch nicht fertig ist.
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

schmutzig="$(git status --porcelain)"

# Ohne Upstream gibt es nichts zu pushen -- dann bleibt nur Fall 1.
if git rev-parse --abbrev-ref '@{upstream}' >/dev/null 2>&1; then
    vor="$(git rev-list --count '@{upstream}..HEAD' 2>/dev/null || echo 0)"
else
    vor="0"
fi

[ -z "$schmutzig" ] && [ "$vor" = "0" ] && exit 0

if [ -n "$schmutzig" ]; then
    jq -n '{
        decision: "block",
        reason: "Der Arbeitsbaum ist nicht sauber. Pruefe: Ist die begonnene Aenderung fachlich abgeschlossen? Wenn ja: fuehre `./gradlew check` vollstaendig aus; laeuft er gruen, committe die Aenderung mit einer Conventional-Commits-Message und pushe anschliessend -- beides gehoert zusammen, ein fertiger Commit, der lokal liegen bleibt, ist nicht fertig geliefert (git-regeln-hook.sh prueft dabei automatisch, dass zuvor gepullt wurde). Laeuft check rot, behebe die Ursache oder beende die Antwort ohne zu committen und nenne dem Nutzer den Grund. Ist die Arbeit erkennbar noch nicht fertig (Zwischenstand, naechster Schritt offen), committe NICHT und stoppe normal."
    }'
else
    jq -n --arg vor "$vor" '{
        decision: "block",
        reason: ("Der Arbeitsbaum ist sauber, aber " + $vor + " Commit(s) liegen vor dem Upstream. Fertige Arbeit gehoert gepusht, nicht dem Nutzer als Frage angeboten (Vorgabe des Projektinhabers, 2026-08-20). Pushe jetzt. Bedenke dabei, was der Push ausloest: Ein releasender Typ (feat:/fix:/perf:) unter den Commits startet Semantic Release und damit den Deploy auf Fly.io (ADR-019, Skill freigabe) -- reine docs:/chore:/test:-Commits nur den Build. Willst du bewusst nicht pushen, weil die Reihe noch nicht abgeschlossen ist, stoppe normal und nenne dem Nutzer den Grund.")
    }'
fi
