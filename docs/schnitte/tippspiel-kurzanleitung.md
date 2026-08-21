# Schnittplan — Kurzanleitung Tippspiel

Herkunft: Skill `schneiden`, 2026-08-21. Der Satz, den ein Mensch nach dem
ganzen Vorhaben sagen kann: „Ich kann mir im Tippspiel jederzeit erklären
lassen, wie Anmeldung, Tippen, Wertung, Ligen und der Spieltags-Report
funktionieren — ohne dass mir das jemand am Tisch vorher erklären muss."

Vorbild ist `Guide.jsx` bei den Live-Wetten: dort ebenfalls ein einziges
Overlay für Ablauf, Punkte, Wettkatalog und Host-Rolle zusammen, nicht
stufenweise über mehrere Merges aufgebaut.

Diese Datei ist der Fortschrittsanzeiger für `/feature`: Wer sie ohne
Chatverlauf öffnet — auch in einer neuen Sitzung — findet hier den
nächsten offenen Schnitt. `feature` aktualisiert Status und
Feature-Dokument-Spalte selbst, sobald ein Schnitt grün ist; sonst bleibt
diese Datei unverändert liegen.

| # | Schnitt | Behelf | Kritikalität | Status | Feature-Dokument |
|---|---|---|---|---|---|
| 1 | Ich kann mir als Tipper die Regeln des Tippspiels jederzeit erklären lassen | — | LOW | fertig | `docs/features/011-tippspiel-kurzanleitung.md` |

Status ∈ `offen`, `in Arbeit`, `blockiert`, `fertig`.

Anmerkung zur Ein-Schnitt-Entscheidung: Eine Teilung nach Themenblöcken
(z. B. erst Tipp-/Wertungsregeln, dann Liga-Regeln, dann Report-Opt-in)
wurde geprüft und verworfen. Die Abbruchprobe scheitert an jeder Teilung —
eine Kurzanleitung, die nur einen Teil der Regeln erklärt, wäre am
Spielabend sichtbar unfertig, kein für sich benutzbares Ergebnis. Anders
als beim Spieltags-Report (`docs/schnitte/spieltags-report.md`) gibt es
hier auch keinen Behelf, der in einem späteren Schnitt ersetzt würde: Die
Komponente ist reine, serverdatengestützte Textanzeige ohne eigene
Fachlogik, durchgängig `LOW` (analog zu `Bets`, `docs/teststrategie.md`
Abschnitt 6.4 — statische Daten, Fehler sofort sichtbar). Geplanter Umfang
grob: Zugriffspunkt (Button + Erstbesuch-Automatik wie bei `Guide.jsx`),
Anmeldung ohne Passwort, Abgabeschluss/ein Tipp je Spiel, verdeckte fremde
Tipps vor Anstoß, Wertung (Tendenz/Abstand/exaktes Ergebnis), Ligen
anlegen/beitreten, Rangliste samt Gleichstandsregel, Spieltags-Report
Opt-in — geschätzt rund zehn Akzeptanzkriterien, unter der Zwölfer-Grenze.
