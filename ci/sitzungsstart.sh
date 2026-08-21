#!/usr/bin/env bash
# SessionStart-Hook: speist zu Sitzungsbeginn ein, was am Schreibtisch gerade
# gilt -- Arbeitsstand und die offenen Entscheidungen.
#
# Der Grund steht in CLAUDE.md: "Was noch nicht entschieden ist, steht in
# docs/offene-entscheidungen.md -- dort bitte nichts stillschweigend
# festlegen, sondern nachfragen." Diese Regel funktioniert nur, wenn der
# Agent weiss, was drinsteht. Bis hierher musste er die Datei erst lesen --
# also genau dann, wenn er ohnehin schon vermutet, dass eine Frage offen ist.
# Das ist die falsche Reihenfolge: Der Zweck der Datei ist, ein
# stillschweigendes Festlegen zu verhindern, und wer stillschweigend
# festlegt, schlaegt vorher nicht nach.
#
# Vorgeschlagen im Prozess-Audit vom 2026-08-20, gebaut am 2026-08-21.
#
# Bewusst knapp: Ueberschrift je offenem Punkt, nicht der ganze Text. Wer
# mehr braucht, liest die Datei -- ein Hook, der bei jedem Sitzungsstart
# siebzig Zeilen einspeist, wird ueberlesen.

set -uo pipefail

cd "${CLAUDE_PROJECT_DIR:-$PWD}" 2>/dev/null || exit 0
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

zweig="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
schmutzig="$(git status --porcelain | wc -l | tr -d ' ')"
if [ "$schmutzig" = "0" ]; then
    baum="sauber"
else
    baum="$schmutzig geaenderte Datei(en) -- siehe git status"
fi

# Stand zum Upstream: dieselbe Groesse, an der git-regeln-hook.sh den Commit
# ablehnt (Pull vor Commit). Fehlt der Upstream, wird nichts behauptet.
if git rev-parse --abbrev-ref '@{upstream}' >/dev/null 2>&1; then
    hinter="$(git rev-list --count 'HEAD..@{upstream}' 2>/dev/null || echo 0)"
    vor="$(git rev-list --count '@{upstream}..HEAD' 2>/dev/null || echo 0)"
    upstream="$vor Commit(s) vor, $hinter hinter dem Upstream"
    [ "$hinter" != "0" ] && upstream="$upstream -- vor dem naechsten Commit pullen"
else
    upstream="kein Upstream gesetzt"
fi

commits="$(git log --format='  %h %s' -5 2>/dev/null)"

datei="docs/offene-entscheidungen.md"
if [ -f "$datei" ]; then
    # Nur die Eintragsueberschriften zwischen "## Fachlich" und dem Abschnitt
    # der bewusst ausgeschlossenen Punkte -- Letztere sind keine offenen Fragen.
    offen="$(awk '
        /^## Nicht offen/ { drin = 0 }
        drin && /^\*\*/    { zeile = $0
                             sub(/^\*\*/, "", zeile); sub(/\*\*.*$/, "", zeile)
                             print "  - " zeile ((abschnitt != "") ? " (" abschnitt ")" : "") }
        /^## Fachlich/     { drin = 1; abschnitt = "fachlich"; next }
        /^## Technisch/    { drin = 1; abschnitt = "technisch"; next }
    ' "$datei")"
    # Die Ausschluesse stehen als Liste ("- **(Live-Wetten)** ..."), die
    # offenen Punkte darueber als Absatzueberschrift ("**Titel.**") -- daher
    # zwei verschiedene Muster.
    ausgeschlossen="$(awk '/^## Nicht offen/ { drin = 1; next } drin && /^- \*\*/ { n++ } END { print n + 0 }' "$datei")"
else
    offen=""
    ausgeschlossen="0"
fi

[ -z "$offen" ] && offen="  (keine)"

text="Arbeitsstand (SessionStart-Hook, ci/sitzungsstart.sh)

Zweig: $zweig | Arbeitsbaum: $baum | $upstream

Letzte Commits:
$commits

Offene Entscheidungen ($datei) -- hier nichts stillschweigend festlegen,
sondern nachfragen (CLAUDE.md):
$offen

Dazu $ausgeschlossen bewusst ausgeschlossene Punkte im selben Dokument --
bevor eine davon wieder aufgemacht wird, dort nachlesen."

jq -n --arg text "$text" '{
    hookSpecificOutput: {
        hookEventName: "SessionStart",
        additionalContext: $text
    }
}'
