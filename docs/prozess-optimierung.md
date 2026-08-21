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
| die **Form** des Dokuments, an dem die Regel scheitert | eine Änderung an Vorlage und Bestand | ein Formatmangel ist kein Naturgesetz (2026-08-21) |

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

### Audit vom 2026-08-20: welche Pipeline-Stufen noch einen harten Check vertragen

Ausgangspunkt war die Pipeline-Grafik (Artifact „Watchparty-Pipeline") —
für jeden Übergang darin geprüft, ob er wirklich Urteilssache ist (→ Skill)
oder sich am Ergebnis ablesen ließe (→ Gradle-Task). Die neun Skills aus
diesem Dokument sind daraus entstanden und liegen jetzt unter
`.claude/skills/`.

Umgesetzt, noch am selben Tag: zwei neue ArchUnit-Regeln
(`domaeneOhneSynchronisierteMethoden`, `domaeneOhneVolatileFelder`) und der
Task `featuredoku` (Vollständigkeit der Feature-Dokumente, siehe
`docs/teststrategie.md` Abschnitt 10).

**Korrektur zum ersten Entwurf dieses Audits:** Der damalige Befund,
Invariante 1 und 2 seien komplett ungeprüft, war falsch.
`domaeneOhneNebenlaeufigkeit` und `anwendungsringBlockiertNicht` deckten in
`ArchitectureTest.java` schon vorher den größten Teil ab — nur das
Schlüsselwort `synchronized`/`volatile` selbst fiel durch eine reine
Abhängigkeitsregel (Verbot von `java.util.concurrent`-*Importen*) hindurch.
Der Fehler im ersten Durchgang: nur nach Abschnittsüberschriften gegrept,
nicht die Regelkörper gelesen. Deshalb jetzt hier festgehalten, nicht nur
im Commit — ein Audit, das sich selbst falsch zitiert, ist schlimmer als
keins.

Offen geblieben, mit Begründung:

- **ADR-Nummern lückenlos.** Weiterhin nicht umgesetzt: trivial zu prüfen,
  aber auch trivial von Hand zu sehen — kein eigener Task wert.
- **Erwogen und verworfen: Commit-Typ passt zum Diff.** Ob `fix:` wirklich
  nur Verhalten ändert und `docs:` wirklich nur Prosa — als Gate zu
  störanfällig (zu viele legitime Mischfälle), deshalb bewusst nicht
  automatisiert und stattdessen Skill `/freigabe`.
- **Invariante 2 über `anwendungsringBlockiertNicht` hinaus.** Das
  bestehende Muster verbietet gezielt blockierende Aufrufe (mit der
  begründeten Ausnahme `awaitIdle`), nicht pauschal `java.io`/`java.nio`.
  Ein zusätzliches Import-Verbot wäre redundant dazu und ein schwächeres
  Signal als die gezielte Regel — zurückgestellt.

**Nachtrag 2026-08-21.** `ci/commit-format-pruefen.sh` prüfte das
Commit-Format bislang nur in CI (`build.yml`) — ein falscher Typ fiel erst
nach dem Push auf. Das Skript unterstützte einen Einzel-Commit-Aufruf zwar
schon (`ci/commit-format-pruefen.sh` ohne Argumente), nichts rief ihn aber
lokal auf. Jetzt ein echter Git-`commit-msg`-Hook (`.githooks/commit-msg`,
aktiv über `git config core.hooksPath .githooks`, siehe README.md) — anders
als `ci/git-regeln-hook.sh`, der nur den Claude-Code-Agenten bindet, gilt
dieser für jeden, der hier committet. Das ist **nicht** dasselbe wie der
oben verworfene „Commit-Typ passt zum Diff" — geprüft wird weiterhin nur
das *Format*, nicht ob der Typ zur Änderung passt; diese Entscheidung
bleibt bei `/freigabe`.

**Nachtrag 2026-08-21, zweiter Teil.** Drei weitere Kandidaten geprüft,
diesmal am tatsächlichen Inhalt der Dateien, nicht nur an ihrer Struktur:

- **`/freigabe` als (Teil-)Check statt nur Skill — umgesetzt, 2026-08-21.**
  Ein Vergleich „passt der Commit-Typ zum Diff" bleibt aus den oben
  genannten Gründen zu störanfällig. Die engere, einseitige Fassung ist
  jetzt Teil von `ci/commit-format-pruefen.sh` (und damit sowohl im
  `commit-msg`-Hook als auch im CI-Range-Modus): Ein releasender Typ
  (`feat:`/`fix:`/`perf:`), der **keine** einzige Datei unter `src/main/`,
  `frontend/src/`, `frontend/package(-lock).json`, `build.gradle.kts`,
  `settings.gradle.kts`, `Dockerfile` oder `fly.toml` ändert, wird
  abgelehnt. Kein Verständnis des Diff-*Inhalts* nötig, nur der Pfade.
  Der Gegenprobe-Lauf gegen die eigene Historie fand sofort einen echten
  Treffer: Commit `cb3e298` (der `commit-msg`-Hook selbst) war genau dieser
  Fehler — `feat:` ohne jede Anwendungsdatei.
- **Anforderungen ↔ Feature-Dateien: erst verworfen, dann umgesetzt
  (2026-08-21).** Der ursprüngliche Befund stimmte in jeder Einzelheit: Der
  Abschnitt „Betroffene Anforderungen" enthielt echte ID-Zitate,
  Tabellen mit unterschiedlichen Spaltenköpfen je Dokument, bloße
  Kapitelverweise (`11 (out of scope)`, dreifach im selben Dokument), eine
  Platzhalter-Range (`13.1-a bis 13.8-x`) und eine Kollision mit einer
  anderen Zählung (`Invariante 4`). Ein Regex darauf hätte Fehlalarme
  erzeugt.
  Falsch war die **Folgerung**. Aus „diese Prosa ist nicht parsbar" wurde
  „diese Prüfung ist nicht baubar" — und damit ein Formatmangel als
  Naturgesetz verbucht. Dass an Anforderungen nichts unparsbar ist, beweist
  Anhang A: derselbe Stoff, feste Spalten, und daran hängen `abdeckung`,
  `@Anforderung` und `jedeKritikalitaetsAnforderungExistiertInAnhangA`.
  Dazu kam ein übersehener Hinweis: 002, 003 und 004 hatten von sich aus
  dieselbe Tabelle erfunden — eine unformalisierte Konvention, kein Chaos.
  Umgesetzt: Der Abschnitt trägt jetzt eine Pflichttabelle
  `| ID | Bezug | Anmerkung |`, Fließtext bleibt darunter und ungeparst.
  `featuredoku` prüft `Bezug` gegen vier feste Wörter und jede ID mit
  `Bezug != neu` gegen Anhang A. Die fünf bestehenden Dokumente sind
  nachgezogen. Genau die Fälle, die den ersten Anlauf gekippt haben, fängt
  die Gegenprobe jetzt einzeln ab.

- **Kritikalität: erst „nicht schließbar", dann geschlossen (2026-08-21).**
  Auch hier stimmte die Beobachtung — Feature 005 trägt acht Bereiche mit
  drei Stufen — und auch hier zeigte sie auf die Vorlage, während die
  Folgerung auf die Prüfbarkeit zeigte. Ein Feature aus Anmeldung,
  Wertung, Feed und Oberfläche *hat* keine eine Stufe; 005 ist das
  ehrliche Dokument und die Vorlage die widerlegte Seite.
  Der zweite Denkfehler war eine Verschiebung des Ziels: Zur Debatte stand
  Feature-Dokument ↔ `@Criticality`. Die Antwort „Anhang A trägt keine
  Kritikalitätsspalte" beantwortet die viel größere Frage „welche
  *Anforderung* muss HIGH sein" — und lehnte die kleine ab, weil die große
  offen war. Eine neue Spalte in Anhang A braucht es dafür nicht.
  Umgesetzt: Die Vorlage verlangt genau eine Stufe als maschinenlesbare
  Zeile `**Stufe:** LOW|MEDIUM|HIGH`. Damit wird „mehr als eine Stufe
  nötig" zum Teilungskriterium statt zur geduldeten Ausnahme.

- **Featuregröße als Gate (2026-08-21, neu).** Aus beidem folgte die
  eigentliche Ursache: 005 ist kein Feature, sondern neun. Das Dokument
  sagt es selbst — eine Neun-Stufen-Bautabelle, ein `feat:`-Commit je
  Stufe. Die Teilung hatte stattgefunden, nur nicht in Dateien, und entlang
  der Schichten statt der Fähigkeiten: Bis Stufe 7 („Oberfläche", als
  einzige mit Umfang L) konnte kein Mensch etwas tun, obwohl das Dokument
  jede Stufe „einzeln einsetzbar" nennt.
  `featuredoku` prüft deshalb zusätzlich: höchstens zwölf
  Akzeptanzkriterien und keine eigene Baustufentabelle. Drei Dokumente
  (002, 004, 005) liegen darüber und sind im Build namentlich als
  Bestandsschutz eingetragen — dass drei von fünf die Regel verletzen, ist
  der Befund und nicht ein Grund, die Grenze weicher zu setzen.
  Was sich damit **nicht** prüfen lässt, ist die Schnittrichtung: Ein
  horizontaler Schnitt besteht alle Prüfungen. Dafür der neue Skill
  `schneiden` — er läuft vor `feature` und macht aus einer Idee eine Folge
  vertikaler Scheiben, jede für sich benutzbar. Nach dem Ordnungsprinzip
  oben ist das richtig verortet: Größe ist Ergebnis (→ Gradle),
  Schnittrichtung ist Urteil (→ Skill).

**Was aus beiden Fehlschlüssen zu lernen ist.** Die Sortiertabelle am Kopf
dieses Dokuments kennt vier Ausgänge — Gradle-Task, Skill, Hook,
Subagent — und keinen fünften: *das Format des Artefakts ist der Mangel*.
Ein Audit, das nur mit diesem Raster fragt, kann bei unstrukturierter
Quelle ausschließlich „nicht prüfbar" ausgeben. Deshalb steht die Frage
jetzt hier: **Muss dieses Dokument so aussehen?** — vor der Frage, ob sich
sein heutiges Aussehen prüfen lässt.

## Kontext-Ökonomie

- **`CLAUDE.md` ist groß.** Rund 250 Zeilen, gut die Hälfte Dateibaum, geladen
  bei jedem Turn. Der `aufbaudoku`-Task hält ihn inzwischen aktuell, aber nicht
  kurz. Auslagern nach `docs/aufbau.md` wäre eine Möglichkeit; der Verlust wäre
  die Orientierung beim Einstieg.
- **Breite Recherchen an den Explore-Agenten geben**, statt selbst durch
  mehrere tausend Zeilen Dokumentation zu lesen.
- **`/fewer-permission-prompts`** senkt die Alltagsreibung — die
  Berechtigungsliste in `.claude/settings.local.json` ist kurz.
