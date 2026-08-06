# Umsetzung der Teststrategie — Arbeitsplan

Dieses Dokument ist **vorübergehend**. Es beschreibt den Weg von der
heutigen Testlage zu dem, was `teststrategie.md` festlegt, und wird
gelöscht, sobald Phase 5 abgeschlossen ist. Die dauerhaften Regeln stehen
dort, nicht hier.

## Ausgangslage (gemessen am 2026-08-05)

- **95 Tests in 14 Klassen, alle grün.** Der Bestand ist brauchbar und wird
  umgebaut, nicht weggeworfen.
- **Die Pipeline führt keinen einzigen Test aus.** `release.yml` macht
  Semantic Release und Deploy; `gradle bootJar` zieht `test` nicht an.
- **Der Stack läuft seit ADR-029 durchgehend auf Java 25.** Der Umstieg war
  Voraussetzung für alles Weitere: Gradle 8.10.2 lief nicht auf JDK 25, und
  die Kette zog Gradle 9.6.1, ArchUnit 1.4.1 und Spring Boot 3.5.16 nach
  sich. Für die Pipeline heißt das: Die Java-Version wird ausdrücklich
  gesetzt, nicht vom Runner geerbt.
- **Ein bekannter Wackelkandidat.**
  `RestoreTest.wiederherstellungMitOffenerRundeInDerZukunft…` war einmal in
  zehn Läufen rot und danach nicht mehr reproduzierbar (ADR-029). Nach der
  Strategie ist das ein Fehlschlag, kein Wiederholungsfall — er wird in
  Phase 3.3 beim Umbau von `RestoreTest` aufgelöst, nicht vorher
  weggedrückt.
- **Nicht vorhanden:** JGiven, jqwik, JaCoCo, PIT, Anforderungs-Tags,
  Kritikalitäts-Annotationen, Ebenen-Trennung.
- **Vorhanden und tragfähig:** ArchUnit mit acht Regeln, die
  handgeschriebenen Test Doubles (`FakeClock`, `FakeScheduler`,
  `RecordingClientGateway`, `NoSnapshots`, `FakeWebSocketSession`), ein
  echter Rauchtest über die Leitung.
- **Zu belegen sind 60 Regeln.** Anhang A der Anforderungen enthält 73
  Regeln: 60 `backend`, 6 `frontend`, 5 `organisatorisch`, 2 `beobachtung`.

## Reihenfolge und ihr Grund

Fünf Phasen, jede für sich abgeschlossen und nützlich. Die Reihenfolge ist
nicht beliebig:

**Phase 0 kommt zuerst, weil ohne sie alles Weitere unverbindlich bleibt.**
Solange die Pipeline keine Tests ausführt, ist jede Regel dieser Strategie
eine Verabredung. Der Aufwand ist eine halbe Stunde, der Effekt der größte
im ganzen Plan.

**Die Metriken kommen zuletzt**, weil sie ohne Szenarien nichts messen. Eine
Mutationsschranke auf einer ungetesteten Klasse erzeugt nur eine rote Zahl,
die niemandem sagt, was zu tun ist.

---

## Phase 0 — Die Pipeline führt Tests aus

1. Neuer Workflow `.github/workflows/build.yml`: läuft auf `push` und
   `pull_request`, setzt mit `actions/setup-java@v4` **Java 25** (siehe
   Ausgangslage), führt `./gradlew check -PskipFrontend` aus. Das Frontend
   bleibt außen vor — es wird im Docker-Build ohnehin getrennt gebaut.
2. `release.yml`: Der `release`-Job bekommt `needs: build`. Ein roter Test
   verhindert damit Release *und* Deploy.
3. Testbericht als Artefakt hochladen, damit ein Fehlschlag ohne lokalen
   Nachbau lesbar ist.

**Fertig, wenn** ein absichtlich rot gemachter Test den Release verhindert.

**Risiko:** keines von Belang — der Bestand ist grün, das ist nachgemessen.

---

## Phase 1 — Gerüst: Ebenen, JGiven, Dialekt

1. Abhängigkeiten: `jgiven-junit5` und das JGiven-Gradle-Plugin für den
   HTML-Report, `jqwik` für die Property-Tests.
