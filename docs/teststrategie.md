# Teststrategie

Dieses Dokument sagt, **was auf welcher Ebene geprüft wird, wie streng, und
woran man merkt, dass es nicht geschehen ist**. Es steht neben den anderen
drei Dokumenten, ersetzt aber keines davon:

- `anforderungen.md` sagt, *was* gilt. Sie ist die Referenz für die
  Feature-Abdeckung — jedes Szenario zeigt auf eine Nummer daraus.
- `adrs.md` sagt, *warum* der Bau so aussieht. Diese Strategie wird selbst
  zu einem ADR, sobald sie umgesetzt ist.
- `offene-entscheidungen.md` sammelt, was noch nicht entschieden ist. Auch
  fachliche Lücken, die beim Nachrüsten der Tests auffallen, landen dort,
  bis der Fachexperte sie geklärt hat.
- `probelauf.md` sammelt, was sich am Schreibtisch grundsätzlich nicht
  prüfen lässt. Abschnitt 5.4 sagt, wie das mit dieser Strategie
  zusammenhängt.

## 1. Grundsatz

**Jede Ebene testet nur, was die Ebene darunter nicht kann.** Das ist die
eine Regel, aus der alles Übrige folgt. Eine Anforderung wird genau einmal
fachlich geprüft — auf der innersten Ebene, die sie überhaupt entscheiden
kann. Höhere Ebenen prüfen dann nur noch, dass die Übersetzung stimmt und
die Teile zusammenpassen.

Die Regel ist kein Stilwunsch. Sie ist der Grund, warum die Testmenge nicht
mit jeder Ebene multipliziert wird, und sie ist messbar (Abschnitt 7.4).

| Ebene | Frage | Werkzeug | JUnit-Tag |
|---|---|---|---|
| Domäne | Stimmt die Regel? | JGiven + Property-Tests | `unit` |
| Port-to-Port | Stimmt der Ablauf? | JGiven über `RoomCommands` | `port` |
| Adapter | Kann der Adapter alles übertragen, was der Port ausdrückt? | JGiven am Port-Datentyp | `adapter` |
| API | Passen die Teile zusammen, hält das System die NFR? | JGiven über echten Socket | `api` |
| Struktur | Hält der Bau die Invarianten? | ArchUnit | `arch` |

Getrennt wird über JUnit-Tags, nicht über eigene Source Sets: Die
handgeschriebenen Test Doubles (`FakeClock`, `FakeScheduler`,
`RecordingClientGateway`, ADR-025) sollen auf allen Ebenen dieselben sein.
Meta-Annotationen bündeln den JUnit-Tag und den JGiven-Report-Tag, damit
beides nicht auseinanderlaufen kann:

```java
@Tag("port")
@IsTag(name = "Port-to-Port")
public @interface PortTest { }
```

## 2. Die Ebenen im Einzelnen

### 2.1 Domäne (`unit`)

Hier liegt jede Regel, die **aus dem Zustand eines Objekts oder aus einer
reinen Funktion entscheidbar** ist:

- Invarianten der Value Objects: `Points` nie negativ, `PlayerName` 1 bis
  20 Zeichen, `Share`/`PointsDelta` als eigene Typen.
- Regeln einzelner Entities: `Player.stakeFor` (Anforderung 6, 8.3), der
  Verpasste-Runden-Zähler (8.1), die benannten Übergänge auf `Round`.
- **Domain Services isoliert**: `Settlement` ist eine reine Funktion und
  wird direkt geprüft — Anteilsformel (7.1), Größte-Reste-Verfahren (7.2),
  Push (8.2), Null-Punkte-Spieler (8.3), alle tippen richtig (8.5). Es wäre
  Verschwendung, dafür fünf Spieler durch die Ports zu treiben.

JGiven wird auch hier eingesetzt, aber mit einem anderen Zweck als weiter
außen: **Die Domänen-Szenarien bauen im Report das Vokabular auf.** Ein
Leser, der `anforderungen.md` kennt, aber keinen Code, findet dort je einen
Abschnitt zu Punkten, Anteilen, Einsatz, Strafe, Tipp — der Bausteinteil des
Reports ist das ausführbare Glossar der Ubiquitous Language (ADR-025).
Dafür trägt jedes Domänen-Szenario den Tag `@Baustein("Points")`.

Property-Tests (Abschnitt 4) hängen an derselben Ebene, erscheinen aber
nicht als Szenario im Report — sie prüfen Allaussagen, keine Beispiele.

