# CLAUDE.md

Kontext für die Arbeit an diesem Projekt. Bei fachlichen Fragen zuerst
`docs/anforderungen.md` lesen, bei technischen `docs/adrs.md`. Was noch nicht
entschieden ist, steht in `docs/offene-entscheidungen.md` — dort bitte nichts
stillschweigend festlegen, sondern nachfragen. Was am ersten echten Spielabend
zu beobachten ist, steht in `docs/probelauf.md`.

## Was das ist

Eine Live-Wett-App für Freunde, die gemeinsam vor Ort ein Football-Spiel
schauen. Über ihre Handys tippen sie auf den Ausgang des nächsten Drives,
setzen dabei Punkte und teilen sich einen Pool nach Totalisator-Prinzip
(pari-mutuel). Kein echtes Geld, keine Buchmacher-Quoten.

Aktueller Stand: Fachlich vollständig. Der volle Rundenablauf ist umgesetzt
und durchspielbar, dazu der Wettkatalog aus Anforderung 4 mit vier Wetten,
eine Kurzanleitung im Spiel und der Broadcast-Look. Was fehlt, lässt sich
nicht mehr am Schreibtisch klären: Die drei Parameter aus Anforderung 3.1
sind implementiert, aber nicht am echten Spielabend kalibriert.

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
5. **Punkte sind ganzzahlig und nullsumme.** Der Pool ist exakt die Einsätze
   plus die *tatsächlich eingesammelten* Strafen — die Strafe wird auf den
   Kontostand gekappt (Anforderung 8.1), damit kein Konto negativ wird. Die
   Summe aller Auszahlungen entspricht exakt dem Pool. Keine Fließkommazahlen
   für Punkte. Reste werden nach dem Größte-Reste-Verfahren verteilt.
6. **Genau eine Server-Instanz.** Kein Autoscaling, kein Sharding. Zwei
   Instanzen wären zwei getrennte Räume.

## Aufbau

```
src/main/java/de/fourteenit/watchparty/
  room/RoomActor.java      Eventloop und Zustandsautomat (ADR-020): OPEN_BET,
                           PLACE_PICK, CLOSE_BET, RESOLVE, ANNUL, Auto-Close
  room/Room.java           Raumzustand, Host-Rolle, Rundenverwaltung
  room/Round.java          Eine Runde: Wette, closesAt, eingefrorener
                           Teilnehmerkreis, Tipps, Ergebnis
  room/Settlement.java     Abrechnung als reine Funktion (Anforderung 7/8)
  room/Player.java         Teilnehmer, inkl. Verpasste-Runden-Zähler (8.1)
  room/Phase.java          IDLE/OPEN/CLOSED/RESOLVED
  room/Bets.java           Wettkatalog (ADR-017), einzige Quelle für Wetten
  room/Bet.java            Eine Wette: Frage, Regel, Ausgänge
  room/Pick.java           Ein abgegebener Tipp (ADR-022)
  ws/GameWebSocketHandler  Frames -> Kommandos, ändert selbst nichts
  ws/ClientSession.java    Verbindung mit eigener Ausgangs-Queue
  protocol/Messages.java   Nachrichten Server -> Client (STATE, YOUR_PICK, ...)
frontend/src/
  useRoom.js               Verbindung, Reconnect, Token, Uhren-Offset
  App.jsx                  Phasen-Ansichten: Tippen, Countdown, Aufdeckung,
                           Ergebnis, Leaderboard
  Guide.jsx                Kurzanleitung als Overlay, baut den Wettkatalog
                           aus den Serverdaten auf
docs/                      Anforderungen, ADRs, offene Entscheidungen,
                           Beobachtungsbogen für den Probelauf
```

## Nächster Schritt

Der erste Probelauf an einem echten Spielabend. Was dabei zu beobachten ist
— Parameter, Fensterlänge, Größe des Wettkatalogs, Verhalten der Handys —
steht als Beobachtungsbogen in `docs/probelauf.md`. Reconnect ist über
automatisierte Tests in jeder Phase durchgespielt (`ReconnectTest`); was
bleibt, sind Mobile-Browser-Eigenheiten (Tab-Suspend, Wake Lock), die sich
nicht simulieren lassen.

## Konventionen

- Sprache im Code: Bezeichner englisch, Kommentare und Dokumentation deutsch.
- Kommentare erklären das *Warum* (meist einen ADR), nicht das *Was*.
- Der Host ist eine Rolle, kein Gerät: nur zusätzliche Steuerknöpfe.
- Fachbegriffe konsistent halten: Wette, Wettfenster, Runde, Tipp, Einsatz,
  Anteil, Pool, Strafe, Auflösen. Es heißt **nicht** Markt (ADR-022).
- Zwei Begriffe, die sich im Code leicht verwechseln (ADR-022): eine `Bet`
  ist die *Wette*, also die Frage; ein `Pick` ist der *Tipp* eines Spielers.
- Sichtbare Texte stehen mit Umlauten im Quelltext; die Kodierung ist in
  `build.gradle.kts` auf UTF-8 festgenagelt.
