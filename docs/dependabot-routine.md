# Die Dependabot-Routine

Der Prompt der täglichen Routine „Merge critical Dependabot PRs using
OpenRewrite" (claude.ai/code/routines, `trig_019nySXHRAv9xJjDrgT8TCm7`,
täglich 02:00 UTC, angelegt am 2026-08-31 07:32 UTC). Anders als ihre beiden
Vorgängerinnen weckt sie keine bestehende Sitzung, sondern erzeugt bei
jedem Lauf eine frische, mit dem Repository direkt am Trigger als Quelle
(`job_config.ccr.session_context.sources`) — kein Umweg mehr über eine
dauerhaft gebundene Sitzung.

Zwei Vorgängerinnen liegen dahinter, beide nicht mehr in der
Trigger-Übersicht:

- `trig_01SgSa2dwKv7cpiirFASUDyz` (ohne OpenRewrite) — am 2026-08-31 von
  Hand gelöscht, weil ein Agent sie nicht abschalten konnte: über die
  Weboberfläche angelegt, und `update_trigger` lehnt eine fremde Routine ab
  („Agents can only update routines they created"). Bis dahin liefen
  morgens zwei Agenten auf dieselben Pull-Requests.
- `trig_01FHkz4oeYEVXVqM12YWM5EU` (mit OpenRewrite, gebunden an die feste
  Sitzung `session_01Bnqk9B4NNgedJCoyJ3YQeX`) — der Ansatz, den dieses
  Dokument bis zum 2026-08-31 als aktuellen beschrieb (Abschnitt unten).
  Ersetzt durch die jetzige Routine, aus demselben Grund wie oben: Ersatz
  heißt selbst abräumen, das erledigt kein Agent automatisch.

Der Punkt bleibt für jede künftige Ablösung gültig: Wer eine Routine
ersetzt, muss die alte selbst abräumen.

## Warum jetzt eine frische Sitzung je Lauf reicht

Die ursprüngliche Annahme war: Eine Routine bekommt ihre Git-Quelle
ausschließlich beim Anlegen einer *Sitzung*, nicht beim Anlegen einer
Routine — die Werkzeuge, mit denen ein Agent Routinen anlegt und ändert
(`create_trigger`/`update_trigger`), kennen bis heute keinen Parameter für
ein Repository, egal ob die Routine eine bestehende Sitzung weckt oder bei
jedem Lauf eine neue erzeugt. Der erste Versuch überhaupt
(`trig_01DNECwWbfL5oRRXpAmsmFMc`, am 2026-08-30 angelegt und wieder
gelöscht) fiel genau darüber: eine frische Sitzung ohne Repo-Angabe stand
ohne Checkout da. Die Konsequenz daraus war `trig_01FHkz4oeYEVXVqM12YWM5EU`
— eine dauerhafte Sitzung mit Repo-Quelle, täglich neu geweckt, mit allen
Folgen einer nicht-frischen Sitzung (hartes Rücksetzen als erster Schritt,
voller Regeltext bei jedem Weckruf, siehe Git-Historie dieser Datei).

Die aktuelle Routine zeigt, dass diese Konsequenz nicht nötig war: Über die
Weboberfläche lässt sich ein Repository auch direkt an eine Routine hängen,
die bei jedem Lauf eine *neue* Sitzung erzeugt (`job_config.ccr` trägt
`environment_id` und `session_context.sources` unmittelbar, ohne
`persistent_session_id`) — nur die Agent-Werkzeuge (`create_trigger`) bieten
dafür weiterhin keinen Parameter. Jeder Lauf startet damit bei einem
echten, sauberen Checkout; die drei Punkte, die der alte Weckruf-Vorspann
deshalb voranstellte (Rücksetzen, Hook-Hinweis, „Regeln verjähren nicht"),
entfallen dadurch nicht als Sorge, sondern als Notwendigkeit — es gibt
schlicht keinen Vortagszustand mehr, auf den sich zurücksetzen ließe.

Weiterhin unverändert: Der Prompt lässt sich nur über die Weboberfläche mit
einer Repo-Quelle versehen. Jede Textänderung heißt also weiterhin: hier
ändern, committen, dann in der Weboberfläche einfügen — bei dieser Routine
reicht dafür (anders als bei der alten) vermutlich ein einfaches
Bearbeiten des Prompts über `update_trigger`, sofern die Routine vom
richtigen Konto aus angelegt wurde; ungetestet, da diese Datei bislang nur
per Löschen-und-Neuanlegen aktuell gehalten wurde.

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

Der Weckruf-Vorspann aus der Zeit der festen Sitzung (Rücksetzen auf
`origin/main`, Hinweis auf `ci/git-regeln-hook.sh`, „Regeln verjähren
nicht") ist hier bewusst nicht mehr Teil des Prompts: Jeder Lauf bekommt
jetzt ohnehin einen frischen Checkout, es gibt keinen Vortagszustand, auf
den zurückzusetzen wäre. Der Hook selbst gilt natürlich unverändert für
jede Sitzung, die ihn im Repository vorfindet — er muss dem Agenten nur
nicht mehr eigens vorab erklärt werden, da er in `gh pr checkout` und den
übrigen Schritten unten ohnehin nicht im Weg steht (siehe Commit 316f56f).

---

```text
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
   a. `gh pr checkout <n>` - den PR-Branch auschecken. Danach SOFORT den Basis-Branch nachziehen: `git fetch origin main && git merge origin/main`. Dependabot baut den Branch auf dem main-Stand von damals; alles Folgende laeuft sonst gegen diesen alten Stand - einschliesslich `ci/openrewrite-anwenden.sh` selbst, das aus dem ausgecheckten Branch kommt und nicht von main. Genau daran waere der Lauf vom 31.08. an PR #7 ein zweites Mal gescheitert: Die Entkopplung vom Kompilieren lag auf main, auf dem PR-Branch stand noch die alte Fassung des Skripts. Laesst sich der Merge nicht ohne Ermessen aufloesen (beide Seiten haben dieselbe Stelle geaendert): abbrechen (`git merge --abort`) und weiter mit Schritt 6.
   b. `gh pr view <n> --json title,body` lesen. Zwei Dinge daraus: erstens die Versionsspruenge als Zeilen "<koordinate> <von> <nach>" (die Koordinate so, wie Dependabot sie nennt - Plugin-ID wie "org.springframework.boot" oder volle Koordinate wie "org.testcontainers:postgresql"); zweitens die Release Notes/den Changelog, die Dependabot dort meist anhaengt - sie bleiben die Quelle fuer alles, was kein Rezept abdeckt.
   c. Erst ohne eigene Aenderung pruefen, ob es schon durchlaeuft: bei gradle/npm-PRs gestuft wie der /pruefen-Skill (compileJava, dann test, dann archTest, dann voller `./gradlew check`), bei github-actions-PRs gibt es keinen lokalen Testlauf - hier zaehlt nur der Blick in die Release Notes auf Breaking Changes.
   d. Laeuft es nicht durch bzw. verlangen die Release Notes eine Anpassung: ZUERST das Rezept, nicht die Handarbeit.
      - Trocken durchrechnen: `printf 'org.springframework.boot 3.5.16 4.0.1\n' | ci/openrewrite-anwenden.sh --trocken` (eine Zeile je Sprung der PR). Den Patch unter build/reports/rewrite/rewrite.patch ansehen.
      - Exit 4 ("Kein Rezept anzuwenden"): kein Rezept fuer diesen Sprung - direkt weiter mit e, Handarbeit wie bisher. Das ist der Normalfall fuer npm und Actions-Tags und kein Fehler.
      - Passt der Patch zum Sprung: dasselbe Kommando ohne `--trocken` erneut aufrufen; es schreibt die Aenderung und zeigt danach `git diff --stat`.
      - Den erzeugten Diff LESEN, nicht blind uebernehmen. Ein Rezept aendert gelegentlich mehr, als der Sprung erzwingt (etwa Stilregeln aus einem eingebetteten Teilrezept, `list.get(0)` -> `list.getFirst()`). Alles, was der Sprung nicht verlangt, gezielt zuruecknehmen (`git checkout -- <datei>` bzw. die einzelne Stelle). Die Grenze "nur was der Sprung erzwingt" gilt fuer ein Rezept genauso wie fuer Handarbeit.
      - Ein Rezept kennt die Regeln dieses Projekts nicht - Ringe (ADR-024), Stereotypen (ADR-027), JSpecify-Nullness (ADR-026), die sieben harten Invarianten. Der Rezeptlauf ist ein Vorschlag; ueber "gruen" entscheiden weiterhin ausschliesslich die Gates und Schritt f.
      - Ein Rezept fasst nur an, was es als Muster kennt. Was der alte Major an Versionen FESTGESCHRIEBEN hat, wandert nicht mit: geh nach dem Rezeptlauf build.gradle.kts gezielt auf hartkodierte Versionen und Pins durch (resolutionStrategy/useVersion, force, explizite Versionen an Abhaengigkeiten, die sonst aus dem Dependency-Management kommen) und zieh sie auf den Stand, den die neue Hauptversion managt. Belegt am 31.08. an Spring Boot 4.1.1: Der JUnit-Pin stand noch auf Boots 3er-Stand (5.12.2/1.12.2) und liess JEDEN @SpringBootTest mit NoSuchMethodError abbrechen, weil SpringExtension aus Spring Framework 7 die JUnit-6-Signatur von ExtensionContext.Store.computeIfAbsent ruft.
      - Verlass dich fuer die Bewertung eines Rezeptlaufs nie auf "kompiliert wieder". Der Rezeptlauf selbst laeuft mit -x compileJava; was er offen laesst, faellt oft erst eine Stufe spaeter auf - beim Kompilieren der Tests, beim Testlauf oder erst im laufenden Kontext. Deshalb gestuft weiter wie in c (compileJava, compileTestJava, test, archTest, voller check), und nicht nach dem ersten gruenen Schritt aufhoeren. Ebenfalls am 31.08.: Nach dem Rezept kompilierte der Produktivcode, aber TestRestTemplate fehlte als Bean (Boot 4 verlangt @AutoConfigureTestRestTemplate aus spring-boot-resttestclient) - sichtbar erst im Testlauf.
      - Aendert ein Rezept eine Exception-Signatur mit, pruefe die zugehoerigen catch-Bloecke auf ihre WIRKUNG, nicht nur auf ihre Uebersetzbarkeit. Eine vormals gepruefte Ausnahme, die ungeprueft wird, kompiliert weiter und entkommt trotzdem - in einem eigenen Thread heisst das: der Thread stirbt still. Am 31.08. betraf das SnapshotStore.writeToDisk (Jackson 3 wirft JacksonException ungeprueft statt JsonProcessingException als IOException); ohne das erweiterte Multi-Catch waere der Snapshot-Thread bei einem Serialisierungsfehler beendet worden, Invariante 2.
      - Findest du zu einem Major-Sprung ein passendes, tatsaechlich veroeffentlichtes OpenRewrite-Rezept, das im Katalog von ci/openrewrite-anwenden.sh fehlt: den Katalog im selben PR ergaenzen (kurzer Kommentar dazu, warum). Den Rezeptnamen dabei gegen die Rezeptsammlung pruefen, nicht aus dem Gedaechtnis schreiben - ein erfundener Name laesst den Lauf rot werden.
   e. Was das Rezept nicht abgedeckt hat (oder wenn es keines gab): minimal-invasive Aenderung von Hand, die exakt das abbildet, was der Versionssprung erzwingt - kein Refactoring, keine Verbesserung nebenbei, keine neue Abstraktion ueber das Notwendige hinaus. Bestehende Konventionen aus CLAUDE.md einhalten (Ringe/Abhaengigkeitsrichtung, DDD-Stereotypen, JSpecify-Nullness, keine Mockito, Kommentare nur fuers Warum).
   f. Betrifft die Aenderung - vom Rezept erzeugt oder von Hand gemacht - Code unter src/main/java/de/fourteen/watchparty/domain, .../application oder .../adapter (nicht nur Build-/CI-Konfiguration wie build.gradle.kts oder .github/workflows): den Skill /invarianten-review ueber das Skill-Werkzeug aufrufen und die Aenderung gegen die sieben harten Invarianten aus CLAUDE.md pruefen lassen. Findet die Pruefung einen plausiblen, nicht restlos ausgeraeumten Verstoss: nicht mergen, weiter mit Schritt 6. Dass eine Aenderung aus einem Rezept stammt, ist dabei kein Freibrief - sie wird genauso geprueft wie eine handgemachte.
   g. Bis zu zwei Fix-und-erneut-pruefen-Zyklen pro PR versuchen, danach abbrechen, wenn weiterhin rot.
   h. Laeuft `./gradlew check` vollstaendig gruen (und bei domain-nahen Aenderungen die Invarianten-Pruefung ohne Befund): eigene Aenderungen committen (auch wenn keine noetig waren, will der Merge-Commit aus Schritt a gepusht werden; Praefix chore/ci passend zum Oekosystem, NIEMALS fix/feat/perf - siehe Nebenbedingung oben; im Commit-Text nennen, welches Rezept gelaufen ist, falls eines lief) und auf den PR-Branch pushen (`git push`), danach `gh pr merge <n> --squash --subject "<Original-PR-Titel oder kurze eigene Zusammenfassung mit chore:/ci:-Praefix>" --delete-branch=false --repo ajFourteen/Watchparty`.

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