Testdaten werden ausschließlich über legale Übergänge aufgebaut. Die
Mutatoren sind seit ADR-025 paket-privat; ein Testaufbau, der einen Zustand
nur über einen Setter erreichen könnte, beschreibt einen Zustand, den es im
Betrieb nicht gibt.

### 2.2 Port-to-Port (`port`)

Hier liegt jede Regel, die **über Zeit, Phasen oder mehrere Beteiligte
spannt**. Eingang ist `RoomCommands`, Ausgang der `RecordingClientGateway`;
`FakeClock` und `FakeScheduler` machen die Zeit steuerbar.

Der Eingangs-Port ist bewusst asymmetrisch — jede Methode reiht nur ein und
liefert nichts zurück (siehe Javadoc an `RoomCommands`). Ein Szenario ist
deshalb immer: Kommando einreihen, Leerlauf abwarten, an den ausgehenden
Nachrichten prüfen. **Der Griff auf `getRoomForTest()` entfällt**: Was durch
die Ports nicht sichtbar ist, ist auch fachlich nicht sichtbar.

Ein Sonderfall, der genau daran hängt: Während ein Wettfenster offen ist,
*darf* der einzelne Tipp nicht beobachtbar sein (Invariante 4). Die
Behauptung „der Tipp ist angekommen" wird deshalb erst nach dem Schließen
geprüft — zusammen mit der Behauptung, dass vorher nichts davon zu sehen
war. Die Verzögerung ist nicht Umständlichkeit, sie ist die Anforderung.

Hierher gehören unter anderem: Teilnehmerkreis beim Öffnen einfrieren
(8.1), Strafe nur für eingefrorene Nicht-Tipper, Pause ab der dritten
verpassten Runde, Annullieren (8.6), Zurücksetzen (8.7), Reconnect
(ADR-014), Wiederanlauf aus dem Snapshot (ADR-023), und die Trennung von
Aufdecken beim Schließen und Verrechnen beim Auflösen (Anforderung 9).

**Zeit und Reihenfolge sind hier eine eigene Kategorie von Szenarien**, kein
eigenes Verfahren. Weil der `FakeScheduler` die Verschränkung deterministisch
macht, lassen sich die Rennen als gewöhnliches Angenommen-Wenn-Dann
schreiben:

> *Angenommen* ein Wettfenster ist offen,
> *wenn* der Host von Hand schließt und danach der Auto-Close-Timer der
> beendeten Runde feuert,
> *dann* bleibt die Runde geschlossen und es wird nicht doppelt abgerechnet.

Das ist ADR-010 (Rundenwache) in der Sprache der Fachabteilung. Was hier
*nicht* geprüft wird, sind echte Thread-Rennen: Die sind durch Invariante 1
strukturell ausgeschlossen und werden von ArchUnit gehütet (Abschnitt 2.5),
nicht von Szenarien.

**Nach jedem Szenarioschritt läuft die Invariantenprüfung** aus Abschnitt 3.

### 2.3 Adapter (`adapter`)

Die Frage dieser Ebene ist **nicht** „erfüllt der Adapter die fachlichen
Anforderungen" — das hat die Ebene darunter schon entschieden. Sie lautet:
**Kann der Adapter alles übertragen, was der Port ausdrücken kann?**

- `ws` ein: Jeder Frametyp wird auf das richtige Kommando abgebildet; ein
  fehlendes Feld, ein kaputtes JSON, ein unbekannter Typ tötet weder die
  Verbindung noch den Raum-Thread.
- `ws` aus: Jeder Nachrichtentyp aus `Messages` serialisiert und kommt
  vollständig an; `ClientSession` hält die Ausgangs-Queue durch, wenn ein
  Client nicht mehr liest (Invariante 2).
- `file`: Round-Trip über einen **vollständig gefüllten** Raum. Der teuerste
  Fehler bei ADR-023 ist ein neues Feld in `Room`, das in `RoomSnapshot`
  fehlt — dabei fällt kein Szenario um, weil kein Szenario das Feld kennt.
  Vollständigkeit ist hier eine strukturelle Eigenschaft, kein Beispiel.
- `time`: Der Scheduler feuert, sagt ab und läuft nicht auf dem Raum-Thread.

Daraus folgt eine Bauvorschrift: **Adapter-Tests konstruieren ihren Fall am
Port-Datentyp**, sie erzeugen ihn nicht durch die Domäne. Sonst würden sie
Domänenabdeckung hinzufügen, die es auf dieser Ebene nicht geben soll
(Abschnitt 7.4). Die Abgrenzung nach oben ist Ihr eigener Satz: Der Adapter
muss nur beherrschen, was fachlich auftreten kann — er ist Unterstützung der
Domäne, nicht ihr Gegenstück.

