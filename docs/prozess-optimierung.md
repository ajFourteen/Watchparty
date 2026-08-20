# Optimierung des Entwicklungsprozesses

Ideensammlung, kein geltender Stand. Was hier steht, ist **vorgeschlagen,
nicht beschlossen** — die Abgrenzung ist dieselbe wie zwischen einem
Feature-Dokument und `anforderungen.md`. Wird eine Idee umgesetzt, verschwindet
sie hier und lebt danach im Bau, in einem Skill oder in einem ADR weiter.

Entstanden am 2026-08-20 aus einer Bestandsaufnahme des Prozesses.

## Das Ordnungsprinzip

Die meisten Einzelfälle beantwortet diese Sortierfrage von selbst:

| Die Regel betrifft… | gehört in… | weil |
|---|---|---|
| das **Ergebnis** (Code, Tests, Doku-Konsistenz) | einen Gradle-Task an `check` | gilt für Mensch, Agent und CI gleichermaßen |
| die **Reihenfolge** der Arbeit | einen Skill | braucht Urteil, ist am Ergebnis nicht messbar |
| die **Gewohnheiten** des Agenten | einen Hook | betrifft nur dessen Sitzung |
| nur **Kontext**, kein Urteil | einen Subagenten | Lesen auslagern, Kontext sparen |

Der Satz, auf den es ankommt: **Wer eine Regel über den Code in einen Hook
schreibt, halbiert sie.** Sie gilt dann nur, wenn der Agent arbeitet — nicht
beim Editieren von Hand und nicht in CI. Deshalb liegt der Schwerpunkt in
diesem Projekt auf Gradle, nicht auf Hooks.

## Der Fortschrittsanzeiger, den es schon gibt

Für den Stand eines Features braucht es kein Ticketsystem; die Zustände sind
maschinell ablesbar:

| Zustand | woran erkennbar |
|---|---|
| entschieden, nicht gebaut | Regel steht in Anhang A, `abdeckung` ist rot |
| in Arbeit | JGiven-Szenarien existieren und laufen rot |
| fertig | `check` grün |
| in Produktion | Release-Tag und Deploy erfolgreich |

`abdeckung` wird heute nur als Gate benutzt. Als **Kompass** ist es mehr wert:
Rot heißt nicht „Build kaputt", sondern „hier liegt beschlossene, unerledigte
Arbeit".

## Skills

Ein Skill ist erst dann etwas wert, wenn er den Ablauf **erzwingt** statt ihn
zu beschreiben. Gute Skills sind Übergänge zwischen Stufen, keine Tätigkeiten
— deshalb `/feature` und nicht `/tests-schreiben`.

### Von der Idee zur Entscheidung

- **`/triage`** — ordnet eine rohe Idee genau einem von vier Orten zu: offene
  Entscheidung, Beobachtung für den Spielabend, ADR oder Feature. Die
  Abgrenzung ist in den Dokumenten bereits scharf definiert, es fehlt nur das
  Ritual, das sie anwendet.
- **`/entscheidung`** — führt die Vier-Dokumente-Kette, wenn eine offene Frage
  beantwortet ist: Eintrag aus `offene-entscheidungen.md` streichen, ADR
  schreiben, `anforderungen.md` nachziehen, atomare Regel in Anhang A
  ergänzen. Endet damit, dass `abdeckung` **rot** läuft — die neue Regel ist
  beschlossen und noch unbelegt, und genau das soll die Metrik sagen.
- **`/probelauf`** — arbeitet den Beobachtungsbogen nach dem Spielabend ab:
  Was ist beantwortet, was wird gestrichen, was wird ADR, was Anforderung?

### Von der Entscheidung zum grünen Code

- **`/feature`** — Abschnitt 9.1 der Teststrategie, erzwungen statt
  beschrieben. Kern ist der Zwischenschritt, den man am leichtesten
  überspringt: Szenarien werden zu JGiven-Stufen, die **rot laufen**, bevor
  eine Zeile Produktivcode entsteht.
