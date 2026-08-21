# Schnittplan — Spieltags-Report

Herkunft: Skill `schneiden`, 2026-08-21. Der Satz, den ein Mensch nach dem
ganzen Vorhaben sagen kann: „Nach dem Spieltag sehe ich auf einen Blick,
was meine Tipps wert waren und wie ich in meiner Liga dastehe."

Diese Datei ist der Fortschrittsanzeiger für `/feature`: Wer sie ohne
Chatverlauf öffnet — auch in einer neuen Sitzung — findet hier den
nächsten offenen Schnitt. `feature` aktualisiert Status und
Feature-Dokument-Spalte selbst, sobald ein Schnitt grün ist; sonst bleibt
diese Datei unverändert liegen.

| # | Schnitt | Behelf | Kritikalität | Status | Feature-Dokument |
|---|---|---|---|---|---|
| 1 | Ich sehe nach dem Spieltag meine eigene Bilanz | keine Liga-Sicht, keine Platzveränderung, keine Highlights | MEDIUM | fertig | `docs/features/006-spieltags-report.md` |
| 2 | Ich sehe, wie meine Liga den Spieltag getippt hat | keine fremden Einzeltipps im Report (die bleiben im `MatchdayScreen`) | MEDIUM | fertig | `docs/features/007-spieltags-report-liga.md` |
| 3 | Ich sehe, ob ich in der Saison gestiegen oder gefallen bin | — | MEDIUM | fertig | `docs/features/008-spieltags-report-platzierung.md` |
| 4 | Ich sehe die Höhepunkte des Spieltags | — | LOW | fertig | `docs/features/009-spieltags-report-hoehepunkte.md` |
| 5 | Ich bekomme den Report per Mail | Text-Mail statt gestalteter HTML-Mail; Bestellen/Abbestellen als ein Schalter am Konto, nicht je Liga; Versand fire-and-forget wie der bestehende `MailSender`, kein Retry bei Zustellfehlern | MEDIUM | fertig | `docs/features/010-spieltags-report-mail.md` |

Status ∈ `offen`, `in Arbeit`, `blockiert`, `fertig`. `feature` nimmt
immer die oberste Zeile mit Status `offen` — die Reihenfolge selbst ist
die Entscheidung von `schneiden` (Abbruchprobe je Schnitt), nicht bei
jedem Aufruf neu zu verhandeln.

Drei Anmerkungen aus dem ursprünglichen Schnitt bzw. seiner Fortschreibung,
die für spätere Schnitte gelten:
- **Schnitt 2 bleibt bewusst MEDIUM.** Fremde Einzeltipps in den Report
  aufzunehmen würde Kriterium 19/20 berühren und ihn HIGH machen — dafür
  müsste er weiter geteilt werden. Die Spieltagsrangliste allein verrät
  keinen einzelnen Tipp.
- **Schnitt 3 rechnet, statt zu speichern.** Der Platz „vor dem Spieltag"
  entsteht aus der Rangliste über die Spieltage < N, nicht aus einem
  eingefrorenen Stand (bleibt verträglich mit 13.6-j).
- **Schnitt 5 bleibt trotz vier erkennbarer Bausteine (Opt-in-Verwaltung,
  FINAL-Übergang-Erkennung, Mailinhalt, Abmeldelink) ein einziger Schnitt.**
  Die Abbruchprobe scheitert an jeder Teilung: Ein Opt-in ohne jemals
  versendete Mail hat keinen Wert, eine Erkennung des FINAL-Übergangs ohne
  Empfänger ebenso wenig, und ein Abmeldelink ohne vorherigen Versand hat
  nichts abzubestellen. Die vier Bausteine sind Teile *eines* dünnen Wegs
  durch die Ringe, keine vier Fähigkeiten. Auslöser, Empfängerkreis und
  Abmeldung selbst sind bereits entschieden (ADR-041, `anforderungen.md`
  13.9, Anhang A 13.9-n bis 13.9-p) — dieser Schnitt setzt sie um, verhandelt
  sie nicht neu. Der Mailinhalt ist eine reine Ableitung aus der bereits
  bestehenden Bilanz-Berechnung (13.9-a ff.), kein neuer fachlicher Bau.