2. **Meta-Annotationen je Ebene** — je eine Annotation, die den JUnit-Tag
   und den JGiven-Report-Tag zusammen trägt, damit beides nicht auseinander
   laufen kann: `@UnitTest`, `@PortTest`, `@AdapterTest`, `@ApiTest`.
3. **Gradle-Tasks je Ebene** über Tag-Filter: `test` (unit, port, arch —
   der schnelle Lauf), `adapterTest`, `apiTest`. `check` hängt alle ein.
   Die Test Doubles bleiben in einem gemeinsamen Quellbaum; genau dafür sind
   es Tags und keine Source Sets.
4. **Deutsche Basisklasse** mit den Einstiegen `angenommen()`,`wenn()`, `dann()`.
5. **Stufen-Paket** einrichten, dazu die ArchUnit-Regel, dass jede
   JGiven-`Stage` dort liegt — das ist die Grenze der Sprachausnahme.
6. **Je ein Pilotszenario pro Ebene**, um das Gerüst zu beweisen statt es zu
   behaupten: 8.1-c (gekappte Strafe) auf Domänenebene, 8.1-b (eingefrorener
   Teilnehmerkreis) auf Port-Ebene, der Snapshot-Round-Trip auf
   Adapter-Ebene, ein vollständiger Rundenablauf auf API-Ebene.
7. **ADR-030** schreiben: die Teststrategie als Entscheidung, einschließlich
   der Sprachausnahme für die Stufenklassen und ihrer strukturellen Grenze.

**Fertig, wenn** `./gradlew check` einen JGiven-Report mit vier
Pilotszenarien erzeugt und ArchUnit das Stufen-Paket bewacht. **Erledigt** —
siehe ADR-030.

**Offene Detailfrage — entschieden (ADR-030):** Die Abschnittsüberschriften
des HTML-Reports lassen sich mit JGiven 2.0.3 nicht auf Deutsch umstellen
(`jgiven-html-app` hat dafür keinen Lokalisierungs-Hook, geprüft durch
Zerlegen von `app.bundle.js`). Sie bleiben englisch, die Schritttexte sind
deutsch — kosmetisch unschön, inhaltlich unkritisch.

---

## Phase 2 — Rückverfolgbarkeit und Kritikalität

1. **`@Anforderung`** als JGiven-`@IsTag` einführen.
2. **Der Abdeckungsbericht.** Ein Gradle-Task `abdeckung` liest nach dem
   Testlauf die JGiven-Ergebnisse und die Tabellen aus Anhang A und bildet
   die Differenz. Bewusst ein nachgelagerter Task und kein Test: Gezählt
   werden soll, was *grün gelaufen* ist, nicht was annotiert ist — sonst
   belegt ein fehlschlagendes Szenario weiterhin seine Regel.
3. **Der Bericht ist zunächst nur ein Bericht.** Er meldet die Lücke, aber
   er bricht den Build nicht ab. Scharf gestellt wird er am Ende von
   Phase 3. Grund: Bei 60 offenen Regeln wäre der Build ab Tag eins rot,
   und eine Mannschaft, die sich an Rot gewöhnt, liest auch das echte Rot
   nicht mehr. Der Übergang ist terminiert, nicht optional.
4. **`@Criticality`** anlegen: eigenes, abhängigkeitsfreies Paket, anwendbar
   auf Typ und Methode, mit `level` und `requirements`. Dazu die
   ArchUnit-Regel, dass dort nur Annotationen liegen.
5. Die Einstufung aus Abschnitt 6.4 der Strategie an den Code anbringen.

**Fertig, wenn** `./gradlew abdeckung` die namentliche Liste der noch
unbelegten `backend`-Regeln ausgibt. **Erledigt** — Stand nach Phase 2: 55
offen, 5 belegt (die vier Pilotszenarien aus Phase 1 tragen inzwischen
`@Anforderung`: 8.1-c, 8.1-a, 8.1-b, 1-d, 9-a). Die restlichen 55 sind
Phase 3.

---

## Phase 3 — Die Szenarien nachrüsten

Der große Brocken. Vorgehen in jedem Teilschritt gleich, und die Reihenfolge
ist wichtig:

> **Erst die Szenarien aus Anhang A schreiben, dann nachsehen, welcher
> vorhandene Test sie schon abdeckt.** Nicht umgekehrt.