### 2.4 API (`api`)

Echter Server, echter WebSocket, echtes JSON. Diese Ebene fügt **keine neue
fachliche Abdeckung** hinzu. Sie prüft dreierlei:

1. **Verdrahtung**: Die Spring-Beans aus `config` ergeben einen Raum, der
   einen kompletten Rundenablauf durchhält. Ein einziges durchgängiges
   Szenario reicht dafür.
2. **Leck-Tests** (Abschnitt 3) am tatsächlich übertragenen JSON.
3. **Nicht-funktionale Anforderungen**, soweit sie hier entscheidbar sind:
   Ein Client, der nicht mehr liest, darf die anderen nicht anhalten
   (Invariante 2); der Zustand nach Reconnect ist vollständig
   (Invariante 3); die Auto-Close-Zeit wird eingehalten.

### 2.5 Struktur (`arch`)

ArchUnit prüft, was kein Szenario sehen kann und was auch keine
Zeilenabdeckung findet: die Ringregel (ADR-024), die DDD-Bausteine
(ADR-027), die Framework-Freiheit des Kerns, `java.util.concurrent`-Verbot
in der Domäne (Invariante 1), keine öffentlichen Setter. Dazu kommen mit
dieser Strategie:

- Das Kritikalitäts-Paket enthält ausschließlich Annotationen und hängt von
  nichts ab (Abschnitt 6.2).
- Jede Anforderungs-ID in einer `@Anforderung`-Annotation existiert in
  Anhang A von `anforderungen.md` (Abschnitt 5).
- Jede JGiven-`Stage` liegt im dafür bestimmten Paket — das ist die Grenze
  der Sprachausnahme aus Abschnitt 8.
- Die als sehr kritisch annotierten Klassen sind genau die, die im
  Mutationslauf konfiguriert sind (Abschnitt 7.2).

## 3. Leck-Tests und die Invariantenprüfung

Beides sind **Allaussagen über jeden Ablauf**, keine Szenarien. Sie laufen
deshalb nicht als eigene Testfälle, sondern als Prüfung, die an jedes
Szenario angehängt ist.

### 3.1 Leck-Tests

Invariante 4 verlangt eine *Abwesenheit*: Solange ein Wettfenster offen ist,
darf der Server keinen einzelnen Tipp senden, nur den Zähler.

**Warum die Feature-Abdeckung das nicht findet.** Feature-Abdeckung ist über
die Anforderungsliste geschlossen — sie findet Anforderungen ohne Szenario.
Ein Leck entsteht in der Gegenrichtung, durch *Code ohne Anforderung*: ein
neues Feld in `Messages`, ein Getter, den Jackson mitnimmt. Anforderung 6
hat dann längst ihr grünes Szenario, die Abdeckung steht auf 100 %, und das
Leck ist trotzdem da. Mutationstests helfen ebenfalls nicht: Sie verändern
vorhandenen Code, sie fügen kein Feld hinzu.

Ein Leck-Test quantifiziert deshalb **über die Ausgabefläche, nicht über die
Anforderungen**: Jedes Feld jedes Frames, das in der offenen Phase
verschickt wird, muss auf einer Positivliste stehen. Was nicht auf der Liste
steht, ist ein Fehlschlag — auch wenn niemand weiß, was es ist. Ein neues
Feld erzwingt damit eine bewusste Entscheidung.

Leck-Tests gibt es auf **zwei** Ebenen, und beide werden gebraucht:

- **Port-to-Port**, gegen die Nachrichtenobjekte: findet den Fehler früh und
  billig; hier sitzt mit `RoomView` der eigentliche Türsteher.
- **API**, gegen das serialisierte JSON: findet, was erst durch Jackson
  entsteht — ein Feld, das das Nachrichtenobjekt selbst nicht offensichtlich
  preisgibt.

### 3.2 Invariantenprüfung nach jedem Port-to-Port-Schritt

Nach **jedem** Schritt eines Port-to-Port-Szenarios gilt:

1. **Kein Konto ist negativ** (Anforderung 3, Invariante 5).
2. **Die Punktesumme ist erhalten**: Die Summe aller Kontostände entspricht
   dem Startguthaben mal der Zahl der beigetretenen Spieler. Sie ändert sich
   ausschließlich beim Beitritt (+ Startguthaben) und beim Zurücksetzen
   (8.7) — durch keinen anderen Vorgang, insbesondere nicht durch Auflösen.
