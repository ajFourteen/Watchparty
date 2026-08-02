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
eine Kurzanleitung im Spiel und der Broadcast-Look. Seit ADR-023 übersteht
der Raumzustand außerdem einen Neustart innerhalb desselben Abends (Snapshot
auf Platte, Fly-Volume), mit `RESET` als explizitem Gegenstück für den Host.
Was fehlt, lässt sich nicht mehr am Schreibtisch klären: Die drei Parameter
aus Anforderung 3.1 sind implementiert, aber nicht am echten Spielabend
kalibriert.

## Stack

- Java 21, Spring Boot 3.3, rohe WebSocket (kein STOMP), Gradle (Kotlin DSL)
- React 18 mit Vite, Build wird ins Jar gepackt
- Ein Container, eine Instanz, Zustand im Arbeitsspeicher — seit ADR-023
  zusätzlich als Snapshot auf einem Fly-Volume gesichert (kein Ersatz,
  nur ein Abzug für Neustarts innerhalb desselben Abends)

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
   WebSocket-Handler reihen nur Kommandos über `RoomCommands` ein und fassen
   `Room`/`Player` nie direkt an. Das gilt auch für Verbindungsauf- und
   -abbau. Deshalb braucht die Raum-Logik kein `synchronized`, kein
   `volatile` und keine Concurrent-Collections — und darf auch keine
   bekommen, weil das die Regel verschleiern würde.
2. **Der Raum-Thread blockiert nie.** Er berechnet Zustand und Nachrichten;
   das Schreiben auf Sockets läuft über `ClientGateway` in die Ausgangs-Queue
   der `ClientSession`. Ein eingeschlafenes Handy darf das Spiel nicht
   anhalten. Dasselbe gilt für das Schreiben des Snapshots.
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

Dazu kommt seit ADR-024 eine strukturelle Regel, die diese Invarianten
ergänzt statt sie zu ersetzen: **Abhängigkeiten zeigen nur nach innen.**
`domain` kennt niemanden, `application` kennt nur die Domäne und seine
Ports, Spring und WebSockets leben ausschließlich in `adapter` und `config`.
`ArchitectureTest` prüft das an den Importzeilen — wer die Regel bricht,
bekommt einen roten Test und keine Diskussion. Achtung: Die Ringe ordnen
nach Abhängigkeitsrichtung, nicht nach Thread. Die Queue zwischen Handler
und Actor, an der Invariante 1 hängt, ist im Paketbaum deshalb *nicht* mehr
zu sehen — dafür sind diese Invarianten hier da.

Der Snapshot aus ADR-023 ist kein Sonderfall dieser Regeln, sondern ihre
Anwendung: Das Snapshot-Objekt entsteht als reine Feldkopie auf dem
Raum-Thread (Invariante 1), das eigentliche Schreiben läuft auf einem
eigenen Thread in `SnapshotStore`, damit der Raum-Thread nicht auf
Dateisystem-I/O wartet (Invariante 2) — analog zur Ausgangs-Queue in
`ClientSession`. Wer daran etwas ändert, prüft beide Invarianten mit.

## Aufbau

Onion-Architektur (ADR-024): Abhängigkeiten zeigen ausschließlich nach
innen. Innerhalb der Domäne trägt das Modell zusätzlich Aggregate, Entities
und Value Objects (ADR-025) — die Sprache aus `docs/anforderungen.md` findet
sich eins zu eins im Typsystem wieder. `ArchitectureTest` (ArchUnit) prüft
beides, es ist keine bloße Verabredung.

Zusätzlich gilt JSpecify-Nullness (ADR-026): `domain`, `application`,
`adapter` und `config` sind `@NullMarked` — jeder Verweistyp ist dort
nicht-null, sofern nicht ausdrücklich `@Nullable`. NullAway prüft das beim
Kompilieren (`gradle compileJava`), nicht erst zur Laufzeit oder in der IDE.
Ein `@Nullable` an einer Stelle im Domänenmodell ist deshalb keine
Empfehlung, sondern eine Zusicherung, die der Compiler nachhält — ebenso wie
ihr Fehlen. Testcode ist bewusst nicht `@NullMarked` (siehe ADR-026).

