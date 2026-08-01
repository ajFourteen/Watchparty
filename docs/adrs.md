# Architecture Decision Records

Format: Kontext → Entscheidung → Konsequenzen. Status ist **Akzeptiert**
(festgelegt) oder **Vorgeschlagen** (empfohlen, noch nicht bestätigt).

| # | Entscheidung | Status |
|---|---|---|
| ADR-001 | Pari-mutuel statt modellbasierter Quoten | Akzeptiert |
| ADR-002 | Web-App im Handy-Browser statt native App | Akzeptiert |
| ADR-003 | Zentral gehosteter Server als alleinige Autorität | Akzeptiert |
| ADR-004 | State im Arbeitsspeicher, keine Persistenz/DB | Akzeptiert |
| ADR-005 | Genau eine Server-Instanz, kein horizontales Skalieren | Akzeptiert |
| ADR-006 | WebSocket für Echtzeit-Kommunikation | Akzeptiert |
| ADR-007 | Rohe WebSocket statt STOMP | Akzeptiert |
| ADR-008 | Spring Boot mit Gradle als Server-Stack | Akzeptiert |
| ADR-009 | Nebenläufigkeit über Single-Thread-Eventloop (Actor) | Akzeptiert |
| ADR-010 | Runden-ID-Wache gegen veraltete Timer | Akzeptiert |
| ADR-011 | Bet-Validierung gegen `closesAt`-Zeitstempel | Akzeptiert |
| ADR-012 | Senden vom Raum-Thread entkoppeln | Akzeptiert |
| ADR-013 | Verdeckte Tipps über den Server erzwungen | Akzeptiert |
| ADR-014 | Reconnect über Token im localStorage | Akzeptiert |
| ADR-015 | React mit Build-Schritt als Frontend | Akzeptiert |
| ADR-016 | Erster Joiner wird Host, Rolle wandert bei Verlust | Akzeptiert (Teilfrage offen) |
| ADR-017 | Markt als Datenstruktur, nicht als Sonderfall im Code | Akzeptiert |
| ADR-018 | Fly.io als Hosting, Subdomain bei IONOS | Akzeptiert |

---

## ADR-001: Pari-mutuel statt modellbasierter Quoten

**Status:** Akzeptiert

**Kontext:** Die Auszahlung soll von der Wahrscheinlichkeit des getippten
Ergebnisses abhängen. Diese lässt sich entweder aus einem Modell/Odds-Feed
schätzen (Buchmacher) oder aus dem Wettverhalten der Gruppe ableiten
(Totalisator).

**Entscheidung:** Pari-mutuel. Alle Einsätze wandern in einen Pool, die
Gewinner teilen ihn nach Anteilen auf.

**Konsequenzen:**
- Kein Wahrscheinlichkeitsmodell, kein Odds-Feed.
- Kein virtuelles „Haus"; die Punkte-Ökonomie ist reine Umverteilung.
- „Unwahrscheinlich → mehr Punkte" ergibt sich selbst; das System ist
  selbstbalancierend (die Masse zu kopieren senkt die eigene Auszahlung).
- Bei sehr wenigen Spielern hohe Varianz. Für ein Partyspiel als Feature
  akzeptiert.

## ADR-002: Web-App im Handy-Browser statt native App

**Status:** Akzeptiert

**Kontext:** Teilnehmer sitzen vor Ort und sollen ohne Installation
mitmachen können.

**Entscheidung:** Web-App im Handy-Browser, Teilnahme über einen Link.

**Konsequenzen:**
- Niedrigste Einstiegshürde, keine App-Store-Distribution.
- Mobile Randbedingungen zu behandeln: Verbindungsabbrüche, schlafende
  Handys. Wake-Lock-API hält den Screen während einer Runde wach.

## ADR-003: Zentral gehosteter Server als alleinige Autorität

**Status:** Akzeptiert

**Kontext:** Ursprünglich sollte das Host-Gerät die Autorität sein. Mit dem
Wunsch nach zentralem Hosting verschiebt sie sich auf den Server.

**Entscheidung:** Der Server ist die einzige Quelle der Wahrheit. Handys sind
Renderer: Sie zeigen an, was der Server sendet, und schicken Absichten
zurück. „Host" ist nur eine Rolle mit Steuerknöpfen.

**Konsequenzen:**
- Eine maßgebliche Uhr auf dem Server steuert den 15-Sekunden-Timer; kein
  Drift zwischen Geräten.
- Reconnect ist billig: Der Client zieht den kompletten Zustand neu.
- Spiellogik konzentriert sich serverseitig und ist an einer Stelle testbar.