3. **Beim Auflösen ist die Summe der Deltas exakt null** und die Summe der
   Auszahlungen exakt der Pool aus Einsätzen plus *tatsächlich
   eingesammelten* Strafen (8.1).

Damit wird jedes Szenario nebenbei zum Invariantentest — die
Nullsummen-Eigenschaft aus Anforderung 2 wird nicht an ein paar Beispielen
geprüft, sondern an allem, was je durchgespielt wird.

## 4. Property-Based Testing

Beispielbasierte Szenarien treffen eine Allaussage prinzipiell nicht. Die
härtesten Anforderungen dieses Projekts sind aber Allaussagen. Property-Tests
(jqwik) prüfen sie über erzeugte Wettbilder:

- Für jede Konstellation aus Einsätzen, Tipps und Nicht-Tippern gilt:
  Summe der Auszahlungen = Pool, kein Konto negativ, alles ganzzahlig.
- Der Anteil ist immer `max(Einsatz, Mindesteinsatz)` (7.1).
- Das Größte-Reste-Verfahren verteilt genau den Rest, nie mehr, nie weniger
  (7.2), und die Zuteilung hängt nicht von der Eingabereihenfolge ab.
- Die gekappte Strafe (8.1) sammelt nie mehr ein, als vorhanden war.

Nebeneffekt, der zum Mutationsziel passt: Ein Property-Test tötet ganze
Mutantenklassen, an denen ein Beispieltest vorbeiläuft. 99 % Mutation Score
auf `Settlement` ist mit Beispielen allein teuer und mit Properties nahezu
geschenkt. Der bereits vorhandene handgeschriebene Zufallstest in
`SettlementTest` (`new Random(42)`) wird dabei abgelöst.

## 5. Rückverfolgbarkeit und Feature-Abdeckung

### 5.1 Der Tag

Jedes Szenario trägt `@Anforderung("8.1-c")` — einen JGiven-`@IsTag`, der im
Report zur Gliederung wird und zugleich die Feature-Abdeckung trägt. Ein
Szenario darf mehrere Regeln belegen, eine Regel mehrere Szenarien haben.
Das ist kein Schlupfloch, sondern gewollt: Abschnitt 2 der Anforderungen
nennt das Grundprinzip, das Abschnitt 7 konkretisiert — ein Szenario zur
Anteilsformel belegt beide, ohne doppelt geschrieben zu werden.

### 5.2 Die Messung

Bezugsliste ist **Anhang A von `anforderungen.md`**: die Zerlegung des
Fließtexts in einzeln prüfbare Regeln, jede mit ID und Marke. Ein Test liest
den Anhang und die Tags aus dem Testbestand und bildet die Differenz:

- Eine ID in einem Tag, die es in Anhang A nicht gibt → Fehler.
- Eine Regel mit der Marke `backend` ohne grünes Szenario → Fehler.

Damit ist „100 % Feature-Abdeckung" eine Zahl aus dem Build und keine
Selbsteinschätzung.

Die Liste liegt in derselben Datei wie die Anforderungen selbst. Eine
zweite Datei wäre eine zweite Wahrheit, die still veraltet.

### 5.3 Warum es die Zerlegung braucht

Zwei Gründe, beide für die Messung erledigend.

**Erstens ist die Nummerierung im Fließtext zu grob.** Nummern gibt es nur
bei 3.1, 4.1–4.4, 7.1–7.2 und 8.1–8.7. Abschnitt 6 trägt unter einer
einzigen Nummer fünf eigenständige Regeln. „Anforderung 6 ist abgedeckt"
hätte also bedeutet: *irgendeine* der fünf hat ein Szenario — vier könnten
ungetestet sein, und die Abdeckung stünde auf 100 %. Genau die Zahl, die wir
nicht wollen. Anhang A vergibt deshalb Buchstaben-Suffixe (`6-a` … `6-f`)
für alles, was im Text keine eigene Überschrift hat; die bestehenden Nummern
bleiben unangetastet, weil CLAUDE.md, die ADRs und `probelauf.md` auf sie
verweisen.

**Zweitens ist nicht jede Regel im Backend prüfbar.** Der Host kann per
Programm nicht dazu gebracht werden, das Fenster vor dem Snap zu öffnen
(5-e); ob die Eimer einer Wette die Wirklichkeit lückenlos abdecken, weiß
kein Test (4-d); der unscheinbare Zurücksetzen-Knopf ist Oberfläche (8.7-b).
Ohne Marken stünde die Abdeckung dauerhaft unter 100 % und das Ziel wäre
wertlos.