Das ist die Sicherung gegen die Characterization-Falle: Wer vom Code
ausgeht, schreibt dessen Verhalten fest, auch wo es falsch ist. Ein
vorhandener Test, der zu keiner Regel passt, ist ein Fund — entweder fehlt
die Anforderung (dann nach `offene-entscheidungen.md`), oder der Test prüft
ein Implementierungsdetail (dann entfällt er).

**3.1 Domäne, Kritikalität HIGH.** Abrechnung und Anteile: 7.1, 7.1-a, 7.2,
8.2, 8.2-a, 8.3, 8.5, 8.1-c. Dazu die Property-Tests für 2-c, 2-d, 7.1, 7.2
und 8.1-c. Der handgeschriebene Zufallstest in `SettlementTest`
(`new Random(42)`) wird dabei abgelöst. **Erledigt.** Nebenbei aufgefallen:
Property-Tests brauchen zusätzlich `net.jqwik.api.Tag`, sonst laufen sie am
Tag-Filter des `test`-Tasks vorbei, ohne Fehlermeldung (ADR-030).

**3.2 Port, Kritikalität HIGH.** Verdeckte Tipps: 6-b und 9-b. Hier
entstehen die **Leck-Tests auf Nachrichtenebene** und die
**Invariantenprüfung nach jedem Schritt** (kein Konto negativ, Punktesumme
erhalten, Deltasumme null beim Auflösen). **Erledigt.**

**3.3 Port, Kritikalität MEDIUM.** Rundenablauf (9-a bis 9-c, 5-a bis 5-d),
Strafen und Teilnehmerkreis (8.1 bis 8.1-e), Annullieren (8.6 ff., dazu 8.4),
Zurücksetzen (8.7 ff.), Wettmechanik (6-a, 6-c, 6-e, 6-f), Rollen (10-a,
10-b), Host-Rolle (10.1 ff.), Reconnect und Wiederanlauf (1-c). Die Zeit-
und Reihenfolge-Szenarien gehören hierher, allen voran die Rundenwache aus
ADR-010. **Erledigt** — Stand danach: 41 von 60.

`getRoomForTest()` ist **entfernt**; `RoomActorStateMachineTest`,
`ReconnectTest`, `RestoreTest` und `RoomActorResetTest` sind gelöscht,
ersetzt durch `RundenwacheScenarioTest`, `PauseScenarioTest`,
`AnnullierenScenarioTest`, `ZuruecksetzenScenarioTest`, `RollenScenarioTest`,
`ReconnectScenarioTest` und `WiederanlaufScenarioTest` (alle über die Ports).
Der in ADR-029 dokumentierte sporadische Fehlschlag von `RestoreTest`
(`wiederherstellungMitOffenerRundeInDerZukunft…`) ist dabei tatsächlich
aufgetreten und aufgelöst: `shutdown()` auf Actor/Store ohne vorheriges
`awaitWritten()` lässt den Schreib-Thread mitten im Schreiben stehen, `@TempDir`
räumt dann gegen eine Datei auf, die noch entsteht. `WiederanlaufStufen` wartet
jetzt vor jedem `shutdown()` explizit auf das Ende des Schreibvorgangs.

Eine Lücke im `abdeckung`-Task selbst kam dabei ans Licht: Property-Tests
erscheinen nie als JGiven-Szenario im Report (Abschnitt 2.1), waren also für
den ursprünglich nur JSON-lesenden Task unsichtbar, obwohl grün und korrekt
verknüpft (2-c/2-d fehlten trotz vorhandener Szenarien). Der Task liest
`@Anforderung`-Methoden jetzt per Reflection aus den kompilierten
Testklassen und gleicht sie mit den JUnit-XML-Berichten ab — ein Weg für
JGiven-Szenarien und jqwik-Properties gleichermaßen.

Bewusst nicht 1:1 nachgebaut: ein paar reine Implementierungs-Regressionstests
ohne eigene Anhang-A-ID (doppeltes Schließen ist ein No-op, ein Tipp exakt
auf `closesAt` zählt nicht mehr). Ihre Abwesenheit ist eine bewusste
Abwägung angesichts des Umfangs, kein Versehen — sie betreffen keine offene
`backend`-Regel. ("Ein zweites Öffnen während einer laufenden Runde bleibt
bei der einen Runde" ist dagegen doch nachgebaut, als 1-b in
`RundenwacheScenarioTest`.)

