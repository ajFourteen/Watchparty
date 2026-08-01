# Weg zum MVP

**Status:** Etappen 1–5 sind umgesetzt, ein Spielabend ist durchspielbar.
Offen ist Etappe 6 (Parameter-Kalibrierung am echten Abend, siehe
`offene-entscheidungen.md`).

Vom Walking Skeleton zur ersten spielbaren Fassung. Fachliche Grundlage ist
`anforderungen.md`, technische `adrs.md`. Die hier getroffenen Entscheidungen
sind aus `offene-entscheidungen.md` heraus entschieden und müssen dort und in
den jeweiligen Quelldokumenten noch nachgezogen werden.

## Zieldefinition

Ein Spielabend ist durchgehend spielbar: Der Host öffnet den Markt „Ausgang
des nächsten Drives", alle tippen verdeckt, das Fenster schließt nach 15
Sekunden oder per Notbremse, die Tipps werden aufgedeckt, der Host löst auf,
Punkte werden nullsummen-korrekt verrechnet, das Leaderboard steht. Reconnect
funktioniert in jeder Phase.

Nicht im MVP: weitere Markttypen (ADR-017 hält den Weg offen, mehr braucht es
heute nicht), Persistenz, Wake Lock, Remote-Play.

## Getroffene Entscheidungen

Diese fünf Punkte waren offen und blockierten die Abrechnung bzw. den
Zustandsautomaten.

**Parameter: Startguthaben 1000, Mindesteinsatz 25, Strafe 25.** Das sind 40
Mindesteinsätze Puffer bei etwa 25 Drives pro Abend — echter Bankrott ist
damit unwahrscheinlich, wie Abschnitt 12 der Anforderungen es verlangt. Strafe
gleich Mindesteinsatz sorgt dafür, dass Aussitzen strikt dominiert ist:
gleicher Preis wie ein Mindest-Tipp, aber ohne Gewinnchance. Ein Einsatz von
100 bis 200 ist eine sichtbare Ansage. Alle drei Werte sind am realen
Spielgefühl zu justieren.

**Die Strafe wird auf den Kontostand gekappt.** Eingesammelt wird
`min(Strafe, Punkte)`; der Pool besteht aus dem, was tatsächlich eingesammelt
wurde. Damit bleibt die Nullsumme exakt erhalten (Invariante 5) und Konten
werden nie negativ. Ein Spieler bei 0 Punkten zahlt faktisch nichts mehr, was
zu 8.3 passt: Die Null ist kein absorbierender Zustand, und die Strafe darf
sie nicht doch zu einem machen.

**Der Teilnehmerkreis wird beim Öffnen eingefroren.** Wer während des offenen
Fensters dazukommt, darf tippen und gewinnen, wird aber nicht bestraft.
Niemand zahlt für eine Runde, die schon lief, als er kam.

**Ein getrennter Spieler pausiert ab der dritten verpassten Runde.** Er zahlt
für die erste und zweite verpasste Runde die Strafe, danach fällt er aus dem
Teilnehmerkreis und zahlt nicht mehr. Bei Reconnect ist er sofort wieder
dabei, der Zähler beginnt von vorn. Das deckt 8.1 ab — das eingeschlafene
Handy zahlt, Wegdösen ist nicht die günstigste Strategie — verhindert aber,
dass jemand, der früh nach Hause geht, über zwanzig Runden ausblutet und das
Leaderboard verzerrt. Die Pause greift ausdrücklich nur bei getrennter
Verbindung: Wer verbunden ist und nicht tippt, zahlt weiter jede Runde.

**Beim Push werden die Strafen nach den Anteilen aus 7.1 verteilt**, also
`Anteil = max(Einsatz, Mindesteinsatz)`, Rest nach dem Größte-Reste-Verfahren.
Eine Anteilsdefinition für beide Fälle statt zweier, und der All-in-Spieler
mit 0 Punkten bekommt auch beim Push etwas ab.

**Die Host-Rolle klebt am Token, die Rückgabe erfolgt zwischen den Runden.**
Ausformuliert als Rangfolge statt als Sonderfall: **Host ist immer der am
frühesten beigetretene verbundene Spieler.** Damit gilt „erster Joiner wird
Host" aus ADR-016 nicht nur einmal beim Start, sondern dauerhaft, und der
Fall, dass auch der Vertreter sein Handy sperrt oder zurückkehrt, während der
ursprüngliche Host noch weg ist, ist mit abgedeckt, ohne dass es ein
gesondertes „Original-Host-Token" bräuchte.

Dabei gilt eine Asymmetrie:

- **Verlieren wirkt sofort, in jeder Phase.** Sonst wäre der Raum mitten im
  offenen Fenster steuerlos — genau dann, wenn jemand schließen können muss.
- **Zurückholen wirkt erst in IDLE oder RESOLVED.** Kehrt ein
  höherrangiger Spieler während OPEN oder CLOSED zurück, wird die Übergabe
  vorgemerkt und beim Erreichen von RESOLVED ausgeführt. Sonst rutschen dem
  Vertreter die Steuerknöpfe mitten in einer laufenden Runde weg.

