#!/usr/bin/env bash
# PostToolUse-Hook: beobachtet nach einem Push die GitHub-Actions-Laeufe.
#
# Laeuft im Hintergrund (async) und meldet sich nur, wenn etwas schiefgeht:
# Exit 2 weckt das Modell mit der Ausgabe dieses Skripts, Exit 0 bleibt still.
# Ein gruener Lauf soll niemanden stoeren.
#
# Bewusst wird NUR bei einem echten Fehlschlag geweckt. Ein nicht erreichbares
# Netz, ein fehlender Lauf oder eine Zeitgrenze sind keine Pipeline-Fehler --
# dafuer zu wecken waere Rauschen, und ein Beobachter, der staendig faelschlich
# anschlaegt, wird abgeschaltet.

set -uo pipefail

eingabe="$(cat)"
befehl="$(printf '%s' "$eingabe" | jq -r '.tool_input.command // empty')"
echo "$befehl" | grep -Eq '(^|[;&|] *)git +(-[^ ]+ +)*push' || exit 0

cd "${CLAUDE_PROJECT_DIR:-$PWD}" 2>/dev/null || exit 0

herkunft="$(git remote get-url origin 2>/dev/null)" || exit 0
repo="$(printf '%s' "$herkunft" | sed -E 's#^.*github\.com[:/]##; s#\.git$##')"
[ -z "$repo" ] && exit 0
sha="$(git rev-parse HEAD 2>/dev/null)" || exit 0

# Ein Token hebt das Limit von 60 Anfragen je Stunde auf; ohne eines wird
# seltener gepollt, damit ein Lauf nicht mittendrin ins Limit faellt.
kopfzeilen=(-H "Accept: application/vnd.github+json")
takt=30
if [ -n "${GH_TOKEN:-${GITHUB_TOKEN:-}}" ]; then
    kopfzeilen+=(-H "Authorization: Bearer ${GH_TOKEN:-$GITHUB_TOKEN}")
    takt=15
fi

api() {
    curl -s --max-time 20 "${kopfzeilen[@]}" "https://api.github.com/repos/$repo/$1"
}

# Auf den Start warten: Zwischen Push und sichtbarem Lauf vergehen Sekunden.
gefunden=0
for _ in $(seq 1 10); do
    if api "actions/runs?per_page=20&head_sha=$sha" | jq -e '.workflow_runs | length > 0' >/dev/null 2>&1; then
        gefunden=1
        break
    fi
    sleep 15
done
[ "$gefunden" -eq 0 ] && exit 0

ende=$((SECONDS + 1500))
while [ $SECONDS -lt $ende ]; do
    antwort="$(api "actions/runs?per_page=20&head_sha=$sha")"
    printf '%s' "$antwort" | jq -e '.workflow_runs' >/dev/null 2>&1 || { sleep "$takt"; continue; }

    offen="$(printf '%s' "$antwort" | jq -r '[.workflow_runs[] | select(.status != "completed")] | length')"
    if [ "$offen" -gt 0 ]; then
        sleep "$takt"
        continue
    fi

    fehlgeschlagen="$(printf '%s' "$antwort" | jq -r '
        [.workflow_runs[] | select(.conclusion != "success" and .conclusion != "skipped")]')"
    anzahl="$(printf '%s' "$fehlgeschlagen" | jq -r 'length')"
    [ "$anzahl" -eq 0 ] && exit 0

    # Ab hier steht fest: mindestens ein Lauf ist rot. Die Meldung nennt den
    # fehlgeschlagenen Schritt, nicht nur den Lauf -- sonst beginnt die
    # Fehlersuche mit dem Nachschlagen, das dieses Skript schon erledigt hat.
    echo "Pipeline fehlgeschlagen fuer ${sha:0:8} in $repo:"
    echo
    for lauf_id in $(printf '%s' "$fehlgeschlagen" | jq -r '.[].id'); do
        printf '%s' "$fehlgeschlagen" | jq -r --arg id "$lauf_id" '
            .[] | select(.id == ($id | tonumber))
            | "  Lauf: \(.name) -> \(.conclusion)\n  \(.html_url)"'
        api "actions/runs/$lauf_id/jobs" | jq -r '
            .jobs[]
            | select(.conclusion != "success" and .conclusion != "skipped")
            | "    Job \(.name) -> \(.conclusion)",
              (.steps[]? | select(.conclusion == "failure") | "      Schritt \(.number). \(.name)")'
        echo
    done
    exit 2
done

exit 0
