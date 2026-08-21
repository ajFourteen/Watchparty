# 001 — Wake Lock

## Anlass

Handys sperren den Bildschirm nach kurzer Inaktivität. Das trifft zwei
Stellen: Der aktuelle Host verliert nach ADR-021 sofort seine Rolle, sobald
die Verbindung wegbricht — ein gesperrtes Handy kann das auslösen, ohne
dass jemand es bemerkt. Und jeder Spieler mit gesperrtem Bildschirm
riskiert, ein Wettfenster zu verpassen und in die Nicht-Tipper-Strafe
(8.1) zu laufen. Als Idee in `offene-entscheidungen.md` notiert, seit
ADR-021 dort verlinkt.

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 10.1 | bestehend | Host-Rolle |
| 10.1-d | neu | Marke `frontend` |

Kein Effekt auf 8.1 selbst, nur auf die Wahrscheinlichkeit, dass sie
zuschlägt.

## Akzeptanzkriterien

1. Solange ein Spieler beigetreten ist (`joined`), fordert die Oberfläche
   einen Screen Wake Lock an, sofern der Browser die API kennt.
2. Verliert das Dokument die Sichtbarkeit (Tab/Bildschirm in den
   Hintergrund) und kommt zurück, wird der Lock erneut angefordert — die
   Spezifikation gibt ihn beim Verstecken automatisch frei, das ist kein
   Fehlerfall.
3. Kennt der Browser die API nicht oder schlägt die Anfrage fehl (z. B.
   wenig Akku, ältere iOS-Version), bleibt die App uneingeschränkt
   nutzbar — best effort, kein Fehler sichtbar für den Spieler.
4. Verlässt der Spieler den Raum (RESET, `joined` wird `false`) oder wird
   die App geschlossen, wird ein gehaltener Lock freigegeben.

## Szenarien

Bewusst in Prosa, nicht als JGiven-Szenario: Die Wake-Lock-API lässt sich
nicht sinnvoll gegen eine echte Bildschirmsperre testen, und
`teststrategie.md` §11 nennt Wake Lock ausdrücklich als etwas, das *am
Spielabend beobachtet*, nicht automatisiert geprüft wird — dieselbe
Ausnahme wie fürs übrige Frontend (§11, erster Punkt). Die vier
Akzeptanzkriterien oben sind das Äquivalent, von Hand nachvollzogen:
Kriterium 1–2 im Chrome-DevTools-Tab "Sensors" (Wake-Lock-Status
sichtbar), Kriterium 3 durch Deaktivieren von `navigator.wakeLock` in der
Konsole, Kriterium 4 durch RESET aus einem zweiten Tab.

Am echten Spielabend zeigt sich erst die eigentliche Frage — siehe
`probelauf.md`, Abschnitt „Handys": Wandert die Host-Rolle noch
unbemerkt weg, oder hilft der Lock spürbar?

## Kritikalität

**Stufe:** LOW

Reiner Komfortgewinn, best effort, kein Einfluss auf
Punkteverrechnung oder Serverzustand (Invarianten 1–6 aus `CLAUDE.md`
bleiben unberührt — das ist eine rein clientseitige Änderung). Schlägt die
Anfrage fehl, verhält sich die App exakt wie vorher.

## Umgesetzt in

`frontend/src/useWakeLock.js`, verdrahtet in `frontend/src/App.jsx`.

## Offene Fragen

Ob der Lock tatsächlich verhindert, dass die Host-Rolle am Spielabend
unbemerkt wandert, ist keine Frage, die sich am Schreibtisch beantworten
lässt — Beobachtung steht in `probelauf.md`.