**Restlücken geschlossen.** Nach 3.1–3.3 fehlten noch Katalog-Struktur
(4-a bis 4.4-a), Pool/Gewinnaufteilung (2-a, 2-b), Ganzzahligkeit/Kontostand
(3-b, 3-e) und Beitrittsgrundlagen (1-b, 1-e, 3-a, 3-c) — nachgetragen auf
bestehende oder neue, kleine Szenarien/Tests. Dabei ein echter Befund:
3.1-a stimmte nicht (Startguthaben stand getrennt von `Params`), durch
Nutzer-Entscheidung aufgelöst (`Params` bekommt ein drittes Feld,
`docs/offene-entscheidungen.md`). Abdeckung seither 60 von 60.

**3.4 Adapter.** Fixtures werden am Port-Datentyp konstruiert, nicht durch
die Domäne erzeugt. Snapshot-Round-Trip über einen **vollständig gefüllten**
Raum; jeder Nachrichtentyp serialisiert; kaputte und unvollständige Frames
töten weder Verbindung noch Raum-Thread. **Erledigt.** Der Snapshot-Round-Trip
aus Phase 1 (`SnapshotRoundTripScenarioTest`) war schon vollständig genug
(zwei Spieler, einer mit `missedRounds > 0`, eine aufgelöste Runde mit
Tipps und Deltas); neu sind `GameWebSocketHandlerTest` (kaputtes JSON,
unbekannter Typ, fehlendes Typ-Feld, fehlende Pflichtfelder — keins davon
tötet Verbindung oder Kommando-Fluss) und `WebSocketClientGatewayTest`
(jeder `Messages`-Typ inklusive der RESOLVED-spezifischen Felder
serialisiert vollständig).

**3.5 API.** Ein durchgängiger Rundenablauf, der **Leck-Test am
serialisierten JSON** (der bestehende `WireProtocolSmokeTest` ist die
Grundlage), die Vollständigkeit des Zustands nach Reconnect und die
nicht-funktionale Prüfung: Ein Client, der nicht mehr liest, hält die
anderen nicht auf. **Erledigt**, alle drei als neue Testmethoden in
`WireProtocolSmokeTest`: der Leck-Test vergleicht die tatsächlichen
JSON-Feldnamen eines OPEN-Frames gegen dieselbe Positivliste wie auf der
Port-Ebene (findet, was erst durch Jackson entsteht); der
Reconnect-Test verbindet über einen zweiten echten Socket mit dem alten
Token und prüft Spieler-ID und vollständigen Zustand; die NFR-Prüfung lässt
einen Client abrupt abbrechen und zeigt, dass ein zweiter normal
weiterläuft (das Blockieren einer einzelnen Ausgangs-Queue bei einem
tatsächlich langsamen Client bleibt Sache von ADR-012 und
`ClientSessionTest`).

Dabei ein echter Fund: Tomcat wirft beim Schreiben in eine gerade
schliessende Session eine `IllegalStateException`, keine `IOException` --
`ClientSession.drain()` fing nur Letztere ab, die Ausnahme verliess den
Sende-Pool-Thread unbehandelt. Mit Regressionstest behoben.

**Fertig, wenn** `./gradlew abdeckung` null offene `backend`-Regeln meldet.
**Unmittelbar danach**: den Bericht aus Phase 2 zum Gate machen. **Beides
erledigt** — `check` hängt jetzt von `abdeckung` ab, 60 von 60.

**Risiko:** der Umfang. Gegenmittel ist die Reihenfolge — nach jedem
Teilschritt ist ein sinnvoller, committbarer Zustand erreicht, und die
kritischsten Regeln sind zuerst belegt. Wer hier abbricht, hat trotzdem das
Wertvollste.

---

## Phase 4 — Metriken scharf stellen

1. **JaCoCo** je Ebene erheben und als Artefakt ablegen — ohne
   Prozentschranke. **Erledigt** in `build.gradle.kts` (Commit
   `d1ac56d`): drei `JacocoReport`-Tasks (`jacocoTestReport`,
   `jacocoAdapterTestReport`, `jacocoApiTestReport`), je eigene
   Ausführungsdaten, XML+HTML, in `check` verdrahtet.