Die vier Marken sind in Anhang A definiert. Wichtig ist die Regel dahinter:
**Jede Regel trägt genau eine Marke.** Wo eine Aussage im Text zwei Seiten
hat, wird sie in zwei Regeln zerlegt — der Server liefert die Anmerkungen zu
den Ausgängen (4-e, `backend`), die Oberfläche zeigt sie (4-f, `frontend`).
Früher hätte das eine „teilweise geprüfte" Anforderung ergeben. Die
Unschärfe gehört in die Anforderung aufgelöst, nicht in die Testmarke.

### 5.4 Regressionen aus dem Probelauf

Jeder Fehler, der am Spielabend auffällt, wird zu einem Szenario mit der
Anforderungs-ID, gegen die er verstoßen hat. Findet sich keine passende ID,
ist das der eigentliche Fund: Die Anforderung fehlt und muss vom Fachexperten
ergänzt werden. Die Kritikalität eines solchen Features ist rückwirkend
belegt — es ist ja eingetreten — und wird entsprechend hochgestuft.

## 6. Kritikalität

### 6.1 Bestimmung

Kritikalität = **Eintrittswahrscheinlichkeit × Schadensausmaß**, bestimmt bei
der Anforderungsanalyse, nicht nachträglich.

Schadensausmaß ist in diesem Projekt definiert als **wie teuer die Korrektur
am Spielabend ist**. Das ist der passende Maßstab, weil es um kein Geld
geht, aber auch niemand am Tisch debuggt: Nach dem Auflösen gibt es keine
Rückabwicklung (8.6), und ein falsches Leaderboard fällt womöglich gar nicht
auf. Ein Fehler, der still das Ergebnis verfälscht, ist damit teurer als
einer, der laut abstürzt.

### 6.2 Als Annotation im Produktivcode

Die Einstufung steht dort, wo sie gilt:

```java
@Criticality(level = HIGH, requirements = {"7.1", "7.2", "8.2"})
public final class Settlement { … }
```

Anwendbar auf Typ **und** Methode — eine Klasse kann Features
unterschiedlicher Kritikalität bedienen, und die Einstufung gehört an das
Feature, nicht pauschal an die Datei.

Das Paket `de.fourteen.watchparty.criticality` enthält ausschließlich
Annotationen und hängt von nichts ab. Es folgt damit demselben Muster wie
JSpecify (ADR-026) und jMolecules (ADR-027): reine Marker ohne
Laufzeitverhalten, geprüft von außen. Die Ringregeln aus ADR-024 sind
unberührt — `onionArchitecture()` schränkt ein, *wer auf einen Ring
zugreifen darf*, und ein ringloses Annotationspaket ist kein Ring. Eine
zusätzliche ArchUnit-Regel hält fest, dass dort auch künftig nur
Annotationen liegen.

Die Annotation ist der **maschinenlesbare Griff**, nicht die Begründung. Das
*Warum* der Einstufung steht im Feature-Dokument (Abschnitt 9.1); ein Test
hält beides zusammen, indem er prüft, dass jede genannte Anforderungs-ID
existiert.

### 6.3 Was je Stufe gilt

| Stufe | Anspruch |
|---|---|
| unkritisch (`LOW`) | 100 % Feature-Abdeckung. Jede Anforderung hat ihr Szenario, mehr nicht. |
| mittelkritisch (`MEDIUM`) | zusätzlich: **keine Methode ohne Test**. Jede öffentliche Methode der beteiligten Klassen wird von mindestens einem Test erreicht. |
| sehr kritisch (`HIGH`) | zusätzlich: **Mutation Score ≥ 99 %** auf den beteiligten Klassen. |

### 6.4 Einstufung des Bestands

Für neue Features entsteht die Einstufung beim Feature-Request. Der
vorhandene Code braucht eine einmalige Bewertung; die folgende ist vom
Fachexperten bestätigt:

| Bereich | Stufe | Begründung |
|---|---|---|
| Abrechnung — `Settlement`, `Points`, `PointsDelta`, `Share` (7, 8.1–8.5) | `HIGH` | Schaden maximal: falsche Punkte fallen nicht auf, nach dem Auflösen ist nichts rückabwickelbar |
| Verdeckte Tipps — `RoomView` (6, Invariante 4) | `HIGH` | Ein Leck macht das Spiel sinnlos und ist in der Oberfläche unsichtbar; Eintrittswahrscheinlichkeit hoch, weil jedes neue Feld es auslösen kann |
| Rundenablauf — `Room`, `Round`, `RoomActor` (5, 8.6, 9) | `MEDIUM` | Fehler fallen sofort auf; der Host kann annullieren |
| Snapshot — `RoomSnapshot`, `SnapshotStore` (ADR-023) | `MEDIUM` | Schaden hoch, aber Eintrittswahrscheinlichkeit gering und ein Rückfallweg vorhanden (Neustart, `RESET`) |
| Beitritt, Host-Rolle, Reconnect (10, ADR-014, ADR-021, 8.7) | `MEDIUM` | am Tisch korrigierbar |
| Wettkatalog `Bets` (4) | `LOW` | statische Daten, Fehler sofort sichtbar |

