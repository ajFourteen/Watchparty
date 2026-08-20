#!/usr/bin/env bash
# Prueft Invariante 6 nach dem Deploy: genau eine laufende Maschine.
#
# fly.toml stellt die Regel auf (ADR-005/ADR-018) und nennt die Kontrolle
# ausdruecklich Pflicht -- aber nur als Kommentar: "immer mit --ha=false
# deployen und danach `fly machines list` pruefen". Damit hing die haerteste
# Betriebsinvariante des Projekts daran, dass ein Mensch nachsieht.
#
# Zwei Maschinen waeren zwei getrennte Mengen von Watchpartys, mit Sitzungen,
# die zufaellig auf der falschen landen -- und seit ADR-023 zusaetzlich zwei
# getrennte Snapshot-Dateien.
#
# Gezaehlt wird ausschliesslich der Zustand "started": Eine gestoppte Maschine
# bedient keine Verbindung und haelt keinen Raumzustand. auto_start_machines
# ist zwar an (fly.toml), aber eine gestoppte Maschine startet nur auf einen
# Request hin -- sie ist keine zweite Instanz im Sinne von ADR-005.
#
# Aufruf: ci/eine-maschine-pruefen.sh <app-name>

set -euo pipefail

app="${1:?Aufruf: ci/eine-maschine-pruefen.sh <app-name>}"

if ! maschinen="$(flyctl machines list --app "$app" --json)"; then
    echo "flyctl machines list fehlgeschlagen -- Invariante 6 konnte nicht geprueft werden." >&2
    exit 1
fi

# Die Antwort muss ein JSON-Array sein. Faengt ein geaendertes Ausgabeformat
# ab, statt es als "null laufende Maschinen" zu missdeuten.
if [ "$(printf '%s' "$maschinen" | jq -r 'type')" != "array" ]; then
    echo "Unerwartetes Format von 'flyctl machines list --json' (kein Array):" >&2
    printf '%s\n' "$maschinen" | head -5 >&2
    exit 1
fi

gestartet="$(printf '%s' "$maschinen" | jq -r '.[] | select(.state == "started") | "\(.id)\t\(.name)\t\(.region)"')"
anzahl="$(printf '%s' "$maschinen" | jq -r '[.[] | select(.state == "started")] | length')"

echo "Laufende Maschinen (${anzahl} von $(printf '%s' "$maschinen" | jq -r 'length') insgesamt):"
if [ "$anzahl" -eq 0 ]; then
    echo "  (keine)"
else
    printf '%s\n' "$gestartet" | sed 's/^/  /'
fi

if [ "$anzahl" -ne 1 ]; then
    echo
    echo "Invariante 6 verletzt: genau eine Instanz erwartet, ${anzahl} gefunden."
    echo "Zwei Instanzen sind zwei getrennte Mengen von Watchpartys (ADR-005)."
    echo "Korrektur: flyctl scale count 1 --app ${app}"
    exit 1
fi

echo "✓ Invariante 6 gehalten: genau eine laufende Maschine."