2. **Die Ebenen-Disjunktheit** aus 7.4: Differenz der abgedeckten
   Domänenzeilen zwischen den Ebenen. Deckt ein Adapter- oder API-Test eine
   Domänenzeile ab, die keine innere Ebene abdeckt, ist das eine Lücke
   weiter innen. **Erledigt** — Task `ebenenDisjunktheit`, liest die drei
   JaCoCo-XML-Berichte, bildet die Differenz nur über `domain/`-Zeilen, ist
   ein Gate (Abschnitt 7.4 verlangt "von Anfang an automatisiert", nicht nur
   erhoben). Aktueller Bestand: 0 Lücken. Durch eine Gegenprobe verifiziert
   (unit/port testweise auf nur `arch` verengt: 324 nur-äußerlich gedeckte
   Zeilen gemeldet, danach zurückgesetzt) — die Prüfung erkennt eine echte
   Verletzung, nicht nur den Idealfall.
3. **PIT** auf die `HIGH`-Klassen, Schranke 99 %, als Testmenge nur die
   Tags `unit` und `port`. Dazu der Test, der die Menge der
   `HIGH`-annotierten Klassen gegen die PIT-Konfiguration abgleicht.
   **Erledigt.** Score nach mehreren Runden: 41 von 41 Mutanten getötet
   (100 %). Der ArchUnit-Test dafür (`ArchitectureTest`,
   `highKritikalitaetsKlassenStimmenMitDerPitKonfigurationUeberein`) auch
   durch eine Gegenprobe verifiziert (Erwartungsmenge testweise verfälscht,
   Test schlug fehl, danach zurückgesetzt).

   **Ein echter, gravierender Fund dabei:** `archunit-junit5-engine:1.4.1`
   implementiert `getTags()` auf keinem seiner `TestDescriptor`-Knoten.
   Jeder JUnit-Platform-`TagFilter` — unabhängig von den konkreten Tags,
   unabhängig davon, ob per Paket oder per Klasse ausgewählt wird — sortiert
   dadurch ausnahmslos alle ArchUnit-Tests aus, ohne jede Fehlermeldung
   (verifiziert per Bytecode-Inspektion der Engine-Jar und einem
   eigenständigen `LauncherDiscoveryRequest` außerhalb von Gradle). Der
   `test`-Task filterte seit Phase 1 auf `includeTags("unit", "port",
   "arch")` — `ArchitectureTest` trug `@Tag("arch")` und lief dadurch bei
   **keinem einzigen** `test`/`check`-Lauf dieser gesamten Umsetzung
   tatsächlich mit, obwohl jede Verifikation seither `BUILD SUCCESSFUL`
   zeigte. Ein durch Tag-Filterung leeres Ergebnis sieht identisch aus wie
   ein bestandener Lauf — nur der neue PIT-Guard-Test (der bewusst mit einer
   Gegenprobe verifiziert wurde) deckte das auf, weil dieselbe Gegenprobe für
   ihn ergebnislos blieb, bis der Grund klar war. Fix: Struktur (`arch`)
   läuft seither in einem eigenen Task `archTest`, ausgewählt über
   `includeEngines("archunit")` statt über einen Tag; `check` hängt ihn
   jetzt zusätzlich ein. `@Tag("arch")` bleibt als Dokumentation auf
   `ArchitectureTest`/`TeststrategyArchitectureTest` stehen (ADR-030,
   Nachtrag Phase 4), ist für die Task-Auswahl aber wirkungslos.