## ADR-004: State im Arbeitsspeicher, keine Persistenz/DB

**Status:** Akzeptiert

**Kontext:** Persistenz über Spielabende hinweg ist nicht gefordert.

**Entscheidung:** Gesamter Raumzustand im Arbeitsspeicher einer Instanz,
keine Datenbank.

**Konsequenzen:**
- Deutlich weniger bewegliche Teile.
- Ein Server-Neustart mitten im Abend verliert die laufende Runde. Bewusst
  akzeptiert.

## ADR-005: Genau eine Server-Instanz

**Status:** Akzeptiert

**Kontext:** Der Zustand liegt im Speicher genau eines Prozesses. Viele PaaS
skalieren per Default horizontal.

**Entscheidung:** Deployment mit fest einer Instanz (min = max = 1), kein
Autoscaling.

**Konsequenzen:**
- Zwingend für Korrektheit: Zwei Instanzen wären zwei getrennte Räume.
- Muss in der Hosting-Konfiguration explizit festgenagelt werden.

## ADR-006: WebSocket für Echtzeit-Kommunikation

**Status:** Akzeptiert

**Kontext:** Countdown, Tipp-Zähler, Aufdeckung und Ergebnisse müssen live an
alle Handys. Polling wäre für den 15-Sekunden-Takt zu grob.

**Entscheidung:** Persistente WebSocket-Verbindung pro Client.

**Konsequenzen:**
- Server kann jederzeit broadcasten.
- Verbindungsabbrüche müssen behandelt werden (ADR-014).
- Beim Deployen prüfen, ob die Plattform WebSockets durchreicht und wie lang
  ihr Idle-Timeout ist.

## ADR-007: Rohe WebSocket statt STOMP

**Status:** Akzeptiert

**Kontext:** STOMP ist Spring-idiomatisch und bringt Topic-Broadcasting mit,
zahlt sich aber erst bei mehreren Topics aus. Hier gibt es genau einen Raum.

**Entscheidung:** Rohe WebSocket mit eigener Session-Liste.

**Konsequenzen:**
- Weniger Framework-Magie, direktere Kontrolle über Nachrichtenfluss.
- Broadcast-Logik selbst geschrieben — bei einem Raum trivial.
- Bei späterem Mehrraum-Betrieb neu zu bewerten.

## ADR-008: Spring Boot mit Gradle als Server-Stack

**Status:** Akzeptiert

**Entscheidung:** Spring Boot 3.3 auf Java 21, Gradle mit Kotlin DSL,
deploybar als einzelner Container.

**Konsequenzen:**
- Passt zum vorhandenen Java-Know-how.
- Deckt Asset-Auslieferung und WebSocket in einem Prozess ab.

## ADR-009: Nebenläufigkeit über Single-Thread-Eventloop (Actor)

**Status:** Akzeptiert

**Kontext:** WebSocket-Handler laufen nebenläufig über verschiedene Sessions.
Es gibt mehrere Rennen: gleichzeitige Tipps kurz vor Schluss, Auto-Close vs.
noch fliegender Tipp, manueller vs. automatischer Close, Join/Disconnect
während einer laufenden Runde.

**Entscheidung:** Alle Kommandos landen auf einer Queue und werden von einem
einzigen dedizierten Thread seriell abgearbeitet. WebSocket-Threads reihen
nur ein. **Alles**, was Raum- oder Session-Zustand ändert — auch Join und
Disconnect —, geht durch dieselbe Queue.

**Konsequenzen:**
- Die gesamte Raum-Logik ist gewöhnlicher Single-Thread-Code: kein
  `synchronized`, kein `volatile`, keine Concurrent-Collections. „Ein Lock,
  das man nicht vergessen kann."
- Timer-vs-Tipp und Doppel-Close lösen sich über die Queue-Reihenfolge auf,
  deterministisch. Check-and-Act ist per Konstruktion atomar.
- Deterministisch testbar: Ereignis-Sequenzen lassen sich durchspielen.
- Erfordert eine bewusste Ausnahme beim Senden (ADR-012).

## ADR-010: Runden-ID-Wache gegen veraltete Timer

**Status:** Akzeptiert

**Kontext:** Schließt der Host manuell früh, feuert der geplante
Auto-Close-Task trotzdem später und könnte die nächste, bereits offene Runde
schließen. Cancellation reicht nicht, weil der Task schon enqueued sein kann.

**Entscheidung:** Jede Runde bekommt eine ID. Das `CLOSE`-Ereignis trägt sie
mit; der Handler ignoriert jedes `CLOSE` mit nicht passender ID.
Cancellation ist nur Optimierung.