`Room.reassignHostIfNeeded()` sucht bereits den ersten verbundenen Spieler in
Einfügereihenfolge; es ändert sich im Wesentlichen nur, wann die Methode
läuft — künftig bei jeder Änderung der Verbundenheit statt nur beim Wegfall
des aktuellen Hosts, mit der Phasensperre für den Aufwärts-Fall. Ein
pausierter Spieler ist per Definition getrennt und damit ohnehin nicht
wählbar.

Nebeneffekt: Die Rolle kann über den Abend zwischen den Runden mehrfach
wandern, wenn Handys ein- und aufwachen. Sie landet dabei immer bei dem, der
am längsten dabei ist — in der Praxis der, der die Fernbedienung hat. Das
notierte Wake-Lock würde das zusätzlich beruhigen.

## Etappe 1 — Fundament

Ohne sichtbare Änderung, aber Voraussetzung für alles Weitere.

Es gibt bislang kein `src/test/`. Die zwei Fallen, die als bekannt notiert
sind — ein Auto-Close-Timer aus einer beendeten Runde und die Frage, ob ein
Tipp noch zählt — sind genau die Stellen, die man ohne Kontrolle über die Zeit
nicht deterministisch prüfen kann. Deshalb zuerst:

- `Clock` und ein schmales `Scheduler`-Interface per Konstruktor in
  `RoomActor`. In Produktion `Clock.systemUTC()` und ein
  `ScheduledExecutorService`, im Test eine Fake-Uhr und ein Scheduler, der
  Tasks nur sammelt und auf Kommando feuert. Damit wird „Auto-Close feuert
  verspätet, ein Tipp kommt dazwischen" ein gewöhnlicher Testfall.
- Ein paket-privater Testzugang zum Actor, der auf das Leerlaufen der Queue
  wartet. Ohne ihn sind alle Actor-Tests Race-behaftet.
- Der Marktkatalog als Daten, nicht als Sonderfall (ADR-017):
  `Market(id, question, List<Outcome>)`, dazu eine Konstante mit den sieben
  Drive-Ausgängen. Die Anmerkungen aus 4.1 — etwa dass der verschossene Field
  Goal unter „Turnover on Downs" fällt — gehören mit in die Struktur und damit
  in die Nachricht. Die Anforderung verlangt, dass sie in der Oberfläche
  sichtbar sind; sie im Frontend zu duplizieren wäre die Stelle, an der beide
  Seiten später auseinanderlaufen.

## Etappe 2 — Abrechnung als reine Funktion

Eine eigene Klasse `Settlement`, ohne jeden Bezug zu `Room`, `Player` oder
`RoomActor`:

```
Map<String,Integer> settle(List<Bet> bets, Set<String> nonBettors,
                           Map<String,Integer> balances,
                           String winningOutcome, Params params)
```

Sie gibt Deltas zurück und hat keine Seiteneffekte; der Actor wendet sie nur
an. `balances` ist nötig, weil die Strafe auf den Kontostand gekappt wird.
Damit liegt die gesamte Punkte-Ökonomie in einem Unit-Test, bevor eine
einzige WebSocket-Nachricht existiert.

Die Testfälle fallen direkt aus den Anforderungen: Normalfall, Push ohne
Gewinner (8.2), niemand tippt und die Runde wird annulliert (8.4), alle tippen
denselben richtigen Ausgang mit netto etwa null (8.5), ein Spieler mit 0
Punkten geht all-in und gewinnt (8.3), der Mindest-Anteil greift bei einem
Einsatz unter dem Minimum (7.1), die Rest-Verteilung nach größten Resten
(7.2), und die Strafe wird beim Spieler mit zu wenig Punkten gekappt.

Dazu ein Property-Test über zufällig erzeugte Wettbilder mit genau einer
Behauptung: **Die Summe aller Deltas ist exakt 0.** Das ist Invariante 5,
unmittelbar geprüft statt nur beschrieben.

## Etappe 3 — Zustandsautomat im Actor

Zuerst die Übergangstabelle, dann fällt das JSON-Schema fast von selbst.

| Ereignis | IDLE | OPEN | CLOSED | RESOLVED |
|---|---|---|---|---|
| `OPEN_MARKET` (Host) | → OPEN | Fehler | Fehler | → OPEN |
| `PLACE_BET` (Spieler) | Fehler | annehmen, wenn `now < closesAt` | Fehler | Fehler |
| `CLOSE_MARKET` (Host) | Fehler | → CLOSED | still ignorieren | Fehler |
| `AUTO_CLOSE(roundId)` | ignorieren | → CLOSED, wenn ID passt | ignorieren | ignorieren |
| `RESOLVE(outcome)` (Host) | Fehler | Fehler | → RESOLVED, verrechnen | Fehler |
| Join / Disconnect | in jedem Zustand erlaubt | | | |

`RESOLVED` ist bewusst ein eigener Zustand und nicht einfach `IDLE`: Das
Ergebnis der letzten Runde muss stehen bleiben, bis der Host die nächste
öffnet. Der Übergang `RESOLVED → IDLE` aus dem Diagramm passiert damit
implizit beim nächsten `OPEN_MARKET`.

