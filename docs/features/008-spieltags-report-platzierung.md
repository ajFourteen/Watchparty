# 008 — Spieltags-Report: Platzveränderung

## Anlass

Ein Tipper sieht im Report bisher nur die Rangliste des einen Spieltags,
nicht ob er sich in der Saison dadurch verbessert oder verschlechtert hat.
Mit diesem Feature zeigt der Report zusätzlich, ob der Tipper in der
Saison-Rangliste der ausgewählten Liga gegenüber der Vorwoche gestiegen,
gefallen oder gleich geblieben ist.

Das ist der dritte von fünf Schnitten aus der Idee „Spieltags-Report"
(Skill `schneiden`, 2026-08-21; vorige Schnitte:
`docs/features/006-spieltags-report.md`,
`docs/features/007-spieltags-report-liga.md`). Behelf hier: verglichen wird
nur der eigene Platz, nicht der Abstand in Punkten und keine fremde
Platzveränderung — Highlights des Spieltags sind ein eigener, späterer
Schnitt.

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 13.6-l | neu | kumulierte Saison-Rangliste bis einschließlich eines Spieltags — Grundlage für „vorher"/"nachher" |
| 13.9-i | neu | Report zeigt die Platzveränderung des Tippers in der eingeblendeten Liga gegenüber der Vorwoche |
| 13.9-j | neu | am ersten Spieltag der Saison gibt es keine Vorwoche, also keine Platzveränderung |
| 13.6-j | bestehend | die kumulierte Rangliste wird bei jeder Abfrage neu berechnet, kein eingefrorener Stand |
| 13.9-h | bestehend | ohne Liga-Mitgliedschaft bleibt der Report wie in Schnitt 1, auch ohne Platzveränderung |

Die kumulierte Rangliste rechnet, statt zu speichern: Der Platz „vor dem
Spieltag" entsteht aus der Rangliste über die Spieltage < N, der Platz
„nach dem Spieltag" aus der Rangliste über die Spieltage ≤ N — beides bei
jeder Abfrage neu aus dem aktuellen Stand, verträglich mit 13.6-j.

## Akzeptanzkriterien

1. Fragt der Anwendungskern die Saison-Rangliste einer Liga bis
   einschließlich Spieltag N ab, enthält sie ausschließlich Spiele mit
   Spieltagsnummer ≤ N — spätere Spieltage fließen nicht ein, auch wenn sie
   bereits gewertet sind.
2. Zeigt der Report eines Tippers, der Mitglied mindestens einer Liga ist,
   einen Spieltag N > 1, dann zeigt er zusätzlich, ob der Tipper in der
   kumulierten Saison-Rangliste der ausgewählten Liga gegenüber Spieltag
   N-1 gestiegen, gefallen oder gleich geblieben ist.
3. Zeigt der Report Spieltag 1, zeigt er keine Platzveränderung — es gibt
   keine Vorwoche, mit der verglichen werden könnte.
4. Wechselt der Tipper im Report die ausgewählte Liga, bezieht sich die
   angezeigte Platzveränderung auf die neu ausgewählte Liga.
5. Ist der Tipper Mitglied keiner Liga, zeigt der Report weiterhin keine
   Platzveränderung und keinen Fehler (wie in Schnitt 2 ohne
   Liga-Rangliste).

## Szenarien

Kriterium 1 ist die einzige neue Backend-Regel dieses Schnitts und bekommt
ein JGiven-Szenario in `LigenScenarioTest`:

**Die kumulierte Rangliste zählt nur Spieltage bis zur angefragten Nummer.**
Angenommen Anna ist Mitglied einer Liga, die Spiele an den Spieltagen 1 und
2 gewertet hat und Anna beide richtig getippt hat — wenn die kumulierte
Rangliste bis einschließlich Spieltag 1 abgefragt wird, dann zeigt sie nur
die Wertungspunkte aus Spieltag 1, nicht aus Spieltag 2.

Kriterium 2–5 liegen wie bei Schnitt 2 vollständig in der Oberfläche: Sie
verwenden ausschließlich die neue, oben geprüfte Abfrage zweimal (Spieltag
N und Spieltag N-1) und vergleichen die beiden zurückgegebenen Ränge des
eigenen Kontos — kein neuer Anwendungscode, keine neue Zusammenführung.
Zur Nachvollziehbarkeit trotzdem in Prosa:

**Ein Tipper sieht seinen Aufstieg.**
Angenommen Ben liegt nach Spieltag 3 auf Platz 2 einer Liga, nach Spieltag
2 lag er auf Platz 4 — wenn er seinen Report zu Spieltag 3 abruft, dann
zeigt der Report, dass er gestiegen ist.

**Spieltag 1 zeigt keine Platzveränderung.**
Angenommen Anna ruft ihren Report zu Spieltag 1 ab — dann zeigt der Report
keine Platzveränderung, weder Pfeil noch Text zu einer Vorwoche.

Manuell nachvollzogen wie bei Schnitt 2: Kriterium 2–3 durch Durchspielen
mit zwei aufeinanderfolgenden Spieltagen, Kriterium 4 durch Liga-Wechsel im
selben Aufruf, Kriterium 5 mit einem Konto ohne Liga-Mitgliedschaft.

## Kritikalität

**Stufe:** MEDIUM

Dieselbe Einstufung wie Schnitt 1 und 2: Ein Fehler verändert weder
Punktestand noch die zugrunde liegende Rangliste — beide bleiben
unverändert von `Scoring`/`Standings` berechnet, dieser Schnitt liest sie
nur zweimal mit unterschiedlicher Spieltagsgrenze und vergleicht zwei
Rangzahlen. Anders als bei Kriterium 19/20 (`PredictionView`, HIGH) fließt
keine fremde, verdeckte Information ein: Jeder angezeigte Platz war schon
über die bestehende Rangliste einsehbar (Kriterium 31/33) — dieser Schnitt
zeigt nur zusätzlich die Differenz zweier ohnehin sichtbarer Werte.

## Umgesetzt in
- `de.fourteen.watchparty.application.league.port.in.LeagueCommands` —
  `seasonStandingsThroughMatchday`
- `de.fourteen.watchparty.application.league.LeagueService` —
  `seasonStandingsThroughMatchday`
- `de.fourteen.watchparty.adapter.in.http.LeagueController` —
  `GET /api/league/leagues/{leagueId}/standings/season/through/{week}`
- `frontend/src/league/ReportScreen.jsx`
- `frontend/src/league/api.js`

## Offene Fragen
Keine.
