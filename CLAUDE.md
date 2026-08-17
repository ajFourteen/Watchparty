# CLAUDE.md

Kontext für die Arbeit an diesem Projekt. Bei fachlichen Fragen zuerst
`docs/anforderungen.md` lesen, bei technischen `docs/adrs.md`. Was noch nicht
entschieden ist, steht in `docs/offene-entscheidungen.md` — dort bitte nichts
stillschweigend festlegen, sondern nachfragen. Was am ersten echten Spielabend
zu beobachten ist, steht in `docs/probelauf.md`. Wie getestet wird — Ebenen,
Kritikalität, Metriken, Vorgehen bei neuen Features — steht in
`docs/teststrategie.md`.

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
Seit ADR-033 hält ein Prozess mehrere, voneinander vollständig getrennte
Watchpartys gleichzeitig, adressiert über einen vierstelligen Code
(`docs/features/004-mehrere-watchpartys.md`) — innerhalb einer Watchparty
gilt weiterhin, dass immer nur eine Runde gleichzeitig läuft. Was fehlt,
lässt sich nicht mehr am Schreibtisch klären: Die drei Parameter aus
Anforderung 3.1 sind implementiert, aber nicht am echten Spielabend
kalibriert.

Seit dem 2026-08-17 entsteht daneben ein zweiter, unabhängiger Spielmodus,
das Tippspiel über eine ganze Saison (`docs/features/005-tippspiel-liga.md`,
ADR-034 bis ADR-039, Kapitel 13 in `anforderungen.md`). Gebaut wird
stufenweise; Stufe 0 (Entscheidungen), Stufe 1 (Wertung, `domain/*/league`)
und Stufe 2 (Persistenz: Postgres/Flyway unter `adapter/out/db`, erster
Baustein `Account`) sind fertig, alles Weitere — Konten, Spieldaten, Tippen,
Ligen, Oberfläche, Betrieb — steht noch aus (Tabelle im Feature-Dokument). Die
Live-Wetten sind davon nicht betroffen: Beide Modi teilen sich die Anwendung
und sonst nichts.

## Stack

- Java 25, Spring Boot 3.5, rohe WebSocket (kein STOMP), Gradle (Kotlin DSL)
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
   Instanzen wären zwei getrennte Mengen von Watchpartys, mit Sitzungen, die
   zufällig auf der falschen landen.
7. **Watchpartys sind vollständig voneinander getrennt** (ADR-033). Keine
   Nachricht und kein Kommando einer Watchparty wirkt auf eine andere —
   weder Zustand, noch Tipps, noch Host-Rechte, noch ein Token. Der
   gemeinsame Raum-Thread aus Invariante 1 bedient dabei alle Watchpartys,
   nicht mehr nur eine; die Trennung entsteht über die Zuordnung Sitzung →
   Watchparty, nicht über getrennte Threads.

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

Seit ADR-027 tragen Aggregat, Entities, Value Objects und Domain Services
zusätzlich die jMolecules-Stereotypen (`@AggregateRoot`, `@Entity`,
`@ValueObject`, `@Identity`, `@Service`), und die Ringe aus ADR-024 sind
zusätzlich mit den Onion-Ring-Annotationen (`@DomainModelRing` usw.)
markiert — dieselbe Struktur, jetzt zweimal geprüft: einmal über
Paketnamen, einmal über Annotationen. `ArchitectureTest` prüft beides mit
eigenen, selbst geschriebenen ArchUnit-Regeln, nicht mit den
vorgefertigten jMolecules-Regeln (siehe `build.gradle.kts` für den Grund).

