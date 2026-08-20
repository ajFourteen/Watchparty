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

### Audit vom 2026-08-20: welche Pipeline-Stufen noch einen harten Check vertragen

Ausgangspunkt war die Pipeline-Grafik (Artifact „Watchparty-Pipeline") —
für jeden Übergang darin geprüft, ob er wirklich Urteilssache ist (→ Skill)
oder sich am Ergebnis ablesen ließe (→ Gradle-Task). Die neun Skills aus
diesem Dokument sind daraus entstanden und liegen jetzt unter
`.claude/skills/`. Was an Gates übrig bleibt:

- **ArchUnit-Regel gegen Invariante 1.** Bislang prüft nichts automatisch,
  dass `domain` und `application` frei von `synchronized`, `volatile` und
  `java.util.concurrent.*` bleiben — CLAUDE.md sagt es nur in Prosa
  („darf auch keine bekommen, weil das die Regel verschleiern würde"). Eine
  ArchUnit-Regel dafür ist mechanisch dieselbe Art Prüfung wie die
  bestehenden Ringe/Stereotyp-Regeln in `ArchitectureTest` — der stärkste
  Einzelfund dieses Audits, weil er eine der gefährlichsten Invarianten
  von reiner Erinnerung auf einen roten Test umstellt. Bis dahin deckt
  `/invarianten-review` die Lücke ab.
- **Feature-Dokument-Vollständigkeit.** Kein Task prüft, dass ein
  `docs/features/NNN-*.md` alle sieben Vorlagen-Abschnitte trägt oder dass
  „Umgesetzt in" tatsächlich existierende Klassen nennt — mechanisch
  dieselbe Art Prüfung wie `aufbaudoku` (Reflection/Textabgleich gegen den
  Baum), nur auf die Feature-Vorlage statt auf `CLAUDE.md` angewendet.
- **ADR-Nummern lückenlos.** Trivial zu prüfen, aber auch trivial von Hand
  zu sehen — geringer Nutzen, hier nur der Vollständigkeit halber notiert.
- **Erwogen und verworfen: Commit-Typ passt zum Diff.** Ob `fix:` wirklich
  nur Verhalten ändert und `docs:` wirklich nur Prosa — als Gate zu
  störanfällig (zu viele legitime Mischfälle), deshalb bewusst nicht
  automatisiert und stattdessen Skill `/freigabe`.
- **Schwächerer Kandidat: Invariante 2 (kein I/O im Raum-Thread).** Ein
  Verbot von `java.io`/`java.nio`/Netzwerk-Importen in `domain`/
  `application` wäre technisch möglich, aber ein schwächeres Signal als bei
  Invariante 1 — mehr Näherung als Beweis. Zurückgestellt, bis sich zeigt,
  ob die Lücke in der Praxis überhaupt ausgenutzt wird.

## Kontext-Ökonomie

- **`CLAUDE.md` ist groß.** Rund 250 Zeilen, gut die Hälfte Dateibaum, geladen
  bei jedem Turn. Der `aufbaudoku`-Task hält ihn inzwischen aktuell, aber nicht
  kurz. Auslagern nach `docs/aufbau.md` wäre eine Möglichkeit; der Verlust wäre
  die Orientierung beim Einstieg.
- **Breite Recherchen an den Explore-Agenten geben**, statt selbst durch
  mehrere tausend Zeilen Dokumentation zu lesen.
- **`/fewer-permission-prompts`** senkt die Alltagsreibung — die
  Berechtigungsliste in `.claude/settings.local.json` ist kurz.
