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
# Aufruf: ci/eine-maschine-pruefen.sh <app-name>

set -euo pipefail

app="${1:?Aufruf: ci/eine-maschine-pruefen.sh <app-name>}"

maschinen="$(flyctl machines list --app "$app" --json)"
gestartet="$(echo "$maschinen" | python3 -c '
import json, sys
maschinen = json.load(sys.stdin)
for m in maschinen:
    if m.get("state") == "started":
        print(f"{m.get(\"id\")}\t{m.get(\"region\")}\t{m.get(\"state\")}")
')"

anzahl="$(echo -n "$gestartet" | grep -c . || true)"

echo "Laufende Maschinen (${anzahl}):"
echo "${gestartet:-  (keine)}" | sed 's/^/  /'

if [ "$anzahl" -ne 1 ]; then
    echo
    echo "Invariante 6 verletzt: genau eine Instanz erwartet, ${anzahl} gefunden."
    echo "Zwei Instanzen sind zwei getrennte Mengen von Watchpartys (ADR-005)."
    echo "Korrektur: flyctl scale count 1 --app ${app}"
    exit 1
fi

echo "✓ Invariante 6 gehalten: genau eine laufende Maschine."
