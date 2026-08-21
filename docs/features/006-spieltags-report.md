# 006 — Spieltags-Report (Schnitt 1: eigene Bilanz)

## Anlass
Nach einem Spieltag will ein Tipper auf einen Blick sehen, was seine eigenen
Ergebnistipps wert waren, statt sie Spiel für Spiel im Spielplan
nachzurechnen. Mit diesem Feature kann ein angemeldeter Tipper zu jedem
Spieltag seine eigene Bilanz abrufen — Endergebnis, eigener Tipp und
erreichte Wertungspunkte je gewertetem Spiel, dazu die Spieltagssumme.

Das ist der erste von fünf Schnitten aus der Idee „Spieltags-Report"
(Skill `schneiden`, 2026-08-21). Liga-Vergleich (Spieltagsrangliste im
Report), Platzveränderung in der Saisonrangliste und Highlights sind
eigene, spätere Schnitte — Behelf hier: keiner davon existiert, die Bilanz
zeigt ausschließlich das eigene Konto.

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 13.9-a | neu | Kern: eigene Bilanz je gewertetem Spiel eines Spieltags |
| 13.9-b | neu | noch nicht gewertete Spiele bleiben außen vor |
| 13.9-c | neu | gewertetes, aber ungetipptes Spiel zählt mit 0 Punkten |
| 13.9-d | neu | Spieltagssumme über alle gewerteten Spiele |
| 13.9-e | neu | die Bilanz zeigt ausschließlich das eigene Konto |
| 13.4-b | bestehend | derselbe Ergebnistipp (zwei nicht-negative Zahlen), hier nur gelesen statt abgegeben |
| 13.5-a | bestehend | die Wertung selbst (Scoring) wird wiederverwendet, nicht neu erfunden |

## Akzeptanzkriterien
1. Ein angemeldeter Tipper kann für einen Spieltag seine eigene Bilanz abrufen.
2. Zu jedem gewerteten (FINAL) Spiel des Spieltags zeigt die Bilanz das Endergebnis.
3. Zu jedem gewerteten Spiel zeigt die Bilanz den eigenen Ergebnistipp, falls einer abgegeben wurde.
4. Zu jedem gewerteten Spiel zeigt die Bilanz die daraus erreichten Wertungspunkte (Scoring, 13.5).
5. Ein gewertetes Spiel ohne eigenen Ergebnistipp erscheint in der Bilanz mit 0 Wertungspunkten und ohne eigenen Tipp.
6. Ein noch nicht gewertetes Spiel (Status SCHEDULED oder CANCELLED) ist nicht Teil der Bilanz.
7. Die Bilanz trägt zusätzlich die Summe der Wertungspunkte über alle gewerteten Spiele des Spieltags.
8. Die Bilanz enthält ausschließlich Angaben zum eigenen Konto — keinen fremden Tipp.
9. Die Reihenfolge, in der Spiele getippt oder ausgewertet wurden, spielt für die Bilanz keine Rolle.

## Szenarien

**Die eigene Bilanz zeigt Ergebnis, Tipp und Punkte je Spiel.**
Angenommen ein Spieltag mit einem gewerteten Spiel, zu dem Anna einen
Ergebnistipp abgegeben hat, der die Tendenz, aber nicht das exakte
Ergebnis trifft — wenn Anna ihre Bilanz für diesen Spieltag abruft, dann
zeigt sie für dieses Spiel das Endergebnis, Annas Tipp und 3 oder 5
Wertungspunkte, je nach Abstands-Eimer.

**Ein ungetipptes, aber gewertetes Spiel zählt mit 0 Punkten.**
Angenommen ein Spieltag mit einem gewerteten Spiel, zu dem Anna keinen
Ergebnistipp abgegeben hat — wenn Anna ihre Bilanz abruft, dann erscheint
das Spiel mit 0 Wertungspunkten und ohne eigenen Tipp.

**Ein noch nicht gewertetes Spiel fehlt in der Bilanz.**
Angenommen ein Spieltag mit einem Spiel, das noch nicht angepfiffen wurde
— wenn Anna ihre Bilanz abruft, dann taucht dieses Spiel nicht darin auf.

**Die Bilanz trägt die Spieltagssumme.**
Angenommen ein Spieltag mit zwei gewerteten Spielen, bei denen Anna einmal
das exakte Ergebnis (6 Punkte) und einmal nur die Tendenz (3 Punkte)
getroffen hat — wenn Anna ihre Bilanz abruft, dann zeigt sie eine
Spieltagssumme von 9.

**Die Bilanz zeigt keinen fremden Tipp.**
Angenommen ein Spieltag mit einem gewerteten Spiel, zu dem sowohl Anna als
auch Ben einen Ergebnistipp abgegeben haben — wenn Anna ihre Bilanz
abruft, dann enthält die Antwort Bens Tipp an keiner Stelle.

## Kritikalität

**Stufe:** MEDIUM

Eintrittswahrscheinlichkeit hoch — die Bilanz wird nach jedem Spieltag von
jedem Tipper abgerufen. Schadensausmaß gering: Anders als bei Kriterium
19/20 (`PredictionView`, HIGH) fließt hier keine fremde, verdeckte
Information ein — `matchdayReport` fragt ausschließlich Tipps des
anfragenden Kontos ab, ein fremder Tipp kommt strukturell gar nicht in die
Antwort hinein. Ein Fehler zeigt bestenfalls die eigene, ohnehin bereits
bekannte Bilanz falsch an; er verändert weder Punktestand noch Rangliste
(beide bleiben unverändert von `Scoring`/`Standings` berechnet) und legt
kein fremdes Datum offen.

## Umgesetzt in
- `application/league/view/ReportView.java`
- `application/league/PredictionService.java` (`matchdayReport`)
- `application/league/port/in/PredictionCommands.java`
- `adapter/in/http/PredictionController.java`
- `frontend/src/league/api.js`, `frontend/src/league/ReportScreen.jsx`,
  `frontend/src/league/League.jsx` (Tab „Bilanz"), `frontend/src/styles.css`

## Offene Fragen
Der Mailversand des Reports (Auslöser, Empfängerkreis, Abmeldung) ist
bereits als offene Entscheidung erfasst (`docs/offene-entscheidungen.md`,
Abschnitt Fachlich, seit 2026-08-21) und nicht Teil dieses Schnitts.