```
src/main/java/de/fourteen/watchparty/
  domain/model/            Der Kern. Kein Spring, kein Jackson, kein WebSocket.

    -- Aggregate Root (@AggregateRoot, ADR-027) --
    Room.java              Eine Watchparty: Raumzustand, Host-Rolle,
                           Rundenverwaltung; die Übergänge
                           closeCurrentRound/annulCurrentRound/
                           resolveCurrentRound/applyDeltas/addPick sowie
                           toSnapshot()/fromSnapshot() (ADR-023). Trägt seit
                           ADR-033 ein @Identity-Feld (RoomCode) — ein
                           Prozess hält seither mehrere Watchpartys

    -- Entities (@Entity, ADR-027; Identität über @Identity-Feld) --
    Round.java             Eine Runde: Wette, closesAt, eingefrorener
                           Teilnehmerkreis, Tipps, Ergebnis. Mutatoren
                           paket-privat — das ist die Aggregatgrenze
    Player.java            Teilnehmer, Verpasste-Runden-Zähler (8.1) und die
                           Einsatzregel stakeFor (6/8.3)

    -- Value Objects (@ValueObject, ADR-027; Identität über die Werte) --
    PlayerId/RoundId/BetId/OutcomeId   Identitäten, gegeneinander nicht
                           austauschbar — Vertauschen ist ein Kompilierfehler
    RoomCode.java           Identität einer Watchparty (ADR-033). Anders als
                           die *Id-Typen für Menschen lesbar und vorlesbar:
                           vier Zeichen aus 0-9/A-Z ohne O/I/L, dafür fällt
                           die Eingabe auf die passende Ziffer (parse)
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
                           Dienst, deshalb hier und nicht in domain/service.
                           Traegt bewusst keinen DDD-Baustein (ADR-027,
                           explizite Ausnahme in ArchitectureTest)
    Params.java            Startguthaben, Mindesteinsatz, Strafe (3.1)
    RoomSnapshot.java      Bewusst OHNE Value Objects UND ohne DDD-Baustein
                           (ADR-027, explizite Ausnahme): das Dateiformat für
                           die Platte (ADR-023), unabhängig vom Modell.
                           Room.toSnapshot/fromSnapshot rechnet um
  domain/service/
    Settlement.java        Domain Service (@Service, ADR-027): Abrechnung
                           als reine Funktion (Anforderung 7/8), gehört zu
                           keiner einzelnen Entity. Liefert Deltas, Pool und
                           Annullierung als Result. Zustandslosigkeit ist
                           eine geprüfte Regel, nicht nur eine Behauptung im
                           Javadoc

  domain/model/league/     Eigener Zweig für das Tippspiel (ADR-034,
                           Feature 005) — importiert nichts von oben und wird
                           von dort auch nicht importiert (ArchitectureTest).
                           Stufe 1 (Wertung) und Stufe 2 (Persistenz):
    GameScore.java         Value Object: ein Ergebnis (Heim/Gast-Punkte),
                           trägt tendency() und margin() — Tipp und
                           Endergebnis haben dieselbe Form
    Tendency.java           Value Object (Enum): HEIM/GAST/UNENTSCHIEDEN
    ScoreBucket.java        Value Object (Enum): die vier Abstands-Eimer
                           (13.5-c) samt Grenzen als of(margin)
    LeaguePoints.java       Value Object: Wertungspunkte — ausdrücklich nicht
                           Points, eine Liga zahlt keinen Pool aus
    Account.java            Aggregate Root: das Konto eines Tippers. Bislang
                           nur Datenhaltung (register/of) — Anmeldefluss und
                           Löschen kommen mit Stufe 3
    AccountId.java           Identität, UUID-basiert (anders als RoomCode
                           nie vorzulesen)
    EmailAddress.java        Value Object: Format, Normalisierung
                           (Kleinschreibung)
    DisplayName.java         Value Object — 1..20 Zeichen wie PlayerName,
                           aber eigenständig implementiert: ein Anzeigename
                           ist kein Spielername
  domain/service/league/
    Scoring.java            Domain Service: (Ergebnistipp, Endergebnis) ->
                           LeaguePoints, reine Funktion wie Settlement,
                           höchste erreichte Stufe zählt (13.5, ADR-038).
                           Mutation Score 100 %
  application/             Orchestrierung. Kennt die Domäne und die Ports,
                           sonst nichts — insbesondere kein Spring.
    RoomActor.java         Eventloop und Zustandsautomat (ADR-020) für alle
                           Watchpartys zusammen (ADR-033, ein gemeinsamer
                           Loop statt eines je Raum): OPEN_BET, PLACE_PICK,
                           CLOSE_BET, RESOLVE, ANNUL, RESET, Auto-Close,
                           Laden aller Snapshots beim Start, wiederkehrender
                           Aufräum-Sweep nach sechs Stunden Inaktivität
                           (1-j). Hält die Zuordnung Code -> Watchparty und
                           Sitzung -> Watchparty/Spieler
    RoomView.java          Projektion Raumzustand -> Nachricht, rein lesend;
                           hier hängt Invariante 4 (nur der Zähler in OPEN)
    message/Messages.java  Nachrichten Server -> Client (STATE, YOUR_PICK, ...)
    port/in/RoomCommands   Was von außen ausgelöst werden kann. Spricht
                           Sitzungs-IDs, nie Verbindungsobjekte
    port/out/              ClientGateway (an die Clients), SnapshotRepository
                           (auf Platte), Scheduler (verzögerte Ausführung)
  application/league/      Eigener Zweig für das Tippspiel (ADR-034) — kennt
                           keinen Typ aus application/RoomActor & Co. und
                           umgekehrt (ArchitectureTest, seit Stufe 2 auch auf
                           dem Anwendungsring geprüft, nicht nur der Domäne)
    port/out/AccountRepository   Ausgangs-Port für Konten, ohne die
                           Nicht-blockierend-Zusicherung von
                           SnapshotRepository: die Liga läuft auf
                           Request-Threads, nicht auf dem Raum-Thread
  adapter/in/ws/
    GameWebSocketHandler   Frames -> Kommandos, ändert selbst nichts
    WebSocketClientGateway Hält die Verbindungen, serialisiert nach JSON
    ClientSession.java     Verbindung mit eigener Ausgangs-Queue (ADR-012)
    WebSocketConfig.java   Registriert den Handler auf /ws
  adapter/out/file/
    SnapshotStore.java     Schreiben/Lesen auf Platte, eigener Thread; seit
                           ADR-033 ein Verzeichnis, eine Datei je Watchparty
  adapter/out/time/
    ScheduledExecutorScheduler
  adapter/out/db/          Postgres-Adapter des Tippspiels (ADR-035),
                           Flyway-Migrationen unter
                           src/main/resources/db/league/migration
    AccountRepositoryJdbc  AccountRepository über NamedParameterJdbcTemplate,
                           save() ein Upsert über die Konto-ID
  config/                  Sämtliche Spring-Beans: RoomConfig verdrahtet den
                           Actor, TimeConfig Uhr und Scheduler, SnapshotConfig
                           den Pfad watchparty.snapshot.path, WebConfig
                           leitet /join/{code} auf index.html weiter (1-l)
  config/league/
    LeagueDatabaseConfig    DataSource (Hikari) + Flyway-Migration von Hand,
                           bewusst ohne Spring-Boot-Autoconfiguration
                           (in WatchpartyApplication ausgeschaltet); fehlt
                           watchparty.league.db.url, entsteht kein Bean —
                           die Live-Wetten starten trotzdem (Kriterium 37)
frontend/src/
  useRoom.js               Verbindung, Reconnect, Token je Watchparty-Code
                           (ADR-033), Uhren-Offset
  App.jsx                  Beitrittsformular mit optionalem Code-Feld,
                           /join/CODE-Vorbefüllung, ständige Code-Anzeige,
                           Phasen-Ansichten: Tippen, Countdown, Aufdeckung,
                           Ergebnis, Leaderboard
  Guide.jsx                Kurzanleitung als Overlay, baut den Wettkatalog
                           aus den Serverdaten auf
docs/                      Anforderungen, ADRs, offene Entscheidungen,
                           Beobachtungsbogen für den Probelauf,
                           Teststrategie und Feature-Vorlage
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
- Ein neuer Domänentyp in `domain/model` bekommt sofort seinen
  jMolecules-Stereotyp (`@AggregateRoot`, `@Entity` oder `@ValueObject`,
  ADR-027) — `ArchitectureTest` schlägt sonst fehl, nicht erst beim nächsten
  Refactoring. Passt wirklich keiner (wie bei `Bets`, dem Wettkatalog), muss
  der Typ in `ArchitectureTest` explizit als Ausnahme genannt werden, nicht
  stillschweigend durchrutschen. Analog bekommt ein neuer Domänentyp sein
  JGiven-Szenario sofort, nicht erst nachträglich — derselbe Grundsatz wie
  beim Stereotyp, nur für Verhalten statt für Struktur.
- **Neue Features sind testgetrieben** (`docs/teststrategie.md`, Abschnitt
  9.1): Vor der Implementierung entsteht ein Feature-Dokument unter
  `docs/features/NNN-kurzname.md` nach der Vorlage in
  `docs/features/_vorlage.md` — Anlass, betroffene Anforderungen,
  Akzeptanzkriterien, Szenarien in Angenommen/Wenn/Dann, Kritikalität. Die
  Szenarien werden eins zu eins zu JGiven-Szenarien, bevor der
  Produktivcode dafür existiert. Das gilt für neue Features; der
  bestehende Funktionsumfang wurde einmalig als Characterization Testing
  nachgerüstet (`docs/teststrategie-umsetzung.md`, inzwischen abgeschlossen
  und gelöscht) und ist davon nicht rückwirkend betroffen.
- Entities und der Aggregate Root ändern sich nur über benannte Übergänge
  (`closeCurrentRound`, `addPick`, …), nie über einen öffentlichen Setter
  (ADR-025/ADR-027) — auch das ist eine geprüfte Regel, kein reiner Stil.
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
