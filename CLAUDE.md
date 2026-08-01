# CLAUDE.md

Kontext für die Arbeit an diesem Projekt. Bei fachlichen Fragen zuerst
`docs/anforderungen.md` lesen, bei technischen `docs/adrs.md`. Was noch nicht
entschieden ist, steht in `docs/offene-entscheidungen.md` — dort bitte nichts
stillschweigend festlegen, sondern nachfragen.

## Was das ist

Eine Live-Wett-App für Freunde, die gemeinsam vor Ort ein Football-Spiel
schauen. Über ihre Handys tippen sie auf den Ausgang des nächsten Drives,
setzen dabei Punkte und teilen sich einen Pool nach Totalisator-Prinzip
(pari-mutuel). Kein echtes Geld, keine Buchmacher-Quoten.

Aktueller Stand: **Walking Skeleton**. Alle Schichten sind einmal
durchstochen (Join, Broadcast, Host-Rolle, Deployment), die eigentliche
Wett-Logik fehlt noch.

## Stack

- Java 21, Spring Boot 3.3, rohe WebSocket (kein STOMP), Gradle (Kotlin DSL)
- React 18 mit Vite, Build wird ins Jar gepackt
- Ein Container, eine Instanz, Zustand nur im Arbeitsspeicher

## Bauen und laufen lassen

```bash
# Entwicklung: zwei Terminals
gradle bootRun -PskipFrontend        # Backend auf :8080
cd frontend && npm run dev           # Frontend auf :5173, proxyt /ws

# Produktionsnah
gradle bootJar && java -jar build/libs/watchparty-0.1.0.jar
```

## Harte Invarianten

Diese Regeln sind das Ergebnis expliziter Entscheidungen. Wenn eine Änderung
sie brechen würde, ist das ein Anlass nachzufragen, kein Detail.

1. **Aller Zustand wird ausschließlich auf dem Raum-Thread verändert.**
   WebSocket-Handler reihen nur Kommandos in `RoomActor` ein und fassen
   `Room`/`Player` nie direkt an. Das gilt auch für Verbindungsauf- und
   -abbau. Deshalb braucht die Raum-Logik kein `synchronized`, kein
   `volatile` und keine Concurrent-Collections — und darf auch keine
   bekommen, weil das die Regel verschleiern würde.
2. **Der Raum-Thread blockiert nie.** Er berechnet Zustand und Nachrichten;
   das Schreiben auf Sockets läuft über die Ausgangs-Queue in
   `ClientSession`. Ein eingeschlafenes Handy darf das Spiel nicht anhalten.
3. **Der Server ist die einzige Quelle der Wahrheit.** Clients rechnen keine
   Punkte aus, entscheiden nicht über Fensterschluss und halten keinen
   eigenen Verlauf. Bei Reconnect wird der komplette Zustand neu geschickt.
4. **Verdeckte Tipps sind eine Anforderung an die Leitung, nicht an die UI.**
   Solange ein Wettfenster offen ist, darf der Server keinen einzelnen Tipp
   senden, nur den Zähler. Wer die Frames mitliest, darf nichts erfahren.
5. **Punkte sind ganzzahlig und nullsumme.** Der Pool ist exakt Einsätze plus
   Strafen; die Summe aller Auszahlungen entspricht exakt dem Pool. Keine
   Fließkommazahlen für Punkte. Reste werden nach dem Größte-Reste-Verfahren
   verteilt.
6. **Genau eine Server-Instanz.** Kein Autoscaling, kein Sharding. Zwei
   Instanzen wären zwei getrennte Räume.

## Aufbau

```
src/main/java/de/fourteenit/watchparty/
  room/RoomActor.java      Eventloop; hier kommt die Spiellogik hinein
  room/Room.java           Raumzustand; hier kommt der Zustandsautomat hinein
  room/Player.java         Teilnehmer
  ws/GameWebSocketHandler  Frames -> Kommandos, ändert selbst nichts
  ws/ClientSession.java    Verbindung mit eigener Ausgangs-Queue
  protocol/Messages.java   Nachrichten Server -> Client
frontend/src/
  useRoom.js               Verbindung, Reconnect, Token
  App.jsx                  Join-Screen und Raumansicht
docs/                      Anforderungen, ADRs, offene Entscheidungen
```

## Nächster Schritt

Der Zustandsautomat `IDLE → OPEN → CLOSED → RESOLVED → IDLE`. Erlaubte
Ereignisse je Zustand festlegen, dann fällt das JSON-Schema fast von selbst.
Die Queue und die Host-Prüfung existieren bereits; es kommen im Wesentlichen
neue `handle*`-Methoden in `RoomActor` dazu.

Zwei Fallen, die dabei schon bekannt sind:

- Ein Auto-Close-Timer aus einer beendeten Runde darf die Folgerunde nicht
  schließen. Jede Runde bekommt eine ID, das `CLOSE`-Ereignis trägt sie mit,
  und der Handler ignoriert nicht passende IDs. Das Canceln der
  `ScheduledFuture` ist nur eine Optimierung, nicht die Absicherung.
- Ob ein Tipp noch zählt, entscheidet der Vergleich `serverNow < closesAt`
  beim Abarbeiten — nicht die Frage, ob der Timer-Task schon gefeuert hat.

## Konventionen

- Sprache im Code: Bezeichner englisch, Kommentare und Dokumentation deutsch.
- Kommentare erklären das *Warum* (meist einen ADR), nicht das *Was*.
- Der Host ist eine Rolle, kein Gerät: nur zusätzliche Steuerknöpfe.
- Fachbegriffe konsistent halten: Markt, Wettfenster, Runde, Tipp, Einsatz,
  Anteil, Pool, Strafe, Auflösen.
