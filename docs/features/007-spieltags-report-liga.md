# 007 — Spieltags-Report: Liga-Rangliste (Schnitt 2)

## Anlass

Ein Tipper sieht seine Spieltags-Bilanz bisher nur allein — wie seine Liga
denselben Spieltag getippt hat, erfährt er erst nach einem Wechsel in die
Liga-Detailseite. Mit diesem Feature blendet die Bilanz die
Spieltagsrangliste einer seiner Ligen direkt daneben ein.

Das ist der zweite von fünf Schnitten aus der Idee „Spieltags-Report"
(Skill `schneiden`, 2026-08-21; erster Schnitt: `docs/features/006-spieltags-report.md`).
Behelf hier: Einzelne fremde Ergebnistipps bleiben weiterhin außerhalb des
Reports — die bleiben im `MatchdayScreen`, dieser Schnitt zeigt nur die
bereits zusammengefasste Rangliste. Platzveränderung in der Saisonrangliste
und Highlights sind eigene, spätere Schnitte.

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 13.9-f | neu | Report zeigt zusätzlich die Spieltagsrangliste einer Liga des Tippers |
| 13.9-g | neu | bei mehreren Ligen wählt der Tipper, welche angezeigt wird |
| 13.9-h | neu | ohne Liga-Mitgliedschaft bleibt der Report wie in Schnitt 1 |
| 13.6-h | bestehend | die Rangliste je Spieltag selbst wird wiederverwendet, nicht neu berechnet |
| 13.6-i | bestehend | zur Auswahl stehen nur Ligen, in denen der Tipper Mitglied ist |

## Akzeptanzkriterien

1. Ruft ein Tipper, der Mitglied mindestens einer Liga ist, seine Bilanz zu
   einem Spieltag ab, zeigt sie zusätzlich die Spieltagsrangliste einer
   seiner Ligen für denselben Spieltag.
2. Ist der Tipper Mitglied mehrerer Ligen, kann er auswählen, welche
   Liga-Rangliste eingeblendet wird; ohne eigene Auswahl zeigt der Report
   eine davon vor.
3. Ist der Tipper Mitglied keiner Liga, bleibt der Report unverändert wie
   in Schnitt 1: kein Ligavergleich, keine Fehlermeldung.
4. Wechselt der Tipper im Report den Spieltag, aktualisiert sich die
   eingeblendete Liga-Rangliste auf denselben Spieltag.
5. Wechselt der Tipper die ausgewählte Liga, bleibt der angezeigte Spieltag
   unverändert.
6. Die eingeblendete Rangliste zeigt ausschließlich die bereits
   zusammengefasste Wertung je Mitglied (Rang, Wertungspunkte, exakte
   Ergebnisse, richtige Tendenzen) — keinen einzelnen Ergebnistipp eines
   anderen Mitglieds.
7. Die eigene Zeile in der eingeblendeten Rangliste ist hervorgehoben, wie
   in der Liga-Detailseite.

## Szenarien

Alle sieben Kriterien liegen vollständig in der Oberfläche: Backend-seitig
wird ausschließlich die bereits bestehende und geprüfte
`matchdayStandings`-Abfrage wiederverwendet (Kriterium 33/35 aus Feature
005, `LeagueServiceTest`/`LeagueHttpFlowTest`), unverändert und ohne neue
Zusammenführung. Es entsteht kein neuer Anwendungs- oder Domänencode, also
auch kein neues JGiven-Szenario — dieselbe Lage wie bei den
Oberflächen-Kriterien in `docs/features/002-ui-ueberarbeitung.md`
(`teststrategie.md` §11, außerhalb der Backend-Teststrategie).

Zur Nachvollziehbarkeit trotzdem in Prosa:

**Ein Tipper mit einer Liga sieht deren Spieltagsrangliste im Report.**
Angenommen Anna ist Mitglied einer Liga mit gewerteten Spielen an einem
Spieltag — wenn sie ihre Bilanz zu diesem Spieltag abruft, dann zeigt der
Report zusätzlich zur eigenen Bilanz die Spieltagsrangliste dieser Liga.

**Mehrere Ligen lassen sich auswählen.**
Angenommen Anna ist Mitglied zweier Ligen — wenn sie im Report die andere
Liga auswählt, dann zeigt der Report deren Spieltagsrangliste für denselben
Spieltag.

**Ohne Liga bleibt der Report wie zuvor.**
Angenommen Ben ist Mitglied keiner Liga — wenn er seine Bilanz abruft, dann
zeigt der Report keine Liga-Rangliste und keinen Fehler.

Manuell nachvollzogen wie bei 001/002: Kriterium 1–3 und 6–7 durch
Durchspielen mit zwei Konten (eines in keiner, eines in zwei Ligen),
Kriterium 4–5 durch Wechsel von Spieltag und Liga-Auswahl im selben
Aufruf.

## Kritikalität

**Stufe:** MEDIUM

Dieselbe Einstufung wie Schnitt 1 und aus demselben Grund: Ein Fehler
verändert weder Punktestand noch eine Rangliste — beide bleiben unverändert
von `Scoring`/`Standings` berechnet, hier nur unverändert angezeigt. Anders
als bei Kriterium 19/20 (`PredictionView`, HIGH) fließt keine fremde,
verdeckte Information ein: Die Spieltagsrangliste einer Liga ist für jedes
Mitglied ohnehin schon über die Liga-Detailseite einsehbar (Kriterium
33/35) — dieser Schnitt ändert nur, *wo* sie zusätzlich sichtbar ist, nicht
*wer* sie sehen darf. Die Mitgliedschaftsprüfung selbst (`league(requester,
leagueId)`) ist bestehender, ungeänderter Code.

## Umgesetzt in
- `frontend/src/league/ReportScreen.jsx`
- `frontend/src/league/StandingsTable.jsx` (aus `LeagueDetailScreen.jsx`
  herausgelöst, jetzt von beiden Screens verwendet)
- `frontend/src/league/LeagueDetailScreen.jsx` (nur der Import geändert)
- `frontend/src/styles.css`

## Offene Fragen
Keine.