- **`/domaenentyp`** — die vier Dinge, die die Konventionen für einen neuen Typ
  in `domain/model` fordern und die man einzeln vergisst: jMolecules-Stereotyp,
  Nullness, JGiven-Szenario, Gegenprobe gegen den Begriff in
  `anforderungen.md`.
- **`/pruefen`** — gestufte Rückkopplung statt Blindflug: `compileJava`
  (NullAway, Sekunden) → `test` (unit/port) → `archTest` → voller `check`,
  Abbruch beim ersten Rot. Kein neuer Prüfumfang, nur eine andere Reihenfolge.
- **`/invarianten-review`** — prüft eine Änderung gegen die sieben harten
  Invarianten aus `CLAUDE.md`. Der wertvollste der Reihe: Ein generischer
  Code-Review kennt diese Regeln nicht, und die gefährlichsten davon sind
  gerade die, die kein Test prüfen kann.
- **`/adr`** — nächste Nummer, Vorlage, Rückverweis aus `CLAUDE.md` nachziehen.

### Vor dem Commit

- **`/freigabe`** — macht sichtbar, was der gewählte Commit-Typ auslöst. Die
  unterschätzte Stelle des Ablaufs: **Die Commit-Message ist eine
  Deployment-Entscheidung, keine Beschriftung.** `feat:` und `fix:` gehen nach
  Produktion, `chore:` und `docs:` nicht.

## Hooks

Sparsam einsetzen; alles Weitere ist in Gradle besser aufgehoben.

- **SessionStart** — `git status`, letzte Commits und vor allem
  `offene-entscheidungen.md` einspeisen. Die Regel „dort nichts stillschweigend
  festlegen" funktioniert nur, wenn der Agent weiß, was drinsteht.

Ausdrücklich **kein** Hook, der nach jedem Edit `check` anwirft: Das macht die
Sitzung unbenutzbar. Dafür ist `/pruefen` da.

## Weitere Gates

- **Frontend-Verhalten.** Der `protokollvertrag`-Task prüft die *Namen* an der
  Protokollgrenze. Die *Bedeutung* — ob das Frontend ein korrekt benanntes Feld
  auch richtig interpretiert — bleibt außerhalb. Ein Testrunner (Vitest) plus
  Linter für `frontend/` wäre der nächste Schritt, ist aber ein eigenes
  Vorhaben und sollte nicht nebenbei entstehen.
- **Die Wartelogik der Maschinenzählung.** `ci/eine-maschine-pruefen.sh` prüft
  sofort nach dem Deploy. Führt Fly kurzzeitig alte und neue Maschine als
  `started`, schlägt die Prüfung fehl, obwohl nichts kaputt ist. Ein Poll über
  ~60 Sekunden würde das abfangen. Bewusst offen gelassen, bis ein echter
  Deploy zeigt, ob es das Problem überhaupt gibt — beim ersten (2026-08-20)
  trat es nicht auf.
- **Git-Hooks für den Menschen.** `ci/git-regeln-hook.sh` bindet den Agenten,
  nicht das Repository: Wer selbst `git checkout -b` tippt, wird nicht
  gehindert. Ein `pre-commit`/`pre-push`-Hook könnte dasselbe Skript
  wiederverwenden, braucht aber `core.hooksPath` auf ein Verzeichnis im Repo
  plus einen einmaligen Einrichtungsschritt.

## Kontext-Ökonomie

- **`CLAUDE.md` ist groß.** Rund 250 Zeilen, gut die Hälfte Dateibaum, geladen
  bei jedem Turn. Der `aufbaudoku`-Task hält ihn inzwischen aktuell, aber nicht
  kurz. Auslagern nach `docs/aufbau.md` wäre eine Möglichkeit; der Verlust wäre
  die Orientierung beim Einstieg.
- **Breite Recherchen an den Explore-Agenten geben**, statt selbst durch
  mehrere tausend Zeilen Dokumentation zu lesen.
- **`/fewer-permission-prompts`** senkt die Alltagsreibung — die
  Berechtigungsliste in `.claude/settings.local.json` ist kurz.
