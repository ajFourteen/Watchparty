# Plan: Raumzustand über ein Update hinweg erhalten

Entwurf, noch nichts entschieden und nichts implementiert. Der Plan berührt
ADR-004 und einen Punkt aus `offene-entscheidungen.md` („Keine Persistenz")
— die Fragen am Ende brauchen eine Antwort, bevor Code entsteht.

## 1. Problem

Ein Deploy ist ein Neustart (ADR-018, ADR-019), ein Neustart verliert nach
ADR-004 den kompletten Raumzustand: Punkte, Namen, Tokens, Host-Rolle,
laufende Runde. Danach sitzen alle wieder bei 1000 Punkten und mit fremdem
Namen da. Dasselbe passiert bei OOM-Kill, Fly-Wartung und bei einem Rollback
(`fly deploy --image <alte ImageRef>`) — der Rollback macht den Fehler
schneller behoben, aber den Abend trotzdem kaputt.

Heute wird das über Disziplin abgefangen: nicht am Spieltag nach `master`
mergen. Das ist eine Regel, die genau dann bricht, wenn ein Fix am
Spielabend nötig ist — also im wichtigsten Fall.

**Ziel:** Ein Neustart innerhalb desselben Spielabends kostet die
Verbindungen, aber nicht den Spielstand. Ein Fix darf während der Halbzeit
raus.

**Nicht Ziel:** Persistenz über Spielabende hinweg. Der Ausschluss aus
`offene-entscheidungen.md` bleibt bestehen; der Snapshot bekommt deshalb eine
Verfallszeit (Abschnitt 6).

## 2. Verhältnis zu ADR-004

ADR-004 begründet sich mit „Persistenz über Spielabende hinweg ist nicht
gefordert" und nimmt den Verlust bei Neustart bewusst in Kauf. Der Plan
widerspricht dem ersten Satz nicht, sondern nur der Konsequenz. Er wäre ein
Nachtrag (ADR-023), kein Widerruf:

- Keine Datenbank, kein zusätzlicher Dienst.
- Der Arbeitsspeicher bleibt die maßgebliche Kopie. Die Datei ist ein
  Abzug, aus dem nur beim Start gelesen wird — nie im laufenden Betrieb.
- Kein Zustand überlebt den Abend (Verfallszeit).

## 3. Was in den Snapshot gehört

Alles, was ein Spieler nach dem Neustart wiedererkennen muss:

| Quelle | Feld | Warum |
| --- | --- | --- |
| `Player` | `id`, `token` | Ohne Token kein Reconnect (ADR-014) — das ist der eigentliche Kern |
| `Player` | `name`, `points` | Der Spielstand |
| `Player` | `missedRounds` | Sonst startet die Pausenlogik (8.1) neu |
| `Room` | `hostPlayerId` | Sonst wandert die Host-Rolle beim Wiederverbinden zufällig |
| `Room` | `nextRoundId` | Die ID-Wache (ADR-010) braucht Monotonie |
| `Round` | `id`, `betId`, `closesAt`, `phase` | Die laufende Runde |
| `Round` | `participants`, `picks` | Eingefrorener Kreis und abgegebene Tipps |
| `Round` | `winningOutcomeId`, `deltas`, `pool`, `annulled`, `annulledByHost` | Damit die Ergebnisansicht nach dem Neustart noch stimmt |

Nicht in den Snapshot:

- `ClientSession` und die Session-Map. Verbindungen überleben nicht,
  Verbindungen werden neu aufgebaut.
- `Player.connected`. Beim Laden ist jeder getrennt; wer sich meldet, wird
  verbunden. Alles andere wäre gelogen.
- Der Inhalt der Wetten. Gespeichert wird nur `betId`; `Bets` bleibt die
  einzige Quelle für den Katalog (ADR-017). Sonst könnte eine alte Runde
  eine Wette wiederbeleben, die es im neuen Stand nicht mehr gibt.
- Die Parameter aus 3.1. Die kommen aus dem Code, nicht aus der Datei —
  sonst zieht ein Snapshot alte Werte über ein Update, das genau diese
  Werte ändern wollte.

Format: eine JSON-Datei mit `schemaVersion` und `savedAt`. Größenordnung
wenige Kilobyte.

## 4. Schreiben, ohne die Invarianten zu brechen

Zwei Regeln kollidieren hier fast: Der Zustand darf nur auf dem Raum-Thread
gelesen werden (Invariante 1), und der Raum-Thread darf nicht blockieren
(Invariante 2) — Dateisystem-I/O auf dem Raum-Thread wäre genau das.

Auflösung analog zur Ausgangs-Queue in `ClientSession` (ADR-012):

1. Auf dem Raum-Thread wird ein **unveränderliches Snapshot-Objekt** gebaut
   — dieselbe Stelle, an der heute `broadcastState()` steht. Reines Kopieren
   von Feldern, kein I/O.
2. Das Objekt geht an einen `SnapshotStore` mit eigenem Single-Thread-
   Executor. Der serialisiert und schreibt.
3. **Verdichten:** Der Store hält nur den jeweils neuesten ausstehenden
   Snapshot. Wer während eines laufenden Schreibvorgangs nachlegt,
   überschreibt den Wartenden. Damit kann sich keine Queue aufstauen, egal
   wie oft getippt wird.
4. Schreiben atomar: in `room.json.tmp`, `fsync`, dann `ATOMIC_MOVE` auf
   `room.json`. Ein Absturz mitten im Schreiben darf keine halbe Datei
   hinterlassen.
5. Fehler beim Schreiben werden geloggt und sonst geschluckt. Ein volles
   Dateisystem darf das Spiel nicht anhalten.

Praktisch heißt das: `broadcastState()` und `persist()` fassen sich zu einer
Methode zusammen, damit „Zustand geändert, aber nicht gespeichert" gar nicht
erst als Zustand existiert.

## 5. Laden beim Start

Gelesen wird genau einmal, und zwar als **erstes Kommando in der
Actor-Queue** (`@PostConstruct` reiht es ein). Damit läuft das Laden auf dem
Raum-Thread und ist garantiert vor dem ersten `JOIN` fertig — die WebSocket-
Handler reihen ja nur ein und drängeln sich nicht vor. Invariante 1 bleibt
ohne Sonderfall bestehen.

Blockierendes Lesen ist hier in Ordnung: Es passiert vor der ersten
Verbindung, es gibt nichts zu blockieren.

## 6. Sonderfälle beim Laden

Der schlimmste denkbare Ausgang ist ein Snapshot, der den Start
zerschießt — dann startet die Maschine in einer Schleife neu und der Abend
ist endgültig vorbei. Deshalb gilt durchgängig: **Im Zweifel leer starten,
nie hochkommen ist keine Option.**

| Fall | Verhalten |
| --- | --- |
| Datei fehlt | Leerer Raum. Normalfall beim ersten Start. |
| Datei kaputt oder `schemaVersion` unbekannt | Leerer Raum, `ERROR` ins Log, Datei nach `room.json.bad` umbenennen (damit die nächste Runde nicht wieder darüber stolpert). |
| `savedAt` älter als die Verfallszeit | Leerer Raum. Hält den Ausschluss „keine Persistenz über Spielabende hinweg" ein. |
| `betId` gibt es im Katalog nicht mehr | Spieler und Punkte laden, laufende Runde verwerfen (Phase `IDLE`). Der Host macht eine neue auf. |
| Runde war `OPEN`, `closesAt` liegt in der Zukunft | Runde bleibt `OPEN`, Auto-Close mit der Restzeit neu einplanen (ADR-010). |
| Runde war `OPEN`, `closesAt` ist abgelaufen | Runde wird `CLOSED`. Die Tipps sind gültig, wer nach `closesAt` hätte tippen wollen, hätte nach ADR-011 ohnehin nicht mehr gedurft. Ob das fair ist, ist Frage B unten. |
| Runde war `CLOSED` oder `RESOLVED` | Unverändert übernehmen. |
| Host war gesetzt | `hostPlayerId` übernehmen. Beim ersten `JOIN` greift die vorhandene Logik: Der Host gilt als getrennt, die Rolle wandert an den ersten Verbundenen (ADR-021) und wird zurückgeholt, sobald der eigentliche Host in `IDLE`/`RESOLVED` wieder da ist. Kein neuer Code. |

Systemuhr: `closesAt` ist ein absoluter Zeitstempel und übersteht den
Neustart. Beim Wiederverbinden schickt `STATE` ohnehin die Serverzeit mit,
der Client zieht seinen Offset nach.

## 7. Infrastruktur

Ohne dauerhaften Speicher bringt die Datei nichts: Das Container-
Dateisystem ist bei jedem Deploy neu.

- Fly-Volume anlegen (`fly volumes create ... -r fra -s 1`), in `fly.toml`
  unter `[mounts]` nach `/data` mounten.
- Pfad als Property `watchparty.snapshot.path`, per Umgebungsvariable
  gesetzt. **Leer = Persistenz aus** — das ist die Voreinstellung für
  lokale Entwicklung und Tests und gleichzeitig der Notausschalter, falls
  der Snapshot am Spielabend Ärger macht.
- ADR-005 bleibt unberührt, wird aber schärfer: Ein Volume ist an eine
  Maschine gebunden. Zwei Maschinen hätten jetzt nicht nur zwei Räume,
  sondern auch zwei Dateien. `--ha=false` und `fly machines list` bleiben
  Pflicht.
- Ehrliche Grenze: Ein Volume überlebt ein Deploy, aber nicht das Ersetzen
  der Maschine (Hardware-Ausfall, Regionswechsel). Der Snapshot ist eine
  deutliche Verbesserung, keine Garantie. Das gehört so in den ADR.

## 8. Frontend

Voraussichtlich keine Änderung nötig: `useRoom.js` versucht nach `onclose`
alle 1,5 Sekunden neu zu verbinden und schickt beim Öffnen automatisch
`JOIN` mit dem Token aus dem `localStorage`. Ein Deploy dauert einige
Sekunden; die Clients kommen von selbst zurück.

Zu prüfen am echten Gerät: ob nach einem Serverneustart die
Wiederverbindungs-Ansicht nicht dauerhaft klemmt und ob die Ansicht kurz
etwas Falsches zeigt, bevor der erste `STATE` da ist.

## 9. Tests

Der Zweck des Ganzen ist ein Abend, der nicht kaputtgeht — das darf nicht
erst am Spielabend geprüft werden.

- `SnapshotTest`: Hin- und Rückweg für jede Phase (`IDLE`, `OPEN`,
  `CLOSED`, `RESOLVED`, annulliert), inklusive Tipps, Punkten und
  `missedRounds`.
- `RestoreTest`: jeder Sonderfall aus Abschnitt 6, jeweils mit dem
  Nachweis, dass danach weitergespielt werden kann.
- Erweiterung von `ReconnectTest` um einen echten Neustart: Actor mit
  Snapshot-Datei aufsetzen, Actor wegwerfen, neuen Actor auf dieselbe Datei
  setzen, mit demselben Token wieder beitreten — Punkte, Name und Tipp
  müssen stehen.
- Nachweis, dass das Laden vor dem ersten `JOIN` fertig ist.
- Ein `FakeClock`-Fall für die Verfallszeit.

## 10. Schritte

Jeder Schritt ein eigener Commit, jeder Schritt für sich lauffähig.

1. `feat:` Snapshot-Datenmodell und Serialisierung, ohne Anbindung
   (`SnapshotTest`).
2. `feat:` `SnapshotStore` mit Schreib-Thread und Verdichten; Schreiben an
   `broadcastState()` hängen. Ab hier entsteht die Datei, gelesen wird noch
   nichts.
3. `feat:` Laden beim Start inklusive aller Sonderfälle (`RestoreTest`,
   erweiterter `ReconnectTest`).
4. `chore:` Fly-Volume, `fly.toml`, README (auch: was tun, wenn ein
   Snapshot stört).
5. `docs:` ADR-023 als Nachtrag zu ADR-004, `offene-entscheidungen.md`
   nachziehen, Invarianten in `CLAUDE.md` ergänzen.

Schritt 1–3 sind der Kern und hängen aneinander; 4 und 5 lassen sich davon
trennen.

## 11. Verworfene Alternativen

- **Datenbank oder Redis.** Löst dasselbe Problem mit einem zusätzlichen
  Dienst, dessen Ausfall den Abend genauso kostet. Widerspricht ADR-004 im
  Kern, nicht nur in der Konsequenz.
- **Snapshot im Browser des Hosts, der ihn nach dem Neustart hochlädt.**
  Bräuchte kein Volume, macht aber ein Endgerät zur Quelle der Wahrheit und
  bricht Invariante 3. Wer die Frames manipuliert, verteilt sich Punkte.
- **Zweite Instanz zur Übergabe.** Bricht ADR-005.
- **Nur die Punkte sichern, Runden verwerfen.** Wäre einfacher, aber der
  Neustart trifft am ehesten die laufende Runde — genau die, die dann
  verloren ginge.

## 12. Fragen, die vorab beantwortet werden müssen

**A. Ist der Nachtrag zu ADR-004 gewollt?** Der Ausschluss „keine
Persistenz, keine Datenbank" steht ausdrücklich in
`offene-entscheidungen.md`. Der Plan bleibt bei „keine Datenbank", legt aber
eine Datei an. Ohne ein Ja hierzu passiert nichts.

**B. Was passiert mit einer Runde, deren Fenster während des Neustarts
abgelaufen ist?** Vorschlag: `CLOSED`, der Host entscheidet — annullieren
kann er mit einem Knopf (8.6). Alternative wäre automatisches Annullieren,
weil der Neustart Tippzeit gestohlen hat. Beides vertretbar; der Vorschlag
kommt ohne neuen Mechanismus aus.

**C. Wie lang ist die Verfallszeit?** Sie trennt „Neustart mitten im Abend"
von „nächster Spielabend". Vorschlag: 6 Stunden — länger als ein Spiel
inklusive Verlängerung und Pausen, kürzer als der Abstand zum nächsten
Spieltag.

**D. Ist das Fly-Volume in Ordnung?** Es kostet ein paar Cent im Monat und
bindet die Maschine fester an einen Host. Ohne Volume bringt der Snapshot
nur bei einem Absturz ohne Deploy etwas — also gerade nicht im Hauptfall.