4. **Ausnahmenregister** anlegen (`docs/test-ausnahmen.md`): äquivalente
   Mutanten und bewusst nicht abgedeckte Fälle, jeweils mit Begründung und
   Datum. Ausschluss über `excludedAnnotations`, damit die Unterdrückung im
   Code sichtbar ist. **Erledigt** — PIT selbst kennt keine
   `excludedAnnotations`-Eigenschaft (weder im Gradle-Plugin noch im
   Kern-CLI), wohl aber ein eingebautes, standardmäßig aktives
   Annotationsfilter-Plugin ("FANN"), das über `pitest.features`
   angesprochen wird. Neue Annotation
   `de.fourteen.watchparty.mutationtest.AequivalenterMutant` (eigenes
   kleines Markerpaket, analog zu `criticality`), in `build.gradle.kts` per
   `features.set(listOf("+FANN(annotation[Generated]annotation[DoNotMutate]
   annotation[CoverageIgnore]annotation[AequivalenterMutant])"))` ergänzt.
   Dabei ein Stolperstein: PITs Feature-Grammatik trennt mehrere Werte
   *desselben* Parameters nicht durch Kommas (jedes Komma wird als Trenner
   zwischen ganzen Features gelesen, mit dem Fehler "X should start with +
   or -"), sondern durch wiederholte `schluessel[wert]`-Klauseln ohne
   Trennzeichen dazwischen. Mit einer echten, danach wieder entfernten
   Test-Annotation auf `Settlement.requireShare` verifiziert (Mutantenzahl
   sank von 41 auf 40, stieg nach dem Entfernen wieder auf 41). Aktueller
   Registerstand: keine Ausnahme eingetragen, 41 von 41 Mutanten stehen bei
   100 %.
5. **Laufzeit messen.** Budget sind 10 Minuten. Wird es eng, wird zuerst die
   Häufigkeit der Disjunktheitsprüfung reduziert (nur auf `master`), danach
   ihr Umfang — nicht stillschweigend die Prüfung selbst. **Erledigt** —
   `./gradlew clean check pitest -PskipFrontend` von Grund auf (kein
   Gradle-Daemon, kein Cache-Treffer): 59 Sekunden, weit unter dem Budget.
   Deutlich unter 10 Minuten selbst mit großem Sicherheitsabstand für einen
   langsameren CI-Runner (kälterer Dependency-Cache, schwächere CPU) — keine
   Gegensteuerung nötig, die in Abschnitt 7.4 vorgesehene
   Häufigkeits-/Umfangsreduktion bleibt für später, falls sich das mit
   wachsendem Testbestand ändert.

**Fertig, wenn** ein absichtlich überlebender Mutant in `Settlement` die
Pipeline rot macht.

---

## Phase 5 — Prozess verankern

1. `docs/features/_vorlage.md` nach der Vorlage aus Abschnitt 9.1 anlegen.
2. **Beobachtungsbogen verknüpfen:** `probelauf.md` führt künftig genau die
   `beobachtung`-markierten IDs (heute 3.1-b und 5-f). Sonst driften die
   beiden Listen auseinander.
3. **CLAUDE.md ergänzen:** Verweis auf `teststrategie.md`, die TDD-Regel für
   neue Features, und die Regel, dass ein neuer Domänentyp sein Szenario
   sofort bekommt — analog zum jMolecules-Stereotyp.
4. ADRs nachziehen, dieses Dokument löschen.

---

## Risiken auf einen Blick

| Risiko | Gegenmittel |
|---|---|
| Umfang: 60 Regeln sind viel | Reihenfolge nach Kritikalität; jeder Teilschritt für sich wertvoll |
| Rotes Gate ab Tag eins gewöhnt an Rot | Abdeckung startet als Bericht, wird am Ende von Phase 3 zum Gate — terminiert, nicht optional |
| Characterization-Falle: Tests zementieren Fehler | Szenarien zuerst aus Anhang A, dann Zuordnung zum Bestand; Lücken zum Fachexperten |
| Laufzeit sprengt die 10 Minuten | ab Phase 4 messen; Gegensteuerung ist benannt |
| Java-Version driftet zwischen Runner, Container und Toolchain | Java 25 ist seit ADR-029 durchgehend gesetzt; die Pipeline setzt die Version ausdrücklich (Phase 0) |
| Sporadisch roter `RestoreTest` verdeckt echte Fehlschläge | in Phase 3.3 auflösen; bis dahin nicht durch Wiederholung übertünchen |

## Was der Plan bewusst nicht enthält

Keine Frontend-Tests und keine Prüfung der Verträglichkeit zwischen
Frontend und Frame-Format. Beides ist in `teststrategie.md`, Abschnitt 11,
als außerhalb benannt — es fällt hier nur deshalb nicht hinten runter, weil
es dort ausdrücklich steht.
