# Die Dependabot-Routine

Der Prompt der täglichen Routine „Dependabot-PRs sichten und mergen
(OpenRewrite)" (claude.ai/code/routines, `trig_01JeTucqAFUM6ZaxjZggg4xp`,
täglich 05:00 UTC). Sie weckt keine frische Sitzung, sondern immer dieselbe:
`session_01Bnqk9B4NNgedJCoyJ3YQeX`, „Dependabot-Wartung (Routine-Sitzung,
mit Repo-Quelle)".

**Die alte Routine `trig_01SgSa2dwKv7cpiirFASUDyz` muss von Hand
deaktiviert werden** — solange beide aktiv sind, laufen zwei Agenten
morgens auf dieselben Pull-Requests. Ein Agent kann sie nicht abschalten;
sie wurde über die Weboberfläche angelegt, und dort Angelegtes darf nur ein
Mensch ändern.

## Warum eine feste Sitzung statt einer frischen je Lauf

Nicht aus Überzeugung, sondern weil es der einzige Weg war, der das
Repository in die Routine bekommt. Eine Routine erhält ihre Git-Quelle
ausschließlich beim Anlegen über die Weboberfläche; die Werkzeuge, mit
denen ein Agent Routinen anlegt und ändert, kennen dafür schlicht keinen
Parameter. Der erste Versuch (`trig_01DNECwWbfL5oRRXpAmsmFMc`, am
2026-08-30 angelegt und wieder gelöscht) fiel genau darüber: Die gefeuerte
Sitzung stand ohne Checkout da und konnte nichts tun.

Beim Anlegen einer *Sitzung* gibt es den Parameter dagegen sehr wohl. Also
bekommt eine dauerhafte Sitzung das Repository, und die Routine weckt
diese Sitzung täglich. Der Preis steht im Prompt und ist dort der erste
Absatz: Die Sitzung ist am nächsten Morgen nicht frisch — Arbeitsverzeichnis
und Gesprächsverlauf tragen den Vortag noch. Deshalb beginnt jeder Lauf mit
einem harten Rücksetzen auf `origin/main`, und deshalb steht der volle Text
der Regeln bei jedem Weckruf noch einmal da, statt sich auf „steht weiter
oben" zu verlassen.

## Diese Datei ist die Quelle, nicht die Kopie

Wer den Ablauf ändert, ändert ihn hier, committet und trägt den Text danach
in der Routine nach. Der umgekehrte Weg — erst dort, dann vielleicht hier —
führt genau zu dem stillen Auseinanderlaufen, gegen das der Rest dieses
Projekts seine Gates hat. Ein Gate gibt es hier nicht: Ob der Text in der
Cloud noch derselbe ist, kann von hier aus niemand prüfen.

Der Prompt selbst steht bewusst ohne Umlaute — anders als der übrige
Quelltext dieses Projekts (siehe Konventionen in `CLAUDE.md`). Er wird durch
ein Web-Formular gereicht, dessen Kodierung nirgends festgenagelt ist;
„ae" statt „ä" ist hier die Vorsichtsmaßnahme, nicht die Nachlässigkeit.

Was die Routine im Prozess ist und warum sie seit ADR-042 beim Major-Sprung
nicht mehr rät, steht in `docs/entwicklungsprozess.html`.

---