## 7. Metriken und Schwellen

### 7.1 Feature-Abdeckung: 100 %, harte Schranke

Über alle `backend`-markierten Anforderungen (Abschnitt 5).

### 7.2 Mutation Score: ≥ 99 %, nur auf `HIGH`

Der Mutationslauf umfasst **ausschließlich** die als `HIGH` eingestuften
Klassen. Als Testmenge bekommt er nur die schnellen Ebenen (`unit`, `port`)
— kein Spring, kein Socket, kein Reportschreiben; sonst wird der Lauf
unbenutzbar, weil PIT für jeden Mutanten Tests wiederholt.

Die Grenze ist 99 % und nicht 100 %, weil äquivalente Mutanten unvermeidbar
sind — semantisch identische Änderungen, die kein Test töten kann. Wo ein
solcher Mutant auftritt, wird er **benannt und begründet** ausgeschlossen
(Abschnitt 10), nicht durch einen Test bekämpft, der nichts prüft.

Die Konfiguration kann von der Einstufung nicht abdriften, weil es keine
zweite Liste mehr gibt: `build.gradle.kts` leitet `pitest.targetClasses` per
Reflection aus den `@Criticality(HIGH)`-Annotationen selbst ab. Eine neu
eingestufte Klasse ist damit ab dem nächsten Lauf im Mutations-Scope, ohne
dass jemand daran denken muss. Läuft das Einsammeln ins Leere, bricht der
Build ab — eine leere Zielmenge wäre der gefährlichste Ausgang, weil PIT
dann durchliefe, nichts mutierte und Erfolg meldete.

### 7.3 Zeilen- und Zweigabdeckung: erhoben, kein Ziel

JaCoCo läuft bei jedem Pipeline-Build und der Bericht wird als Artefakt
abgelegt. Es gibt **keine Prozentschranke**: Als Zielgröße erzeugt Abdeckung
Tests, die für die Zahl geschrieben werden. Als Suchhilfe ist sie nützlich —
sie zeigt vergessene Fehlerzweige. Strukturelle Fehler findet nicht JaCoCo,
sondern ArchUnit (Abschnitt 2.5).

Die Stufe `MEDIUM` verlangt „keine Methode ohne Test". Das ist absichtlich
keine Prozentzahl, sondern eine Ja/Nein-Frage pro Methode.

### 7.4 Ebenen-Disjunktheit: messbar, nicht nur verabredet

Der Grundsatz aus Abschnitt 1 lässt sich nachrechnen, wenn die Abdeckung pro
Tag getrennt erhoben wird: **Deckt ein Adapter- oder API-Test eine
Domänenzeile ab, die kein Port-to-Port- und kein Domänentest abdeckt, ist das
eine Lücke weiter innen** — nicht ein Verdienst der äußeren Ebene.

Die Prüfung läuft **von Anfang an automatisiert**: getrennte JaCoCo-Läufe je
Tag, Differenzbildung über die Domänenzeilen. Der Preis dafür ist Laufzeit —
die Testmenge wird mehrfach ausgeführt. Läuft das Zeitbudget aus
Abschnitt 10 dadurch voll, wird gegengesteuert, statt die Prüfung
stillschweigend fallen zu lassen: zuerst über die Häufigkeit (nur auf
`master` statt bei jedem Lauf), danach über den Umfang.

## 8. Der Report

**Zielleser ist eine Fachabteilung**, die `anforderungen.md` kennt und
keinen Code liest. Daraus folgen drei Regeln:

