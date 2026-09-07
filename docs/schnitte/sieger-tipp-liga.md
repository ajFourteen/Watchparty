# Schnittplan — Sieger-Tipp-Liga

Herkunft: Skill `schneiden`, 2026-09-07, im Anschluss an ADR-043
(„Sieger-Tipp-Wertung als zweites Wertungsschema, Eigenschaft der Liga",
akzeptiert) und die dort bereits nachgezogenen Anforderungen 13.5-f/13.6-m.
Der Satz, den ein Mensch nach dem ganzen Vorhaben sagen kann: „Ich kann eine
Liga anlegen, in der nur zählt, wer den Sieger richtig getippt hat — nicht
das genaue Ergebnis."

Diese Datei ist der Fortschrittsanzeiger für `/feature`: Wer sie ohne
Chatverlauf öffnet — auch in einer neuen Sitzung — findet hier den
nächsten offenen Schnitt. `feature` aktualisiert Status und
Feature-Dokument-Spalte selbst, sobald ein Schnitt grün ist; sonst bleibt
diese Datei unverändert liegen.

| # | Schnitt | Behelf | Kritikalität | Status | Feature-Dokument |
|---|---|---|---|---|---|
| 1 | Ich lege eine Liga mit Sieger-Tipp-Wertung an, und ihre Rangliste zählt nur, wer den Sieger richtig getippt hat | — | HIGH | offen | — |

Status ∈ `offen`, `in Arbeit`, `blockiert`, `fertig`.

Anmerkung zur Ein-Schnitt-Entscheidung: ADR-043 legt die Richtung bereits
fest und lässt kaum Spielraum für eine weitere Teilung. Eine Teilung nach
Bauteilen (erst die Domäne — `ScoringScheme`, zweite `Scoring`-Funktion,
`Standings`-Auswahl —, dann die Persistenz-Spalte, dann die
REST-Erweiterung, dann die Oberfläche) wäre horizontal: Jeder dieser Teile
für sich hat der Skill-Regel nach keinen Wert, den ein Mensch beobachten
kann, und die Abbruchprobe scheitert überall. Eine Teilung nach den beiden
Wertungspunkten selbst (13.5-f getrennt von 13.6-m) scheitert ebenso: Eine
Liga, die sich auf ein Wertungsschema festlegen lässt, das dann noch keine
Wirkung auf die Rangliste hat, wäre irreführend — genau der Fall, den die
Abbruchprobe ausschließen soll. Beide Anforderungspunkte sind deshalb ein
einziger Schnitt.

Die reine Sieger-Tipp-Wertungsfunktion in `Scoring` zuerst zu schreiben,
ohne jede Infrastruktur drumherum, bleibt erlaubt und sinnvoll (wie bei
`Scoring.score` selbst und bei 005/`Scoring`, ADR-038) — das ist eine
Reihenfolge **innerhalb** dieses einen Schnitts, kein eigener Schnitt davor.

Der Schnitt bleibt durchgängig **HIGH**: Er berührt dieselbe Wertungslogik
wie die bestehende Sieger-Tendenz aus 13.5-b (`Scoring`, bereits HIGH,
Mutation Score als Zusage) — falsch entscheiden, wer den Sieger getroffen
hat, ist derselbe Fehlerklasse wie bei der Standardwertung, nur ohne
Abstands-/Exakt-Stufen obendrauf. Die Liga-Anlage und die
Persistenz-Erweiterung selbst tragen keine eigene, geringere
Kritikalitätsstufe, weil sie ohne die Wertungsfunktion dahinter nicht für
sich stehen (s. o.) — nach 6. „genau eine Kritikalitätsstufe" zählt die
höchste im Schnitt enthaltene, nicht der Durchschnitt.

Geplanter Umfang grob, damit `feature` beim Zerlegen in JGiven-Stufen
startet, nicht rät:
- `ScoringScheme` als neues Value Object (zwei Ausprägungen, ADR-027-Stereotyp
  sofort mitgeben), Feld an `League` (`League.create` bekommt den Parameter,
  `League.of` zum Wiederaufbau ebenso).
- Zweite reine Funktion in `Scoring` (`(Prediction, GameScore) -> LeaguePoints`
  bzw. gleich auf `GameScore`/`GameScore` wie die bestehende, je nachdem was
  sich beim Schreiben als der schmalere Schnitt zeigt), `Standings.compute`
  wählt die Funktion nach dem Schema der aufrufenden Liga statt sie fest zu
  verdrahten.
- Flyway-Migration: neue Spalte am `league`-Schema, Vorbelegung Standardwertung
  für bestehende Zeilen (ADR-043, Konsequenzen); `LeagueRepositoryJdbc` liest/
  schreibt sie mit.
- `LeagueCommands.createLeague`/`LeagueController` nehmen das Schema entgegen;
  Standings-Antwort bleibt strukturell gleich (dieselben Felder, `exactCount`
  bei Sieger-Tipp-Wertung strukturell immer 0 statt eines eigenen Antwortfeldes
  — ADR-043 Punkt 5 sagt bereits, dass keine neue Tiebreak-Stufe nötig ist).
- Frontend: `LeaguesScreen.jsx` bekommt die Schema-Auswahl beim Anlegen,
  `LeagueDetailScreen.jsx` zeigt sie danach nur noch informativ (ADR-043,
  Konsequenzen) — vorbehaltlich `triage`, ob das als eigene `frontend`-Regel
  in Anhang A oder als `gestaltung` läuft; das entscheidet `feature`, nicht
  dieser Schnitt.

Geschätzt deutlich unter der Zwölfer-Grenze für Akzeptanzkriterien (zwei
bereits benannte Anhang-A-Punkte plus die üblichen Rand-/Fehlerfälle beim
Anlegen und bei der Rangliste).
