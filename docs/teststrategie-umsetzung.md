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
unbelegten `backend`-Regeln ausgibt. Startwert: 60.

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
(`new Random(42)`) wird dabei abgelöst.

**3.2 Port, Kritikalität HIGH.** Verdeckte Tipps: 6-b und 9-b. Hier
entstehen die **Leck-Tests auf Nachrichtenebene** und die
**Invariantenprüfung nach jedem Schritt** (kein Konto negativ, Punktesumme
erhalten, Deltasumme null beim Auflösen).

**3.3 Port, Kritikalität MEDIUM.** Rundenablauf (9-a bis 9-c, 5-a bis 5-d),
Strafen und Teilnehmerkreis (8.1 bis 8.1-e), Annullieren (8.6 ff.),
Zurücksetzen (8.7 ff.), Wettmechanik (6-a, 6-c, 6-e, 6-f), Rollen (10-a,
10-b), Host-Rolle (10.1 ff.), Reconnect und Wiederanlauf (1-c, 1-d).
Die Zeit- und Reihenfolge-Szenarien gehören hierher, allen voran die
Rundenwache aus ADR-010.

In diesem Schritt wird **`getRoomForTest()` entfernt** und die vorhandenen
`RoomActorStateMachineTest`, `ReconnectTest`, `RestoreTest` und
`RoomActorResetTest` werden auf Prüfung über die Ports umgebaut.

**3.4 Adapter.** Fixtures werden am Port-Datentyp konstruiert, nicht durch
die Domäne erzeugt. Snapshot-Round-Trip über einen **vollständig gefüllten**
Raum; jeder Nachrichtentyp serialisiert; kaputte und unvollständige Frames
töten weder Verbindung noch Raum-Thread.

**3.5 API.** Ein durchgängiger Rundenablauf, der **Leck-Test am
serialisierten JSON** (der bestehende `WireProtocolSmokeTest` ist die
Grundlage), die Vollständigkeit des Zustands nach Reconnect und die
nicht-funktionale Prüfung: Ein Client, der nicht mehr liest, hält die
anderen nicht auf.

**Fertig, wenn** `./gradlew abdeckung` null offene `backend`-Regeln meldet.
**Unmittelbar danach**: den Bericht aus Phase 2 zum Gate machen.

**Risiko:** der Umfang. Gegenmittel ist die Reihenfolge — nach jedem
Teilschritt ist ein sinnvoller, committbarer Zustand erreicht, und die
kritischsten Regeln sind zuerst belegt. Wer hier abbricht, hat trotzdem das
Wertvollste.

---

## Phase 4 — Metriken scharf stellen

1. **JaCoCo** je Ebene erheben und als Artefakt ablegen — ohne
   Prozentschranke.
2. **Die Ebenen-Disjunktheit** aus 7.4: Differenz der abgedeckten
   Domänenzeilen zwischen den Ebenen. Deckt ein Adapter- oder API-Test eine
   Domänenzeile ab, die keine innere Ebene abdeckt, ist das eine Lücke
   weiter innen.
3. **PIT** auf die `HIGH`-Klassen, Schranke 99 %, als Testmenge nur die
   Tags `unit` und `port`. Dazu der Test, der die Menge der
   `HIGH`-annotierten Klassen gegen die PIT-Konfiguration abgleicht.
4. **Ausnahmenregister** anlegen (`docs/test-ausnahmen.md`): äquivalente
   Mutanten und bewusst nicht abgedeckte Fälle, jeweils mit Begründung und
   Datum. Ausschluss über `excludedAnnotations`, damit die Unterdrückung im
   Code sichtbar ist.
5. **Laufzeit messen.** Budget sind 10 Minuten. Wird es eng, wird zuerst die
   Häufigkeit der Disjunktheitsprüfung reduziert (nur auf `master`), danach
   ihr Umfang — nicht stillschweigend die Prüfung selbst.

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