```
src/main/java/de/fourteen/watchparty/
  domain/model/            Der Kern. Kein Spring, kein Jackson, kein WebSocket.

    -- Aggregate Root --
    Room.java              Raumzustand, Host-Rolle, Rundenverwaltung; die
                           Übergänge closeCurrentRound/annulCurrentRound/
                           resolveCurrentRound/applyDeltas/addPick sowie
                           toSnapshot()/fromSnapshot() (ADR-023)

    -- Entities (Identität über eine ID, nicht über die Werte) --
    Round.java             Eine Runde: Wette, closesAt, eingefrorener
                           Teilnehmerkreis, Tipps, Ergebnis. Mutatoren
                           paket-privat — das ist die Aggregatgrenze
    Player.java            Teilnehmer, Verpasste-Runden-Zähler (8.1) und die
                           Einsatzregel stakeFor (6/8.3)

    -- Value Objects (Identität über die Werte, unveränderlich) --
    PlayerId/RoundId/BetId/OutcomeId   Identitäten, gegeneinander nicht
                           austauschbar — Vertauschen ist ein Kompilierfehler
    Token.java             Wiedererkennung ueber Verbindungsabbrüche (ADR-014)
    PlayerName.java        Trägt die Regel "1 bis 20 Zeichen" (statt einer
                           if-Kette im Actor)
    Points.java            Punkte: nie negativ (Invariante 5 als Typinvariante,
                           nicht als Kommentar)
    PointsDelta.java        Die Veränderung eines Kontostands — Gewinn/Verlust,
                           darf negativ sein, eigener Typ als Gegenstück zu
                           Points
    Share.java             Anteil am Pool (Anforderung 7): von Points streng
                           getrennt, damit Punkte und Anteile sich nicht
                           versehentlich vermischen
    Phase.java             IDLE/OPEN/CLOSED/RESOLVED
    Bet/Outcome/Pick       Wette, Ausgang, abgegebener Tipp (ADR-022)
    Bets.java              Wettkatalog (ADR-017) — Datenstruktur, kein
                           Dienst, deshalb hier und nicht in domain/service
    Params.java            Startguthaben, Mindesteinsatz, Strafe (3.1)
    RoomSnapshot.java      Bewusst OHNE Value Objects: das Dateiformat für
                           die Platte (ADR-023), unabhängig vom Modell.
                           Room.toSnapshot/fromSnapshot rechnet um
  domain/service/
    Settlement.java        Domain Service: Abrechnung als reine Funktion
                           (Anforderung 7/8), gehört zu keiner einzelnen
                           Entity. Liefert Deltas, Pool und Annullierung
                           als Result
  application/             Orchestrierung. Kennt die Domäne und die Ports,
                           sonst nichts — insbesondere kein Spring.
    RoomActor.java         Eventloop und Zustandsautomat (ADR-020): OPEN_BET,
                           PLACE_PICK, CLOSE_BET, RESOLVE, ANNUL, RESET,
                           Auto-Close, Laden des Snapshots beim Start. Hält
                           die Zuordnung Sitzung -> Spieler
    RoomView.java          Projektion Raumzustand -> Nachricht, rein lesend;
                           hier hängt Invariante 4 (nur der Zähler in OPEN)
    message/Messages.java  Nachrichten Server -> Client (STATE, YOUR_PICK, ...)
    port/in/RoomCommands   Was von außen ausgelöst werden kann. Spricht
                           Sitzungs-IDs, nie Verbindungsobjekte
    port/out/              ClientGateway (an die Clients), SnapshotRepository
                           (auf Platte), Scheduler (verzögerte Ausführung)
  adapter/in/ws/
    GameWebSocketHandler   Frames -> Kommandos, ändert selbst nichts
    WebSocketClientGateway Hält die Verbindungen, serialisiert nach JSON
    ClientSession.java     Verbindung mit eigener Ausgangs-Queue (ADR-012)
    WebSocketConfig.java   Registriert den Handler auf /ws
  adapter/out/file/
    SnapshotStore.java     Schreiben/Lesen auf Platte, eigener Thread
  adapter/out/time/
    ScheduledExecutorScheduler
  config/                  Sämtliche Spring-Beans: RoomConfig verdrahtet den
                           Actor, TimeConfig Uhr und Scheduler, SnapshotConfig
                           den Pfad watchparty.snapshot.path
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
automatisierte Tests in jeder Phase durchgespielt (`ReconnectTest`), ein
echter Server-Neustart über `RestoreTest` (ADR-023); was bleibt, sind
Mobile-Browser-Eigenheiten (Tab-Suspend, Wake Lock) und das Fly-Volume
selbst, die sich nicht am Schreibtisch simulieren lassen.

## Konventionen

- Sprache im Code: Bezeichner englisch, Kommentare und Dokumentation deutsch.
- Kommentare erklären das *Warum* (meist einen ADR), nicht das *Was*.
- Der Host ist eine Rolle, kein Gerät: nur zusätzliche Steuerknöpfe.
- Fachbegriffe konsistent halten: Wette, Wettfenster, Runde, Tipp, Einsatz,
  Anteil, Pool, Strafe, Auflösen. Es heißt **nicht** Markt (ADR-022).
- Zwei Begriffe, die sich im Code leicht verwechseln (ADR-022): eine `Bet`
  ist die *Wette*, also die Frage; ein `Pick` ist der *Tipp* eines Spielers.
- Ein Fachbegriff aus `anforderungen.md`, der keinen eigenen Typ im
  Domänenmodell hat, ist ein Anlass nachzufragen (ADR-025) — nicht
  stillschweigend als `int`/`String` weiterzuschreiben.
- Punkte (`Points`), Anteile (`Share`) und die Veränderung eines Kontostands
  (`PointsDelta`) sind drei verschiedene Typen, obwohl alle drei intern eine
  Ganzzahl sind (ADR-025). Anforderung 7 trennt sie fachlich, das Typsystem
  erzwingt es: Ein `Share` lässt sich nicht versehentlich als `Points`
  auszahlen.
- Test Doubles werden von Hand geschrieben, kein Mockito (ADR-025) — vom
  Test-Classpath ausgeschlossen.
- `@Nullable` (`org.jspecify.annotations`) steht direkt vor dem Typ, den es
  betrifft — bei einem qualifizierten Typ wie `Scheduler.ScheduledTask` also
  `Scheduler.@Nullable ScheduledTask`, nicht davor (ADR-026). Wo NullAway
  eine Nicht-Null-Bedingung nicht selbst herleiten kann (z. B. ein
  Map-Zugriff, dessen Schlüssel nachweislich existiert), macht ein expliziter
  `Objects.requireNonNull(...)` mit Begründung im Kommentar die Annahme
  sichtbar, statt sie stillschweigend vorauszusetzen.
- Sichtbare Texte stehen mit Umlauten im Quelltext; die Kodierung ist in
  `build.gradle.kts` auf UTF-8 festgenagelt.
- Jede abgeschlossene Änderung bekommt einen eigenen Commit, direkt wenn sie
  fertig ist — nicht auf spätere Sammel-Commits warten. Commit-Messages
  folgen Conventional Commits (`fix:`, `feat:`, `docs:`, `refactor:`,
  `test:`, `chore:` ...).