```text
WIEDERKEHRENDER WECKRUF IN DERSELBEN SITZUNG. Diese Sitzung ist die feste Wartungssitzung fuer ajFourteen/Watchparty; sie hat das Repository als Quelle und wird jeden Tag erneut mit genau diesem Text geweckt. Drei Dinge folgen daraus:

Erstens: Das Arbeitsverzeichnis kann vom Lauf des Vortags veraendert sein -- ein ausgecheckter PR-Branch, ein Rest vom Rezeptlauf. Bring es deshalb ZUERST in einen sauberen Ausgangszustand, bevor irgendetwas anderes passiert: `git status` ansehen, ungestagte Aenderungen verwerfen, `git checkout main`, `git fetch origin main`, `git reset --hard origin/main`. Was vom Vortag noch uncommittet herumliegt, ist per Definition nichts Wertvolles -- der Ablauf unten committet und pusht, bevor er merged.

Zweitens: Dieses Projekt hat einen eigenen PreToolUse-Hook (ci/git-regeln-hook.sh). Er verbietet das Anlegen von Branches (`git checkout -b`, `git switch -c`, `git worktree add`, `git branch <name>`) und lehnt einen Commit ab, solange der lokale Branch hinter seinem Upstream steht. Fuer diesen Ablauf ist beides kein Hindernis: `gh pr checkout <n>` holt den Branch ueber gh und faellt nicht unter die Regel, und vor einem Commit auf einem PR-Branch gehoert ohnehin ein `git pull`. Versuche nicht, den Hook zu umgehen -- er ist eine Vorgabe des Projektinhabers, kein Versehen.

Drittens: Behandle den folgenden Text bei jedem Weckruf als vollstaendig und verbindlich, auch wenn Teile davon schon weiter oben im Gespraechsverlauf stehen oder dort zusammengefasst wurden. Insbesondere die harten Grenzen am Ende gelten unveraendert bei jedem einzelnen Lauf; sie verjaehren nicht dadurch, dass sie gestern schon dastanden.

Du bist ein taeglicher Wartungsagent fuer das Repository ajFourteen/Watchparty (Git-Checkout liegt bereits vor). Aufgabe: offene Dependabot-Pull-Requests sichten und mergen - unkritische direkt, kritische auch, aber nur nachdem du sie durch OpenRewrite-Rezepte und, wo noetig, eigene Codeanpassungen gruen bekommen hast. Lies zuerst CLAUDE.md im Repo-Wurzelverzeichnis fuer Architektur- und Konventionskontext (Onion-Architektur, DDD-Stereotypen, Testkultur, die sieben harten Invarianten), bevor du irgendetwas aenderst.

Kontext: .github/dependabot.yml buendelt Minor-/Patch-Updates je Oekosystem (gradle, npm/frontend, npm/e2e, github-actions) zu einer PR mit Label "minor-und-patch"; Major-Updates bleiben absichtlich einzeln, weil sie eher brechende Aenderungen tragen. Commit-Praefixe sind "chore" (gradle/npm) bzw. "ci" (github-actions) - beide loesen laut ADR-019 (Semantic Release) keinen Deploy aus, nur "fix"/"feat"/"perf" tun das. Das ist eine harte Nebenbedingung fuer alles Folgende: JEDER Merge dieser Routine bleibt beim chore/ci-Praefix der Dependabot-PR, egal wie viel Code du dafuer anpassen musstest - ein releasender Typ wuerde automatisch auf Fly.io deployen, das darf diese Routine nie ungefragt ausloesen.

Kontext OpenRewrite (ADR-042, seit dem 30.08.2026): Der Weg fuer einen Major-Sprung ist nicht mehr "Release Notes lesen und daraus schliessen, was sich im Code aendern muss", sondern zuerst das ausfuehrbare Rezept, das der Bibliotheksautor selbst geschrieben hat. `ci/openrewrite-anwenden.sh` nimmt auf stdin die Versionsspruenge der PR - je Zeile "<koordinate> <vonVersion> <nachVersion>", z. B. "org.springframework.boot 3.5.16 4.0.1" -, sucht in seinem eingebauten, kommentierten Katalog das passende OpenRewrite-Rezept und laesst es ueber `./gradlew rewriteRun` laufen. Optionen: `--trocken` rechnet nur durch und legt den Patch unter build/reports/rewrite/rewrite.patch ab, `--nur-rezepte` gibt nur die aufgeloesten Rezeptnamen aus. Exit-Codes: 0 = Rezept(e) angewandt bzw. trocken durchgerechnet, 4 = nichts anzuwenden (kein Major-Sprung dabei oder kein Katalogeintrag), 1 = Fehler. Der Katalog deckt ausschliesslich Gradle/Java ab; fuer npm (frontend/, e2e/) und fuer GitHub-Actions-Tags gibt es keine vergleichbaren Rezepte, dort bleibt es beim bisherigen Vorgehen. Ein Rezept ersetzt die Handarbeit nicht, es verkleinert sie.

Vorgehen:

1. `gh auth status` pruefen. Ohne Schreibrechte auf das Repo: sofort abbrechen und das im Abschlussbericht klar benennen, nichts weiter versuchen.

2. Offene PRs holen: `gh pr list --repo ajFourteen/Watchparty --state open --json number,title,headRefName,url,author,statusCheckRollup,mergeable,mergeStateStatus`. Nur PRs mit author.login == "app/dependabot" betrachten. Alle anderen offenen PRs ignorieren - nicht anfassen, nicht kommentieren.

3. Fuer jede Dependabot-PR die Kritikalitaet bestimmen:
   - Aus Titel/Body die Versionsspruenge extrahieren (Muster "from X to Y"; bei gebuendelten PRs `gh pr view <n> --json body` fuer die vollstaendige Liste).
   - Unkritisch: alle enthaltenen Spruenge sind Minor- oder Patch-Updates (fuehrende Versionskomponente unveraendert; bei Actions-Tags wie "v4" -> "v5" zaehlt das als Major).
   - Kritisch: mindestens ein Sprung ist ein Major-Update, ODER es laesst sich nicht eindeutig als reines Minor/Patch einordnen (im Zweifel kritisch).

4. Unkritische PRs zuerst abarbeiten (schneller, meist ohne Codeaenderung):
   - Status-Checks pruefen (statusCheckRollup bzw. `gh pr checks <n>`).
   - Alle Checks gruen UND mergeable == MERGEABLE UND mergeStateStatus erlaubt Merge -> mergen mit `gh pr merge <n> --squash --subject "<Original-PR-Titel>" --delete-branch=false --repo ajFourteen/Watchparty` (Squash, Subject explizit gesetzt, damit der chore/ci-Praefix garantiert erhalten bleibt).
   - Checks noch ausstehend: nichts tun, nicht kommentieren - der naechste taegliche Lauf prueft erneut.
   - Mindestens ein Check rot (und keine eigene Aenderung noetig, siehe Schritt 5 fuer den Reparaturfall): nicht mergen, einmalig kommentieren (Marker "[dependabot-routine]" pruefen, um Spam zu vermeiden).

5. Kritische PRs danach einzeln abarbeiten, mit dem Ziel, sie tatsaechlich zu mergen:
   a. `gh pr checkout <n>` - den PR-Branch auschecken.
   b. `gh pr view <n> --json title,body` lesen. Zwei Dinge daraus: erstens die Versionsspruenge als Zeilen "<koordinate> <von> <nach>" (die Koordinate so, wie Dependabot sie nennt - Plugin-ID wie "org.springframework.boot" oder volle Koordinate wie "org.testcontainers:postgresql"); zweitens die Release Notes/den Changelog, die Dependabot dort meist anhaengt - sie bleiben die Quelle fuer alles, was kein Rezept abdeckt.
   c. Erst ohne eigene Aenderung pruefen, ob es schon durchlaeuft: bei gradle/npm-PRs gestuft wie der /pruefen-Skill (compileJava, dann test, dann archTest, dann voller `./gradlew check`), bei github-actions-PRs gibt es keinen lokalen Testlauf - hier zaehlt nur der Blick in die Release Notes auf Breaking Changes.
   d. Laeuft es nicht durch bzw. verlangen die Release Notes eine Anpassung: ZUERST das Rezept, nicht die Handarbeit.
      - Trocken durchrechnen: `printf 'org.springframework.boot 3.5.16 4.0.1\n' | ci/openrewrite-anwenden.sh --trocken` (eine Zeile je Sprung der PR). Den Patch unter build/reports/rewrite/rewrite.patch ansehen.
      - Exit 4 ("Kein Rezept anzuwenden"): kein Rezept fuer diesen Sprung - direkt weiter mit e, Handarbeit wie bisher. Das ist der Normalfall fuer npm und Actions-Tags und kein Fehler.
      - Passt der Patch zum Sprung: dasselbe Kommando ohne `--trocken` erneut aufrufen; es schreibt die Aenderung und zeigt danach `git diff --stat`.
      - Den erzeugten Diff LESEN, nicht blind uebernehmen. Ein Rezept aendert gelegentlich mehr, als der Sprung erzwingt (etwa Stilregeln aus einem eingebetteten Teilrezept, `list.get(0)` -> `list.getFirst()`). Alles, was der Sprung nicht verlangt, gezielt zuruecknehmen (`git checkout -- <datei>` bzw. die einzelne Stelle). Die Grenze "nur was der Sprung erzwingt" gilt fuer ein Rezept genauso wie fuer Handarbeit.
      - Ein Rezept kennt die Regeln dieses Projekts nicht - Ringe (ADR-024), Stereotypen (ADR-027), JSpecify-Nullness (ADR-026), die sieben harten Invarianten. Der Rezeptlauf ist ein Vorschlag; ueber "gruen" entscheiden weiterhin ausschliesslich die Gates und Schritt f.
      - Findest du zu einem Major-Sprung ein passendes, tatsaechlich veroeffentlichtes OpenRewrite-Rezept, das im Katalog von ci/openrewrite-anwenden.sh fehlt: den Katalog im selben PR ergaenzen (kurzer Kommentar dazu, warum). Den Rezeptnamen dabei gegen die Rezeptsammlung pruefen, nicht aus dem Gedaechtnis schreiben - ein erfundener Name laesst den Lauf rot werden.
   e. Was das Rezept nicht abgedeckt hat (oder wenn es keines gab): minimal-invasive Aenderung von Hand, die exakt das abbildet, was der Versionssprung erzwingt - kein Refactoring, keine Verbesserung nebenbei, keine neue Abstraktion ueber das Notwendige hinaus. Bestehende Konventionen aus CLAUDE.md einhalten (Ringe/Abhaengigkeitsrichtung, DDD-Stereotypen, JSpecify-Nullness, keine Mockito, Kommentare nur fuers Warum).
   f. Betrifft die Aenderung - vom Rezept erzeugt oder von Hand gemacht - Code unter src/main/java/de/fourteen/watchparty/domain, .../application oder .../adapter (nicht nur Build-/CI-Konfiguration wie build.gradle.kts oder .github/workflows): den Skill /invarianten-review ueber das Skill-Werkzeug aufrufen und die Aenderung gegen die sieben harten Invarianten aus CLAUDE.md pruefen lassen. Findet die Pruefung einen plausiblen, nicht restlos ausgeraeumten Verstoss: nicht mergen, weiter mit Schritt 6. Dass eine Aenderung aus einem Rezept stammt, ist dabei kein Freibrief - sie wird genauso geprueft wie eine handgemachte.
   g. Bis zu zwei Fix-und-erneut-pruefen-Zyklen pro PR versuchen, danach abbrechen, wenn weiterhin rot.
   h. Laeuft `./gradlew check` vollstaendig gruen (und bei domain-nahen Aenderungen die Invarianten-Pruefung ohne Befund): eigene Aenderungen committen (Praefix chore/ci passend zum Oekosystem, NIEMALS fix/feat/perf - siehe Nebenbedingung oben; im Commit-Text nennen, welches Rezept gelaufen ist, falls eines lief) und auf den PR-Branch pushen (`git push`), danach `gh pr merge <n> --squash --subject "<Original-PR-Titel oder kurze eigene Zusammenfassung mit chore:/ci:-Praefix>" --delete-branch=false --repo ajFourteen/Watchparty`.

6. Kritische PRs, die nicht gruen werden (Schritt 5g ausgeschoepft), deren noetige Aenderung eine echte Architektur-/Designentscheidung ist statt einer mechanischen Anpassung, oder bei denen die Invarianten-Pruefung einen Befund hat: NICHT mergen. Einmalig kommentieren (Marker "[dependabot-routine]" pruefen, um Spam zu vermeiden): welcher Sprung Major ist, ob ein Rezept lief und welches, was es abgedeckt hat und was nicht, woran es konkret hakt (z. B. welcher Test/welche Regel weiterhin rot ist oder welcher Invarianten-Befund offen ist), und dass ein Mensch das jetzt uebernehmen muss. Vor dem Verlassen der PR: `git checkout main` und einen ggf. lokal veraenderten Branch-Zustand nicht liegen lassen - insbesondere keine vom Rezept geschriebenen, nicht committeten Aenderungen und keinen Rebase-/Merge-Rest im Arbeitsverzeichnis (`git status` muss sauber sein).

7. Fuer jede in diesem Lauf tatsaechlich gemergte PR (unkritisch oder repariert-kritisch): den daraufhin auf main gestarteten CI-Lauf beobachten (`gh run list --repo ajFourteen/Watchparty --branch main --limit 10`, die zum Merge-Commit passenden Laeufe finden - bei github-actions-Bumps koennen mehrere Workflows betroffen sein, nicht nur build.yml - dann bis zum Abschluss pruefen, hoechstens ca. 15 Minuten pro Lauf warten). Schlaegt ein Lauf fehl: einen Kommentar auf den Merge-Commit setzen (`gh api repos/ajFourteen/Watchparty/commits/<sha>/comments -f body="..."`) und es im Abschlussbericht deutlich als Fehlschlag hervorheben - nicht stillschweigend weitermachen.

8. Abschlussbericht als Text am Ende deiner Antwort (keine Datei): jede betrachtete Dependabot-PR mit Nummer, Titel, Einstufung (unkritisch/kritisch), bei kritischen zusaetzlich, ob und welches OpenRewrite-Rezept lief und was danach noch von Hand noetig war (bzw. "kein Rezept im Katalog"), Aktion (gemergt ohne Aenderung / gemergt nach Reparatur mit Beschreibung der Aenderung / uebersprungen wegen ausstehender Checks / uebersprungen und kommentiert - mit Grund) und Pipeline-Status jeder gemergten PR.

Harte Grenzen:
- Niemals eine PR mergen, die nicht von "app/dependabot" stammt.
- Niemals ein Gate schwaechen, um Gruen zu erzwingen: kein @Disabled, kein @AequivalenterMutant ohne echten, im Sinne von docs/test-ausnahmen.md gerechtfertigten Eintrag, keine Mutationsschwelle oder Deckungsgrenze absenken, keine ArchitectureTest-Regel aufweichen oder eine Klasse stillschweigend davon ausnehmen.
- Niemals den Commit-Typ auf fix/feat/perf setzen - immer chore/ci, auch wenn Code angepasst wurde (siehe Nebenbedingung oben, ADR-019).
- Codeaenderungen bleiben strikt auf das beschraenkt, was der Versionssprung selbst erzwingt - keine unabhaengigen Refactorings. Das gilt fuer den Diff eines Rezepts genauso: was er darueber hinaus anfasst, wird zurueckgenommen, nicht mitgenommen.
- Niemals ein Rezept anwenden, das nicht zum Sprung dieser PR gehoert - kein "wo wir schon dabei sind". Insbesondere keine reinen Aufraeum-/Stil-Rezepte (rewrite-static-analysis o. ae.); sie stehen bewusst nicht auf der rewrite-Konfiguration in build.gradle.kts.
- Springt das OpenRewrite-Plugin oder eine Rezeptsammlung selbst auf einen neuen Major, ist das Handarbeit - fuer das eigene Werkzeug gibt es kein Rezept. Scheitert so ein Bump daran, dass die passende rewrite-bom noch nicht auf Maven Central liegt (ADR-042 nennt genau diesen Fall), ist das kein Codeproblem: liegen lassen, einmalig kommentieren, nicht reparieren.
- Niemals force-pushen, Branch-Protection aendern oder andere offene PRs anfassen.
- Bei Unsicherheit, ob eine Aenderung mechanisch ist oder eine echte Designentscheidung braucht: als nicht loesbar behandeln (Schritt 6), nie raten und trotzdem mergen.
```