1. **Sprache.** Der Reporttext ist deutsch — er ist Dokumentation, nicht
   Bezeichner. Die Stufenklassen und ihre Schritte werden deshalb deutsch
   benannt; der übrige Produktivcode bleibt englisch (Konvention aus
   CLAUDE.md). Das ist eine bewusste, eng begrenzte Ausnahme, die als ADR
   festgehalten wird.

   Die Ausnahme ist **strukturell eingehegt**, nicht dem Augenmaß
   überlassen: Stufenklassen leben ausschließlich in einem dafür bestimmten
   Paket, und ArchUnit prüft, dass jede JGiven-`Stage` dort liegt. „Deutsche
   Bezeichner" ist damit keine Ermessensfrage mehr, sondern eine Frage des
   Pakets. Die Begründung ist enger als „Ausnahme, weil wir sie brauchen":
   Eine JGiven-Stufe ist kein Bezeichner im Sinne der Konvention, sondern
   **Reporttext in Java-Syntax** — sie steht auf derselben Seite wie
   „Kommentare und Dokumentation deutsch".

   Verwendet wird der **deutsche Gherkin-Dialekt**: *Angenommen*, *Wenn* für die
   Handlung, *Dann* für die Erwartung, *Und* für Fortsetzungen. „Angenommen"
   passt, wo ein Umstand gesetzt wird („angenommen, das Wettfenster ist
   offen"). Eine projekteigene Basisklasse stellt die
   Einstiege unter diesen Namen bereit, damit der Testcode denselben Dialekt
   spricht wie der Report.
2. **Vokabular.** Ein Schritttext benutzt nur Begriffe aus
   `anforderungen.md`: Wette, Wettfenster, Runde, Tipp, Einsatz, Anteil,
   Pool, Strafe, Auflösen (ADR-022). Keine Klassennamen, keine
   Sitzungs-IDs, kein JSON. Das ist eine Reviewregel.
3. **Gliederung.** Der Report hat zwei Teile: die **Bausteine** (Ebene
   `unit`, das ausführbare Glossar) und die **Abläufe** (Ebenen `port` und
   `api`, nach Anforderungsnummer sortiert). Die Ebene `adapter` bleibt
   sichtbar, aber getrennt — sie erzählt keine Fachlichkeit.

## 9. Vorgehen

### 9.1 Neue Features: testgetrieben, Kriterien vorher

Vor der Implementierung entsteht ein Feature-Dokument unter
`docs/features/NNN-kurzname.md` nach fester Vorlage:

```
# NNN — Kurzname

## Anlass
Wozu, in zwei Sätzen.

## Betroffene Anforderungen
Nummern aus anforderungen.md, oder: neu (dann dort ergänzen).

## Akzeptanzkriterien
Nummeriert. Jedes eine prüfbare Aussage.

## Szenarien
Angenommen — Wenn — Dann, in Prosa und in der
Sprache der Anforderungen. Werden eins zu eins zu JGiven-Szenarien.

## Kritikalität
Eintrittswahrscheinlichkeit × Schadensausmaß, begründet.
Ergebnis: LOW | MEDIUM | HIGH.

## Umgesetzt in
Klassen, die die Einstufung tragen. Bindet die Metrik an den Code.

## Offene Fragen
Wandern nach offene-entscheidungen.md, wenn sie es bleiben.
```

Zwei Abgrenzungen, damit keine zweite Wahrheit entsteht:

- Das Feature-Dokument ist der **Antrag**, `anforderungen.md` bleibt der
  **geltende Stand**. Ändert ein Feature das Verhalten, wird
  `anforderungen.md` mitgezogen.
- Nach der Implementierung wird das Feature-Dokument **nicht weiter
  gepflegt**. Lebendes Dokument ist ab dann der JGiven-Report. Das Dokument
  bleibt als Beleg der Kritikalitätsbewertung stehen.

Technische Entscheidungen gehören weiterhin in `adrs.md`, nicht hierher.

### 9.2 Der Bestand: einmalige Nachrüstung

Das Projekt ist fachlich fertig; für den Bestand ist TDD nicht mehr möglich.
Die Nachrüstung ist Characterization Testing und hat ein Risiko: Szenarien,
die aus dem Code abgeleitet werden, schreiben dessen Fehler fest.

Deshalb gilt für diese einmalige Aktion: **Szenarien werden aus
`anforderungen.md` abgeleitet, nicht aus dem Code.** Ein roter Test ist
zuerst ein Fund, nicht ein Testfehler. Wo die Anforderung eine Frage offen
lässt, wird sie **nicht** aus dem Code beantwortet, sondern dem Fachexperten
vorgelegt und bis zur Klärung in `offene-entscheidungen.md` notiert.

Nach dieser Aktion gilt für alles Weitere Abschnitt 9.1.

## 10. Regeln für den Bau

- **Die Pipeline führt Tests aus.** Erfüllt: `release.yml` bindet
  `build.yml` als wiederverwendbaren Workflow ein, und der läuft `check`.
  Ein roter Test verhindert damit Release *und* Deploy. Seit 2026-08-20
  hängt `check` auch an `pitest` — der Schwellwert von 99 % galt vorher nur
  für den, der die Mutationstests von Hand aufrief, und war in keinem
  CI-Lauf wirksam.
- **Der Bau prüft mehr als Tests.** An `check` hängen außerdem die Gates,
  die Verabredungen zu Regeln machen: `abdeckung` (Feature-Abdeckung),
  `ebenenDisjunktheit`, `ausnahmenregister` (Abschnitt 10),
  `protokollvertrag` (Abschnitt 11) und `aufbaudoku` (CLAUDE.md gegen den
  Baum). Alle brechen den Build ab, statt nur zu berichten.
- **Zeitbudget: 10 Minuten** für den vollständigen Lauf einschließlich
  Mutationstests. Wird es enger, wird zuerst der Mutations-Scope
  hinterfragt, nicht die Feature-Abdeckung.
- **Kein `Thread.sleep` in Tests.** Auf Zustand wird mit Zeitgrenze
  gewartet. Ein sporadisch fehlschlagender Test ist ein Fehlschlag, kein
  Wiederholungsfall — Wiederholungen werden nicht eingebaut.
- **Ausnahmenregister.** Äquivalente Mutanten, nicht prüfbare Anforderungen
  und bewusst nicht abgedeckte Fälle stehen an einer Stelle, jeweils mit
  Begründung und Datum. Eine Unterdrückung ohne Eintrag ist ein Fehler —
  seit 2026-08-20 auch maschinell: Der Task `ausnahmenregister` gleicht
  `@AequivalenterMutant` und `@Disabled` mit `docs/test-ausnahmen.md` ab,
  in beide Richtungen.

## 11. Was diese Strategie nicht abdeckt

- **Das Frontend.** `useRoom.js`, `App.jsx` und `Guide.jsx` sind außerhalb.
  Das betrifft echte Anforderungen — dass die Anmerkungen zu den Ausgängen
  sichtbar sind (4), dass das Leaderboard die Kontostände zeigt (3). Sie
  tragen die Marke `frontend` und zählen nicht zur Feature-Abdeckung.
- **Das Verhalten des Frontends am Protokoll.** Die *Namen* gleicht der
  Gradle-Task `protokollvertrag` seit 2026-08-20 ab: Frame-Typen, Phasen,
  Annullierungsgründe und die Feldnamen der Nachrichtentypen müssen auf
  beiden Seiten dieselben sein, sonst bricht der Build. Was er nicht sieht,
  ist die *Bedeutung* — ob das Frontend ein korrekt benanntes Feld auch
  richtig interpretiert, bleibt außerhalb. Geprüft wird nur die
  Live-Wetten-App; das Tippspiel spricht REST mit eigenem Vertrag
  (ADR-039).
- **Was nur am Spielabend sichtbar wird.** Tab-Suspend auf dem Handy, Wake
  Lock, das Verhalten des Fly-Volumes, die Kalibrierung der drei Parameter
  aus 3.1. Dafür ist `probelauf.md` da. Diese Punkte gelten als *beobachtet*,
  nicht als *getestet*, und dürfen nicht durch eine grüne Abdeckung als
  erledigt erscheinen.

## 12. Stand

Die Strategie ist vollständig festgelegt; die drei zuletzt offenen Punkte
sind entschieden:

- **Klassifikation der Anforderungen** — erledigt als Anhang A von
  `anforderungen.md`, mit atomaren Regeln und einer Marke je Regel.
- **Kritikalitätseinstufung des Bestands** — bestätigt (Abschnitt 6.4).
- **Sprachausnahme für die JGiven-Stufen** — entschieden (Abschnitt 8),
  strukturell durch ArchUnit eingehegt; als ADR festzuhalten.

Die Umsetzung ist inzwischen abgeschlossen (fünf Phasen, zuletzt
ADR-030/ADR-031) — der dafür verwendete Arbeitsplan
(`teststrategie-umsetzung.md`) war ausdrücklich vorübergehend und ist mit
Abschluss der letzten Phase gelöscht.

Ein Beleg dafür, dass das Verfahren aus Abschnitt 9.2 trägt: Beim Zerlegen
der Anforderungen fiel auf, dass die Host-Rolle nur in ADR-016 und ADR-021
stand — Abschnitt 10 sagte, *was* der Host darf, nicht *wer* es wird. Die
Lücke wurde nicht aus dem Code beantwortet, sondern dem Fachexperten
vorgelegt und als Anforderung 10.1 nachgetragen. Genau so ist es gemeint.