**Konsequenzen:**
- Kein versehentliches Schließen einer Folgerunde.
- ID-Vergabe pro Runde und ID-Prüfung im Close-Handler nötig.

## ADR-011: Bet-Validierung gegen `closesAt`-Zeitstempel

**Status:** Akzeptiert

**Kontext:** Die Gültigkeit eines Tipps sollte nicht davon abhängen, ob der
Timer-Task exakt pünktlich feuert (GC-Pausen o. Ä.).

**Entscheidung:** Ein Tipp ist gültig, wenn beim Abarbeiten
`serverNow < closesAt` gilt. Der geplante Close-Task ist nur der Auslöser für
den Zustandswechsel und die Aufdeckung.

**Konsequenzen:**
- Auch ein verspäteter Close-Task lehnt danach eintreffende Tipps korrekt ab.

## ADR-012: Senden vom Raum-Thread entkoppeln

**Status:** Akzeptiert

**Kontext:** Der Eventloop darf nicht blockieren. Ein WebSocket-Send kann
blockieren, wenn der Sendepuffer voll ist — typisch bei einem langsamen oder
eingeschlafenen Handy, also genau dem vorliegenden Umfeld.

**Entscheidung:** Der Raum-Thread berechnet nur Zustand und Nachrichten. Das
Schreiben läuft über eine Ausgangs-Queue pro Session auf einem eigenen Pool.

**Konsequenzen:**
- Ein totes Handy kann das Spiel nicht stallen.
- Bewusste, klar abgegrenzte Ausnahme vom „ein Thread macht alles"-Prinzip.
- Pro Session schreibt immer nur ein Thread; Reihenfolge bleibt erhalten.

## ADR-013: Verdeckte Tipps über den Server erzwungen

**Status:** Akzeptiert

**Kontext:** Tipps sind verdeckt, solange das Fenster offen ist. Da jeder die
WebSocket-Frames im Browser mitlesen kann, ist Verdecktheit eine Anforderung
an das, was über die Leitung geht — nicht an die Darstellung.

**Entscheidung:** Während das Fenster offen ist, sendet der Server nur den
Zähler (`k von N`). Die Aufdeckung aller Tipps erfolgt beim Schließen, die
Punkte-Verrechnung getrennt davon erst beim Auflösen.

**Konsequenzen:**
- Drei klar getrennte Ereignisse über die Leitung: Zähler (offen) →
  Aufdeckung (Schluss) → Ergebnis (Auflösen).

## ADR-014: Reconnect über Token im localStorage

**Status:** Akzeptiert

**Kontext:** Handys schlafen ein, Verbindungen brechen weg. Der Spieler muss
ohne Login demselben Konto zugeordnet werden.

**Entscheidung:** Beim Join vergibt der Server ein Token, das der Client im
localStorage ablegt und beim Reconnect mitschickt.

**Konsequenzen:**
- Nahtloser Wiedereinstieg ohne Accounts.
- Gilt nur innerhalb der Lebensdauer der Instanz (ADR-004).
- Zum Testen mehrerer Spieler auf einem Rechner: getrennte Browser-Profile
  oder Inkognito-Fenster, da Tabs sich den localStorage teilen.

## ADR-015: React mit Build-Schritt als Frontend

**Status:** Akzeptiert

**Kontext:** Alternative wäre pures HTML/JS ohne Build gewesen. Die UI ist
klein, wird aber zustandsbehaftet (Countdown, Wettfenster, Aufdeckung,
Leaderboard).

**Entscheidung:** React 18 mit Vite. Der Build wird als statische Ressource
ins Jar gepackt; im Dev-Modus läuft Vite separat und proxyt `/ws`.

**Konsequenzen:**
- Zwei Toolchains (Node und Gradle) statt einer.
- Die WebSocket-URL bleibt dank Proxy in Dev und Prod identisch.
- `-PskipFrontend` überspringt den Frontend-Build, wenn er separat läuft
  (Docker-Multi-Stage).

## ADR-016: Erster Joiner wird Host, Rolle wandert bei Verlust

**Status:** Akzeptiert (Teilfrage offen)

**Kontext:** Der Host braucht Steuerknöpfe. Alternativen wären eine eigene
Host-URL oder ein Kennwort beim Join.

**Entscheidung:** Der erste Joiner wird Host. Verliert der Host die
Verbindung, wandert die Rolle automatisch an den nächsten verbundenen
Spieler — sonst wäre der Raum steuerlos.

**Konsequenzen:**
- Kein zusätzlicher Einstiegsschritt, passt zu „so einfach wie möglich".
- **Offen:** Kommt der ursprüngliche Host per Token zurück, ist er derzeit
  nicht mehr Host. Ob die Rolle am Token kleben soll, ist noch zu klären.