Neu in `Room` eine `Round` mit monoton steigender `roundId`, Markt, Phase,
`closesAt`, den Tipps als `Map<playerId, Bet>`, dem eingefrorenen
Teilnehmerkreis und dem Ergebnis. `hostActionCount` fällt ersatzlos weg. Alles
weiterhin ohne `synchronized`, ohne `volatile` und ohne Concurrent-
Collections (Invariante 1) — der Zustand wird ausschließlich auf dem
Raum-Thread angefasst.

Der Auto-Close-Task schließt nichts selbst, er legt `close(roundId)` in
dieselbe Queue (ADR-010). Das Canceln der `ScheduledFuture` beim manuellen
Schließen bleibt reine Optimierung, die Absicherung ist der ID-Vergleich im
Handler. Ob ein Tipp zählt, entscheidet allein `serverNow < closesAt` beim
Abarbeiten (ADR-011).

Ebenfalls hier: der Zähler für verpasste Runden pro Spieler und die
vorgemerkte Host-Rückgabe, beide beim Übergang nach RESOLVED ausgewertet.

## Etappe 4 — Protokoll

Ein vollständiges `STATE` statt vieler Deltas, weil bei Reconnect ohnehin der
komplette Zustand nachgeliefert wird (Invariante 3). Der Inhalt hängt an der
Phase:

- **OPEN** — Markt mit Optionen und Anmerkungen, `closesAt`, `serverNow`,
  Anzahl abgegebener Tipps und Teilnehmerzahl. **Keine einzelnen Tipps**
  (Invariante 4, ADR-013).
- **CLOSED** — zusätzlich alle Tipps offen.
- **RESOLVED** — zusätzlich Gewinner-Ausgang, Pool und Delta je Spieler.

Zwei Details, die sonst spät weh tun:

**Der eigene Tipp während OPEN.** Ein Spieler muss sehen, was er getippt hat,
alle anderen dürfen es nicht. Statt `STATE` pro Empfänger unterschiedlich zu
serialisieren, geht eine separate `YOUR_BET`-Nachricht an die eine Session —
bei Annahme des Tipps und erneut beim Join oder Reconnect mitten in OPEN. Der
Broadcast bleibt damit ein einziger serialisierter String, und die verdeckte
Phase bleibt auch für jemanden verdeckt, der die Frames mitliest.

**Uhrenversatz.** Ein Handy mit falsch gestellter Uhr zeigt sonst einen
unsinnigen Countdown. `STATE` trägt `serverNow` mit; der Client bildet einmal
den Offset und interpoliert lokal. Das ist reine Anzeige — die Entscheidung
über die Gültigkeit eines Tipps fällt weiterhin ausschließlich serverseitig.

## Etappe 5 — Frontend

`useRoom.js` bleibt strukturell wie es ist und bekommt nur mehr
Nachrichtentypen; es rechnet weiterhin nichts aus. `App.jsx` wird in
Phasen-Ansichten zerlegt:

- **Tippen** — Optionsliste mit den Anmerkungen aus 4.1, Einsatz vorbelegt mit
  dem Mindesteinsatz, erhöhbar bis zum eigenen Kontostand. Ein Tipp pro
  Spieler pro Runde, kein Nachbessern.
- **Countdown** — aus `closesAt` und dem einmal gebildeten Offset.
- **Aufdeckung** — alle Tipps, sobald der Markt geschlossen ist.
- **Ergebnis** — Gewinner-Ausgang, Pool, Delta je Spieler.
- **Leaderboard** — dauerhaft sichtbar, mit Kennzeichnung für getrennte und
  pausierte Spieler.

Host-Knöpfe kontextabhängig: „Markt öffnen" nur in IDLE und RESOLVED, „Jetzt
schließen" nur in OPEN, „Auflösen" nur in CLOSED. Der Host ist eine Rolle,
kein Gerät — sonst ändert sich seine Ansicht nicht.

## Etappe 6 — Härtung und Kalibrierung

Reconnect gezielt in jeder Phase durchspielen; das Handy mitten im offenen
Fenster zu sperren ist der Realfall, nicht der Ausnahmefall. Zum Testen
mehrerer Spieler auf einem Rechner getrennte Browser-Profile verwenden, da
Tabs sich den localStorage teilen (ADR-014).

Danach die drei Parameter am echten Abend justieren und die Dokumentation
nachziehen: die hier getroffenen Entscheidungen aus `offene-entscheidungen.md`
herausnehmen und als ADR beziehungsweise in `anforderungen.md` aufnehmen —
insbesondere die Strafen-Kappung und die Pausenregel, die beide über den
heutigen Stand der Anforderungen hinausgehen.

## Reihenfolge und Schnitt

Die Etappen sind so geschnitten, dass nach jeder etwas Lauffähiges dasteht.
Etappe 1 und 2 ändern nichts Sichtbares, decken aber den Teil ab, der am
Spielabend nicht diskutabel sein darf. Ab Etappe 3 zusammen mit einer
minimalen Fassung von 4 und 5 ist eine erste Runde spielbar; der Rest von
Etappe 5 macht sie ansehnlich.