## ADR-017: Markt als Datenstruktur, nicht als Sonderfall im Code

**Status:** Akzeptiert

**Kontext:** Zum Start gibt es nur „Ausgang des nächsten Drives", später
sollen weitere feste Märkte dazukommen.

**Entscheidung:** Ein Markt ist fachlich eine Frage plus eine Liste von
Optionen plus eine Auflösung. Der Drive-Ausgang ist nur die erste Instanz
davon, kein eingebauter Spezialfall.

**Konsequenzen:**
- Weitere Märkte sind später ein neuer Datensatz, kein Umbau der Wett-Engine.
- Kostet heute kaum etwas, spart den Bruch beim zweiten Markt.

## ADR-018: Fly.io als Hosting, Subdomain bei IONOS

**Status:** Akzeptiert

**Kontext:** Die App ist ein Container mit einem Port. Aus den vorhandenen
ADRs ergeben sich vier harte Anforderungen an die Plattform: fest eine
Instanz (ADR-005), WebSockets müssen durchgereicht werden (ADR-006), TLS ist
Pflicht, weil die Wake-Lock-API einen Secure Context verlangt (ADR-002), und
— am wichtigsten — der Zustand lebt nur im Arbeitsspeicher (ADR-004). Damit
ist die **Neustart-Politik der Plattform das entscheidende Kriterium, nicht
der Preis**: Ein Neustart mitten im Spiel kostet nicht die laufende Runde,
sondern die Punktestände des ganzen Abends. Die Tokens im localStorage
zeigen danach ins Leere.

**Entscheidung:** Fly.io, Region `fra`, `shared-cpu-1x` mit 512 MB, fest eine
Maschine. Die Konfiguration steht in `fly.toml`. Erreichbar unter
`watchparty.fourteen-it.de`, ein CNAME bei IONOS auf
`watchparty-fourteen.fly.dev`; das Zertifikat verwaltet Fly.

Verworfene Alternativen:

- **Scale-to-Zero** (naheliegend bei drei Stunden Nutzung pro Woche): Fly
  stoppt eine Maschine, sobald keine Verbindung mehr offen ist. In der
  Halbzeitpause stecken alle das Handy ein, iOS suspendiert die Tabs, alle
  WebSockets fallen weg — die Maschine hielte an und der Raumzustand wäre
  weg. Die Ersparnis läge unter 3 €/Monat.
- **Heroku:** tägliches Dyno-Cycling. Garantierter Punkteverlust irgendwann
  mitten am Abend.
- **Cloud Run:** CPU-Throttling außerhalb von Requests kollidiert mit einem
  Eventloop, der eigene Timer fährt (ADR-009/011). Mit CPU-always-on
  behebbar, aber dann ohne Preisvorteil.
- **Render Free:** Spin-down nach 15 Minuten Leerlauf, ~50 s Kaltstart für
  den ersten Joiner.
- **Eigener VPS (Hetzner):** gleichwertig und gleich teuer, Neustarts nur
  selbst ausgelöst. Verworfen zugunsten des geringeren Betriebsaufwands —
  kein Reverse Proxy, keine OS-Updates, kein Firewall-Regelwerk.

**Konsequenzen:**
- ADR-005 ist in `fly.toml` festgenagelt statt nur im README erwähnt.
- **`fly.toml` allein genügt dafür aber nicht.** `fly deploy` legt beim
  ersten Deploy eigenmächtig eine zweite Maschine für High Availability an,
  auch bei `min_machines_running = 1`. Beim ersten Deploy dieser App ist das
  eingetreten und wurde mit `fly scale count 1` korrigiert. Deployen darum
  nur mit `--ha=false`, danach `fly machines list` prüfen. Was für Fly ein
  Feature ist, ist hier ein Korrektheitsfehler.
- Fly migriert Maschinen bei Host-Wartung, ohne dass der Zeitpunkt steuerbar
  wäre. Selten, aber das verbleibende Restrisiko dieser Wahl.
- Deployen ist ein Neustart. **Nicht am Spieltag deployen** ist damit eine
  Betriebsregel, keine Stilfrage.
- Die WebSocket-URL leitet sich aus `window.location` ab; die Domain ist
  reine DNS- und Zertifikatsarbeit ohne Codeänderung.
- Der Proxy von Cloudflare o. Ä. bleibt außen vor (bei IONOS ohnehin nicht
  im Weg): eine zusätzliche Schicht brächte ein weiteres Idle-Timeout und
  eine zweite Zertifikatskette, ohne Nutzen für ein paar Handys im selben
  Raum.
