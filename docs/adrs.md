# Architecture Decision Records

Format: Kontext → Entscheidung → Konsequenzen. Status ist **Akzeptiert**
(festgelegt) oder **Vorgeschlagen** (empfohlen, noch nicht bestätigt).

| # | Entscheidung | Status |
|---|---|---|
| ADR-001 | Pari-mutuel statt modellbasierter Quoten | Akzeptiert |
| ADR-002 | Web-App im Handy-Browser statt native App | Akzeptiert |
| ADR-003 | Zentral gehosteter Server als alleinige Autorität | Akzeptiert |
| ADR-004 | State im Arbeitsspeicher, keine Persistenz/DB | Akzeptiert (ergänzt durch ADR-023) |
| ADR-005 | Genau eine Server-Instanz, kein horizontales Skalieren | Akzeptiert |
| ADR-006 | WebSocket für Echtzeit-Kommunikation | Akzeptiert |
| ADR-007 | Rohe WebSocket statt STOMP | Akzeptiert |
| ADR-008 | Spring Boot mit Gradle als Server-Stack | Akzeptiert |
| ADR-009 | Nebenläufigkeit über Single-Thread-Eventloop (Actor) | Akzeptiert |
| ADR-010 | Runden-ID-Wache gegen veraltete Timer | Akzeptiert |
| ADR-011 | Tipp-Validierung gegen `closesAt`-Zeitstempel | Akzeptiert |
| ADR-012 | Senden vom Raum-Thread entkoppeln | Akzeptiert |
| ADR-013 | Verdeckte Tipps über den Server erzwungen | Akzeptiert |
| ADR-014 | Reconnect über Token im localStorage | Akzeptiert |
| ADR-015 | React mit Build-Schritt als Frontend | Akzeptiert |
| ADR-016 | Erster Joiner wird Host, Rolle wandert bei Verlust | Akzeptiert (präzisiert durch ADR-021) |
| ADR-017 | Wette als Datenstruktur, nicht als Sonderfall im Code | Akzeptiert |
| ADR-018 | Fly.io als Hosting, Subdomain bei IONOS | Akzeptiert |
| ADR-019 | Deploy automatisiert über Semantic Release | Akzeptiert |
| ADR-020 | Rundenablauf als Zustandsautomat mit eigenem RESOLVED | Akzeptiert |
| ADR-021 | Host-Rolle nach Beitrittsreihenfolge, Übergabe asymmetrisch | Akzeptiert |
| ADR-022 | „Wette" statt „Markt", Tipp heißt im Code `Pick` | Akzeptiert |
| ADR-023 | Snapshot auf Platte übersteht einen Neustart innerhalb des Abends | Akzeptiert |
| ADR-024 | Onion-Architektur mit Ringen, Ports und Adaptern | Akzeptiert |
| ADR-025 | DDD-Taktik im Domänenmodell, ArchUnit, Test Doubles statt Mockito | Akzeptiert |
| ADR-026 | JSpecify-Nullness mit NullAway durchgesetzt | Akzeptiert |
| ADR-027 | jMolecules-Stereotypen für DDD-Bausteine und Onion-Ringe | Akzeptiert |
| ADR-028 | Repo öffentlich, Default-Branch `main`, Business Source License | Akzeptiert |
| ADR-029 | Java 25 durchgehend, mit den dafür nötigen Versionssprüngen | Akzeptiert |
| ADR-030 | Teststrategie: Ebenen über JGiven-Tags, Sprachausnahme fürs Stufen-Paket | Akzeptiert |
| ADR-031 | Teststrategie: Metriken scharf gestellt — JaCoCo, Ebenen-Disjunktheit, PIT, Ausnahmenregister | Akzeptiert |
| ADR-032 | Screen Wake Lock als Best-Effort-Komfort, ohne automatisierten Test | Akzeptiert |

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
  akzeptiert — ergänzt durch ADR-023: Ein Snapshot übersteht seit
  2026-08-02 einen Neustart innerhalb desselben Abends, ohne dass dieser
  Satz falsch würde. Persistenz über Spielabende hinweg bleibt weiterhin
  nicht gefordert.

## ADR-005: Genau eine Server-Instanz

**Status:** Akzeptiert

**Kontext:** Der Zustand liegt im Speicher genau eines Prozesses. Viele PaaS
skalieren per Default horizontal.

**Entscheidung:** Deployment mit fest einer Instanz (min = max = 1), kein
Autoscaling.

**Konsequenzen:**
- Zwingend für Korrektheit: Zwei Instanzen wären zwei getrennte Mengen von
  Watchpartys, mit Sitzungen, die zufällig auf der falschen landen (seit
  ADR-033: eine Instanz hält viele Watchpartys, nicht mehr genau einen Raum
  — an dieser Konsequenz ändert das nichts, nur an ihrer Formulierung).
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

## ADR-011: Tipp-Validierung gegen `closesAt`-Zeitstempel

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
- Die ursprünglich offene Teilfrage — bekommt ein per Token zurückkehrender
  Host seine Rolle wieder? — ist mit ADR-021 beantwortet.

## ADR-017: Wette als Datenstruktur, nicht als Sonderfall im Code

**Status:** Akzeptiert

**Kontext:** Zum Start gibt es nur „Ausgang des nächsten Drives", später
sollen weitere feste Wetten dazukommen.

**Entscheidung:** Eine Wette ist fachlich eine Frage plus eine Liste von
Optionen plus eine Auflösung. Der Drive-Ausgang ist nur die erste Instanz
davon, kein eingebauter Spezialfall.

**Konsequenzen:**
- Weitere Wetten sind später ein neuer Datensatz, kein Umbau der Wett-Engine.
- Kostet heute kaum etwas, spart den Bruch bei der zweiten Wette.
- Eingelöst mit dem Katalog aus Anforderung 4: Die Wetten nach dem
  Drive-Ausgang waren reine Datensätze in `Bets`, plus die Auswahl beim
  Öffnen. Die Wett-Engine blieb unberührt.

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

## ADR-019: Deploy automatisiert über Semantic Release

**Status:** Akzeptiert

**Kontext:** Bislang war Deployen ein manuelles `fly deploy --ha=false` (ADR-
018). Das ist fehleranfällig, wenn es nicht regelmäßig gemacht wird, und
lässt Versionsstand und tatsächlich laufenden Code auseinanderlaufen. Ein
klassisches „Deploy bei jedem Push auf master" stünde aber im Widerspruch
zur Deploy-Regel aus ADR-018: Ein Deploy ist ein Neustart, ein Neustart
kostet nach ADR-004 den kompletten Raumzustand, und ein Merge am Spielabend
darf das nicht ungefragt auslösen.

**Entscheidung:** Semantic Release wertet Commit-Messages auf `master` aus
(Konvention: `fix:`, `feat:`, `feat!:`/`BREAKING CHANGE:` für Patch/Minor/
Major). Nur wenn ein Commit dieser Art dabei ist, entsteht ein Release —
Git-Tag, `CHANGELOG.md`, GitHub Release. Nur ein tatsächlich veröffentlichter
Release löst per GitHub Actions den Fly-Deploy aus
(`.github/workflows/release.yml`), weiterhin mit `--ha=false` (ADR-018).

**Konsequenzen:**
- Commits mit `chore:`, `docs:`, `refactor:` (ohne Verhaltensänderung) usw.
  landen auf `master`, ohne einen Deploy auszulösen. Das mildert das
  „Push = sofort live"-Risiko, hebt es aber nicht auf: Ein `fix:`- oder
  `feat:`-Commit deployed weiterhin sofort bei Push auf `master`. Die
  Deploy-Disziplin aus ADR-018 verschiebt sich damit auf den Zeitpunkt des
  Merges nach `master`, nicht auf einen separaten manuellen Schritt.
  **Nicht am Spieltag auf `master` mergen**, wenn der Commit release-
  relevant ist.
- Commit-Konvention ist ab jetzt verbindlich für den Typ-Präfix (englisch,
  z. B. `fix:`), der Rest der Nachricht bleibt wie gehabt deutsch.
- `FLY_API_TOKEN` (App-gescoped über `fly tokens create deploy -a
  watchparty-fourteen`) liegt als GitHub-Actions-Secret im Repo, nicht im
  Code. `GITHUB_TOKEN` für Semantic Release selbst kommt automatisch von
  Actions. Auf ein Jahr befristet (gesetzt 2026-08-01), Erneuerung ist
  manuell und ohne automatische Erinnerung — dokumentiert im README.
- Der manuelle Weg (`fly deploy --ha=false`) bleibt für Notfälle bestehen,
  z. B. wenn ein Fix ohne Versionsbump sofort raus muss.
- Kein `@semantic-release/npm`-Plugin: Das Projekt wird nicht auf npm
  veröffentlicht, Semantic Release dient hier ausschließlich als
  Release-/Deploy-Trigger.
- Automatischer Deploy ohne Rollback-Weg wäre der Fehler, der erst am
  Spielabend auffällt. `fly.toml` bekommt deshalb einen HTTP-Health-Check
  (fängt Prozessabsturz/OOM/hängenden Server ab, nicht spezifisch einen
  blockierten Raum-Thread — der bedient die Queue unabhängig von der
  statischen Auslieferung, siehe ADR-009). Rollback per `fly deploy --image
  <alte ImageRef>` ist im README dokumentiert. Ein Rollback ist ebenfalls
  ein Neustart und kostet nach ADR-004 den Raumzustand — er macht den
  Fehler nicht ungeschehen, nur schneller behoben.

## ADR-020: Rundenablauf als Zustandsautomat mit eigenem RESOLVED

**Status:** Akzeptiert

**Kontext:** Die Richtung `IDLE → OPEN → CLOSED → RESOLVED → IDLE` stand fest,
die erlaubten Ereignisse je Zustand aber nicht. Ohne diese Tabelle bleibt für
jedes Ereignis offen, ob es in einem fremden Zustand ignoriert wird, einen
Fehler auslöst oder gar nicht erst gesendet werden darf — und das JSON-Schema
lässt sich nicht ableiten.

**Entscheidung:** Eine geöffnete Wette ist genau eine Runde mit monoton steigender
`roundId`. Erlaubt sind:

| Ereignis | IDLE | OPEN | CLOSED | RESOLVED |
|---|---|---|---|---|
| `OPEN_MARKET` (Host) | → OPEN | Fehler | Fehler | → OPEN |
| `PLACE_BET` (Spieler) | Fehler | annehmen, wenn `now < closesAt` | Fehler | Fehler |
| `CLOSE_MARKET` (Host) | Fehler | → CLOSED | still ignorieren | Fehler |
| `AUTO_CLOSE(roundId)` | ignorieren | → CLOSED, wenn ID passt | ignorieren | ignorieren |
| `RESOLVE(outcome)` (Host) | Fehler | Fehler | → RESOLVED, verrechnen | Fehler |
| Join / Disconnect | in jedem Zustand erlaubt | | | |

`RESOLVED` ist ein eigener Zustand und nicht einfach `IDLE`: Das Ergebnis der
letzten Runde bleibt stehen, bis der Host die nächste öffnet. Der Übergang
`RESOLVED → IDLE` aus dem Diagramm passiert damit implizit beim nächsten
`OPEN_MARKET`.

**Konsequenzen:**
- Das Nachrichtenschema folgt der Tabelle: Der Inhalt von `STATE` hängt an der
  Phase — in OPEN nur der Zähler (ADR-013), in CLOSED die aufgedeckten Tipps,
  in RESOLVED zusätzlich Ergebnis und Deltas.
- Ein doppeltes Schließen ist kein Fehlerfall, sondern wird still ignoriert:
  Manueller und automatischer Schluss treffen sich regelmäßig, und die
  Reihenfolge in der Queue entscheidet (ADR-009/010).
- Die Ereignisse lassen sich als Sequenzen durchspielen und damit
  deterministisch testen — vorausgesetzt, Uhr und Scheduler sind im
  `RoomActor` von außen setzbar. Ohne das ist weder ADR-010 noch ADR-011
  prüfbar.

## ADR-021: Host-Rolle nach Beitrittsreihenfolge, Übergabe asymmetrisch

**Status:** Akzeptiert

**Kontext:** ADR-016 ließ offen, ob ein per Token zurückkehrender Host seine
Rolle wiederbekommt. Die naheliegende Formulierung „die Rolle klebt am Token
des ursprünglichen Hosts" kennt aber nur zwei Beteiligte und beantwortet den
realistischen Fall nicht: Auch der Vertreter sperrt sein Handy, und er kann
zurückkommen, während der ursprüngliche Host noch weg ist.

**Entscheidung:** **Host ist immer der am frühesten beigetretene verbundene
Spieler.** Damit gilt „erster Joiner wird Host" nicht nur einmal beim Start,
sondern dauerhaft, und jede Kombination aus Weggehen und Zurückkommen ist
abgedeckt, ohne dass es ein gesondertes Original-Host-Token bräuchte.

Die Übergabe ist dabei asymmetrisch:

- **Verlieren wirkt sofort, in jeder Phase.** Sonst wäre der Raum mitten im
  offenen Fenster steuerlos — genau dann, wenn jemand schließen können muss.
- **Zurückholen wirkt erst in IDLE oder RESOLVED.** Kehrt ein höherrangiger
  Spieler während OPEN oder CLOSED zurück, wird die Übergabe vorgemerkt und
  beim Erreichen von RESOLVED ausgeführt. Sonst rutschen dem Vertreter die
  Steuerknöpfe mitten in einer laufenden Runde weg.

**Konsequenzen:**
- `Room.reassignHostIfNeeded()` sucht bereits den ersten verbundenen Spieler
  in Einfügereihenfolge. Es ändert sich im Wesentlichen nur, wann die Methode
  läuft: künftig bei jeder Änderung der Verbundenheit statt nur beim Wegfall
  des aktuellen Hosts — plus die Phasensperre für den Aufwärts-Fall.
- Ein nach 8.1 pausierter Spieler ist per Definition getrennt und damit
  ohnehin nicht wählbar; beide Regeln kommen sich nicht ins Gehege.
- Die Rolle kann über den Abend zwischen den Runden mehrfach wandern, wenn
  Handys ein- und aufwachen. Sie landet dabei immer bei dem, der am längsten
  dabei ist — in der Praxis der, der die Fernbedienung hat. Das notierte
  Wake-Lock würde das zusätzlich beruhigen.
- Wer den localStorage leert und neu beitritt, rutscht ans Ende der Reihe.
  Das ist hinnehmbar und für den Notfall sogar nützlich.

## ADR-022: „Wette" statt „Markt", Tipp heißt im Code `Pick`

**Status:** Akzeptiert

**Kontext:** „Markt" stammt aus der Buchmacher-Welt und war von Anfang an ein
Fremdkörper: Es gibt hier keinen Buchmacher, keine Quoten und nichts, was
gehandelt wird (ADR-001). Am Tisch sagt niemand „öffne den Markt". Mit dem
zweiten bis fünften Eintrag im Katalog wurde der Begriff außerdem sichtbarer
— er steht jetzt in einer Auswahlliste, nicht nur in einem einzigen Knopf.

**Entscheidung:** Der Fachbegriff ist **Wette**: die Frage, auf die getippt
wird. Was ein Spieler abgibt, bleibt der **Tipp**.

Im Code heißt die Wette `Bet` und der Tipp `Pick`. Die naheliegende Variante
— `Bet` bleibt der Tipp, die Wette wird `Wager` — wurde verworfen: `Wager`
und `Bet` sind im Englischen nahezu synonym, die Unterscheidung müsste man
sich merken statt sie zu lesen. `Pick` ist im Football für genau diese Sache
gebräuchlich.

| Deutsch | Code |
|---|---|
| Wette | `Bet` (vorher `Market`) |
| Tipp | `Pick` (vorher `Bet`) |
| Einsatz | `stake` |
| Ausgang | `Outcome` |

**Konsequenzen:**
- Bruch im Protokoll: `OPEN_MARKET`/`CLOSE_MARKET`/`PLACE_BET`/`YOUR_BET`
  heißen `OPEN_BET`/`CLOSE_BET`/`PLACE_PICK`/`YOUR_PICK`, im STATE
  `market`→`bet`, `betCount`→`pickCount`, `revealedBets`→`revealedPicks`.
  Folgenlos, weil es keine Persistenz gibt (ADR-004), genau eine Instanz
  läuft (ADR-005) und das Frontend aus demselben Jar kommt (ADR-015) — ein
  Deploy tauscht beide Seiten gleichzeitig.
- Ein alter Client im Browser-Cache spricht nach dem Deploy die alte Sprache.
  Er bekommt auf jede Aktion einen Fehler, ein Neuladen behebt es. Für ein
  Partyspiel ohne laufende Sitzungen zwischen Abenden hinnehmbar.
- `OPEN_BET` ohne `betId` öffnet weiterhin den Drive-Ausgang. Das hält den
  häufigsten Fall billig und macht das Feld optional statt zwingend.

## ADR-023: Snapshot auf Platte übersteht einen Neustart innerhalb des Abends

**Status:** Akzeptiert

**Kontext:** ADR-004 nimmt den Verlust des Raumzustands bei einem Neustart
bewusst in Kauf, weil Persistenz über Spielabende hinweg nicht gefordert
ist. Das galt in der Praxis auch für den Neustart selbst: Ein Deploy, ein
OOM-Kill oder eine Fly-Wartung mitten im Abend kostete Punkte, Namen und
Tokens des ganzen Abends, nicht nur die laufende Runde. Die einzige
Gegenmaßnahme war Disziplin — nicht am Spieltag nach `master` mergen
(ADR-019) —, die ausgerechnet dann bricht, wenn ein Fix am dringendsten
gebraucht wird.

**Entscheidung:** Der Raumzustand wird bei jeder Änderung als
`RoomSnapshot` auf Platte geschrieben und beim Start zurückgeladen, sofern
er nicht älter als sechs Stunden ist. Das ist ein Nachtrag zu ADR-004, kein
Widerruf: keine Datenbank, kein zusätzlicher Dienst, der Arbeitsspeicher
bleibt die maßgebliche Kopie, die Datei ist nur ein Abzug. Weiterhin keine
Persistenz über Spielabende hinweg — dafür sorgt die Verfallszeit.

Umsetzung:

- **Schreiben entkoppelt vom Raum-Thread** (analog zur Ausgangs-Queue in
  `ClientSession`, ADR-012): Auf dem Raum-Thread entsteht nur ein
  unveränderliches Snapshot-Objekt, das Schreiben läuft auf einem eigenen
  Thread in `SnapshotStore`. Verdichtet über eine `ArrayBlockingQueue` der
  Kapazität 1 — korrekt, weil `save()` ausschließlich vom Raum-Thread
  aufgerufen wird (genau ein Erzeuger). Schreiben atomar über eine
  tmp-Datei mit `fsync` und `ATOMIC_MOVE`.
- **Laden als erstes Kommando in der Actor-Queue** (`loadOnStartup`, seit
  ADR-024 als `initMethod` in `RoomConfig` statt als `@PostConstruct` am
  Actor): läuft damit auf dem Raum-Thread und ist garantiert vor dem ersten
  `JOIN` fertig, ohne Sonderfall in Invariante 1.
- **Im Zweifel leer starten.** Fehlende, kaputte oder abgelaufene Datei,
  unbekannte `schemaVersion` oder eine im aktuellen Katalog (ADR-017)
  verschwundene `betId` führen zum leeren Raum oder zum Verwerfen nur der
  Runde, nie zum Absturz. Ein Snapshot, der den Start zerschießt, wäre der
  schlimmste denkbare Ausgang — dann startet die Maschine in einer Schleife
  neu und der Abend ist endgültig vorbei.
- **`RESET` als Gegenstück.** Der Neustart war bisher implizit das
  Zurücksetzen des Raums; ohne ihn braucht der Host einen expliziten Weg.
  Anders als `ANNUL` in jeder Phase gültig und nimmt auch die Spieler mit —
  Testrunden vom Aufbau oder ein doppelt beigetretener Spieler sollen
  verschwinden können, nicht nur der Punktestand.

**Konsequenzen:**
- Ein Fly-Volume ist erforderlich, sonst ist die Datei bei jedem Deploy neu
  und leer (`fly.toml`, README). Macht ADR-005 schärfer statt es
  aufzuweichen: Ein Volume ist an eine Maschine gebunden, zwei Maschinen
  hätten jetzt nicht nur zwei Räume, sondern auch zwei Dateien.
  `--ha=false` und `fly machines list` bleiben aus demselben Grund Pflicht.
- Ehrliche Grenze: Ein Volume übersteht ein Deploy, aber nicht das Ersetzen
  der Maschine (Hardware-Ausfall, Regionswechsel). Eine deutliche
  Verbesserung, keine Garantie.
- `watchparty.snapshot.path` leer oder ungesetzt bedeutet Persistenz aus —
  Voreinstellung für lokale Entwicklung und Tests, und zugleich der
  Notausschalter, falls der Snapshot am Spielabend Ärger macht.
- Eine offene Runde, deren Fenster während des Neustarts abläuft, wird
  beim Laden schlicht geschlossen, kein eigener Annullierungsgrund. Ein
  Deploy fällt nach der Betriebsregel oben nicht in einen echten
  Spielabend, der Fall betrifft also praktisch nur Tests; sollte er doch
  jemanden zu Unrecht treffen, annulliert der Host mit dem vorhandenen
  Knopf (8.6).
- `RESET` beendet ein Spiel, es verschiebt keine Punkte — Invariante 5
  (Nullsumme) bleibt dadurch unberührt, sie gilt innerhalb eines Spiels.
- Die Betriebsregel „nicht am Spieltag deployen" (ADR-019, README) bleibt
  trotzdem bestehen. Der Snapshot macht einen dringenden Fix während der
  Halbzeit möglich, er macht einen Deploy während des laufenden Spiels
  nicht zur Routine.

---

## ADR-024: Onion-Architektur mit Ringen, Ports und Adaptern

**Status:** Akzeptiert

**Kontext:** Die Pakete waren nach Technik geschnitten (`room`, `ws`,
`protocol`). Innerhalb von `room` lagen Domäne, Orchestrierung, Zeit,
Persistenz und Spring-Verdrahtung nebeneinander. Das war bei 1.600 Zeilen
noch überschaubar, aber die Abhängigkeitsrichtung war nirgends festgelegt
und tatsächlich schon verletzt: `RoomActor` hielt `ClientSession`-Objekte
und serialisierte selbst JSON, er kannte `SnapshotStore` direkt statt einer
Abstraktion, und er trug `@Component`, `@PostConstruct` und `@PreDestroy` —
Spring stand damit mitten im Kern.

**Entscheidung:** Der Code wird in Ringe geschnitten, Abhängigkeiten zeigen
ausschließlich nach innen: `domain` (model, service) ← `application`
(inklusive `port/in` und `port/out`) ← `adapter` (in/ws, out/file,
out/time). `config` ist der Kompositionswurzel-Ring außen und die einzige
Stelle mit Spring-Beans.

Was das konkret erzwungen hat:

- **`ClientGateway` als Ausgangs-Port.** Der Actor spricht Sitzungs-IDs
  statt Verbindungsobjekte. Die Zuordnung Sitzung → Spieler war ein Feld in
  `ClientSession`, also Anwendungszustand in der Infrastruktur; sie liegt
  jetzt im Actor. `ClientSession` ist wieder reine Infrastruktur.
- **`SnapshotRepository` als Ausgangs-Port.** `RoomSnapshot` bleibt in
  `domain/model`, weil `Room.toSnapshot()` es spricht — im Adapter wäre es
  Domäne → Adapter und damit ein Ringverstoß. ADR-023 bleibt gewahrt: Dort
  ging es um Entkopplung von den Interna von `Room`, nicht um das Paket.
- **`Room` bekommt sprechende Übergänge** (`closeCurrentRound`,
  `annulCurrentRound`, `resolveCurrentRound`, `applyDeltas`, `addPick`).
  Die Mutatoren von `Round` sind paket-privat, und das ist die
  Aggregatgrenze. Vorher lag der Actor im selben Paket und kam direkt an
  `round.setPhase(...)`; nach dem Schnitt liegt er außen. Die Alternative
  wäre gewesen, `Round` zu öffnen — das hätte die Grenze aufgegeben, um
  eine Paketstruktur zu retten.
- **`Messages` und `RoomView` liegen im Anwendungsring.** `RoomView`
  erzeugt die Nachrichten; lägen sie im Adapter, zeigte `application` nach
  außen. Wichtiger noch: Invariante 4 (verdeckte Tipps) ist eine Zusage der
  Leitung, nicht der Oberfläche, und gehört deshalb nach innen.

**Konsequenzen:**
- **Die Ringregel steht als Test.** `ArchitectureTest` prüfte zunächst die
  Importzeilen der Quellen, seit ADR-025 prüft ArchUnit den Bytecode — das
  fand sofort einen Verstoß, den die Importsuche nicht sehen konnte
  (`Room` rief `Bets.byId` auf, Modell → Service statt umgekehrt). Ohne
  einen solchen Test wäre die Struktur eine Absichtserklärung, die ein
  einziger bequemer Import durchlöchert.
- **Eine bewusst zugelassene Ausnahme:** Die Nachrichtentypen tragen
  Jackson-Annotationen, liegen aber im Anwendungsring. Sie über Mixins zu
  entkoppeln wäre für fünf Records mehr Zeremonie als Gewinn; Annotationen
  sind Metadaten, serialisiert wird allein im Adapter. Der Test lässt
  Jackson deshalb genau in `application/message` zu und sonst nirgends.
- **`java.time.Clock` bekommt keinen Port.** Er ist bereits die
  Abstraktion, die ein Port nur nachbauen würde; die Fake-Uhr in den Tests
  funktioniert unverändert.
- **Die Thread-Grenze wird weniger sichtbar.** Ringe ordnen nach
  Abhängigkeitsrichtung, nicht nach Thread — Handler und Actor liegen jetzt
  weit auseinander, obwohl genau zwischen ihnen die Queue sitzt, die
  Invariante 1 trägt. Die Invarianten in `CLAUDE.md` bleiben deshalb die
  erste Anlaufstelle; die Ringregel ergänzt sie, sie ersetzt sie nicht.
- **Die Tests wurden besser, nicht nur verschoben.** Actor-Tests brauchen
  weder Mockito noch WebSockets und prüfen über einen aufzeichnenden
  Gateway, *was* bei wem ankam, statt „es wurde irgendetwas gesendet".
- Der Preis sind mehr Pakete und mehr Dateien für dieselbe Fachlichkeit.
  Bei dieser Größe ist das vertretbar, weil der Kern dadurch wirklich
  framework-frei und ohne Spring-Kontext instanziierbar ist.

---

## ADR-025: DDD-Taktik im Domänenmodell, ArchUnit, Test Doubles statt Mockito

**Status:** Akzeptiert

**Kontext:** ADR-024 hat die Architektur strategisch nach DDD geschnitten
(Ringe, Ports, Adapter). Innerhalb des Domänenrings blieb das Modell aber
primitiv: `Player`, `Round` und `Pick` trugen ihre Identitäten und Mengen als
`String` und `int`. Zwei Folgen davon waren real, nicht nur ästhetisch —
`Settlement` kappte die Strafe auf den Kontostand einmal in `Settlement` und
einmal im `RoomActor` (behoben mit dem Result-Objekt, aber die Möglichkeit
der Dopplung blieb, weil nichts sie verhinderte), und eine vertauschte
Spieler-ID/Sitzungs-ID/Token wäre erst zur Laufzeit aufgefallen. Der
Architekturtest selbst prüfte nur Importzeilen im Quelltext und konnte
Verstöße über Rückgabetypen oder Feldtypen nicht sehen.

**Entscheidung:**

1. **Taktisches DDD im Domänenmodell.** `Room` ist Aggregate Root, `Player`
   und `Round` sind Entities darin (Identität über `PlayerId`/`RoundId`,
   Mutatoren paket-privat — die Aggregatgrenze aus ADR-024 bleibt bestehen).
   Value Objects für jede Identität (`PlayerId`, `RoundId`, `BetId`,
   `OutcomeId`, `Token`) und jede Menge (`Points`, `PointsDelta`, `Share`,
   `PlayerName`). `Settlement` bleibt Domain Service: Er gehört zu keiner
   einzelnen Entity, sondern zur Runde als Ganzes.

   Die Ubiquitous Language wird dadurch nicht neu erfunden, sondern aus
   `anforderungen.md` und ADR-022 direkt übernommen: „Anteil" heißt `Share`,
   „Pool" bleibt als Feld `Points pool`, „Strafe" ist `Params.penalty()` vom
   Typ `Points`. Bezeichner bleiben englisch (Konvention), die Begriffe
   selbst sind die deutschen Fachbegriffe aus den Anforderungen.

2. **`Points` versus `PointsDelta` versus `Share` — drei Typen für dieselbe
   Ganzzahl, absichtlich.** Anforderung 7 trennt „echte Punkte" von
   „Anteile am Gewinn" ausdrücklich; das Modell erzwingt die Trennung jetzt,
   statt sie nur zu behaupten. `Points` ist nie negativ (Invariante 5 als
   Typinvariante — ein Konstruktoraufruf mit negativem Wert wirft).
   `PointsDelta` ist das Gegenteil und darf negativ sein, sonst müsste
   `Points` die Bedingung aufgeben, die es trägt.

3. **ArchUnit statt einer selbstgebauten Importprüfung.** Prüft den
   Bytecode, nicht den Quelltext — Rückgabetypen und Feldtypen zählen mit.
   Fand beim ersten Lauf sofort einen echten Verstoß (siehe Kontext).
   Zusätzlich zur Ringregel aus ADR-024: kein Spring/Jakarta in `domain`
   und `application`, kein `java.util.concurrent` in `domain` (Invariante 1
   — der Raum-Thread ist die Synchronisierung, nicht die Datenstruktur).

4. **Test Doubles von Hand statt Mockito.** Mockito steckte nur noch in
   `ClientSessionTest`. Was der Test wirklich braucht — beim Senden
   blockieren, Reihenfolge mitschreiben, Schließungen zählen — ist
   Verhalten, kein Aufrufprotokoll; `FakeWebSocketSession` implementiert das
   in klarem Code statt einer `doAnswer`-Kette. Mockito ist zusätzlich aus
   `spring-boot-starter-test` ausgeschlossen, damit es keine Absprache
   bleibt, sondern eine Regel: Ein `mock(...)` kompiliert nicht mehr.

**Konsequenzen:**
- `RoomSnapshot` bleibt bewusst ohne Value Objects — es ist das Dateiformat
  für die Platte (ADR-023), nicht das Modell. `Room.toSnapshot`/
  `fromSnapshot` rechnet um; ein Byte auf der Platte ändert sich dadurch
  nicht, `schemaVersion` bleibt unverändert.
- `Bets` liegt in `domain/model`, nicht in `domain/service`: Es ist nach
  ADR-017 eine Datenstruktur (der Wettkatalog), kein Dienst. ArchUnit
  erzwingt diese Unterscheidung im Onion — das Modell ist der innerste
  Ring, Services liegen darum herum.
- Die Nachrichtentypen (`Messages.BetView`/`OutcomeView`) serialisieren
  jetzt eigene, einfache Typen statt der Domänen-`Outcome` direkt — sonst
  hätte `OutcomeId` als verschachteltes Objekt im JSON-Frame gelegen und
  das Protokoll geändert, ohne dass das Absicht gewesen wäre.
- Ein Test in `RoomActorStateMachineTest`, der über `Player.setPoints`
  einen Kontostand direkt gesetzt hat, ließ sich nicht mehr kompilieren:
  Mutatoren sind jetzt konsequent paket-privat. Die Fälle sind entweder
  über eine echte, aufgelöste Runde nachgebaut (`RestoreTest`) oder als
  Dopplung erkannt und entfernt, weil `PlayerTest` dieselbe Regel schon
  direkt am Domänentyp prüft.
- Mehr Typen für dieselbe Fachlichkeit — bei dieser Größe vertretbar, weil
  jeder neue Typ eine Regel trägt, die vorher nur ein Kommentar war.

---

## ADR-026: JSpecify-Nullness mit NullAway durchgesetzt

**Status:** Akzeptiert

**Kontext:** ADR-025 hat das Domänenmodell auf Aggregate, Entities und Value
Objects umgestellt, aber Nullability blieb implizit — ein `Round`, dessen
`winningOutcomeId` vor RESOLVED null ist, oder ein `Room`, dessen
`hostPlayerId` vor dem ersten Beitritt null ist, waren nur in Kommentaren
dokumentiert. Ein `Player.getPoints()`, das eigentlich nie null ist, und ein
`Round.pickOf(...)`, das es sehr wohl sein kann, sahen im Code identisch
aus — nichts unterschied „garantiert vorhanden" von „kann fehlen", ausser
Disziplin. Genau das ist die Lücke, die ein falsch benutztes Domänenmodell
öffnet: eine NullPointerException an einer Stelle weit entfernt von der
eigentlichen Ursache.

**Entscheidung:** JSpecify-Annotationen (`org.jspecify:jspecify`) markieren
Nullability im Typsystem, NullAway (über Error Prone) erzwingt sie beim
Kompilieren als Fehler, nicht als Warnung.

Umsetzung:

- **`@NullMarked` auf `domain`, `application`, `adapter`, `config`** (je
  eine `package-info.java`). Jeder Verweistyp ist dort nicht-null, sofern
  nicht ausdrücklich `@Nullable`. NullAway laeuft im `OnlyNullMarked`- und
  `JSpecifyMode`-Modus: geprüft wird ausschließlich markierter Code, alles
  andere (Spring, Jackson, die JDK selbst) bleibt „legacy" und wird nicht
  mitgeprüft.
- **Testcode ist bewusst nicht `@NullMarked`.** `compileTestJava` läuft ganz
  ohne Error Prone — Tests bauen bewusst Objekte in unvollständigen
  Zwischenzuständen, das soll nicht dieselbe Disziplin tragen wie das
  Modell selbst.
- **Nur NullAway läuft, keine der übrigen Error-Prone-Prüfungen**
  (`disableAllChecks` plus gezieltes `error("NullAway")` über die getypte
  Plugin-DSL). Diese Einrichtung soll Null-Sicherheit durchsetzen, keinen
  Stilkatalog.
- **Wo eine Nicht-Null-Bedingung nicht aus dem Typ folgt, aber aus der
  Struktur** — ein `Map.get()`, dessen Schlüssel nachweislich aus derselben
  Iteration stammt wie die Map selbst (`Settlement.distributeShares`) —,
  macht ein `Objects.requireNonNull(...)` mit Kommentar die Annahme
  sichtbar, statt sie stillschweigend vorauszusetzen.
- **Eine echte implizite Vorbedingung kam dabei ans Licht:** `Room`s
  Übergänge (`closeCurrentRound`, `addPick`, `annulCurrentRound`,
  `resolveCurrentRound`) griffen auf `currentRound` zu, ohne dass irgendwo
  stand, dass der Aufrufer eine laufende Runde garantieren muss — vorher
  ein stillschweigendes Field, jetzt `@Nullable Round currentRound` samt
  `requireCurrentRound()`, das bei Verletzung eine `IllegalStateException`
  mit Erklärung wirft statt einer kontextlosen NullPointerException an
  anderer Stelle.

**Konsequenzen:**
- **Der Compiler ist die Durchsetzung, nicht eine Konvention.** Ein
  `@Nullable` an der falschen Stelle im Domänenmodell ist ein Build-Fehler.
  Gegenprobe gemacht: ein eingebauter Verstoß (Dereferenzierung eines
  `@Nullable`-Felds ohne Prüfung) lässt `compileJava` fehlschlagen, nach
  dem Zurücknehmen ist er wieder grün.
- **Das Wire-Protokoll trägt jetzt dieselbe Unterscheidung.**
  `Messages.State` hat elf optional befüllte Felder (abhängig von der
  Phase, Invariante 4/ADR-013) — sie sind jetzt `@Nullable` annotiert,
  nicht nur im Javadoc beschrieben.
- **NullAways lokale Dataflow-Analyse verlangt an einigen Stellen einen
  direkten Null-Check auf dieselbe Variable**, selbst wenn die Nichtigkeit
  logisch schon aus einem vorherigen Aufruf folgt (z. B.
  `PlayerName.isValid(rawName)` narrowt `rawName` nicht automatisch für den
  folgenden `PlayerName.of(rawName)`-Aufruf). Der Fix ist ein zusätzlicher,
  redundant wirkender `rawName == null ||`-Check — semantisch ohne
  Wirkung, aber notwendig, damit der Compiler dieselbe Garantie sieht, die
  der Mensch schon hatte.
- Ein Fallstrick beim Einrichten: `net.ltgt.gradle-nullaway` Version 2.2.0
  exponierte `jspecifyMode` als Kotlin-`internal` und liess sich aus
  `build.gradle.kts` nicht aufrufen (unresolved reference trotz öffentlicher
  Bytecode-Sichtbarkeit) — Version 3.1.0 behebt das.

---

## ADR-027: jMolecules-Stereotypen für DDD-Bausteine und Onion-Ringe

**Status:** Akzeptiert

**Kontext:** ADR-025 hat das Domänenmodell auf Aggregate, Entities und Value
Objects umgestellt, ADR-024 auf Onion-Ringe — aber beides ausschließlich in
Javadoc und Paketstruktur ausgedrückt. „`Room` ist der Aggregate Root" stand
als Satz im Kommentar, nicht als Typ, den ein Werkzeug lesen kann. Zwei
Folgen: Erstens bemerkt niemand automatisch, wenn ein neuer Domänentyp ohne
erkennbaren Baustein dazukommt. Zweitens war die Ringzugehörigkeit nur über
Paketnamen geprüft (`ArchitectureTest.ringeZeigenNachInnen`, ADR-024) — eine
zweite, unabhängige Prüfung über eine explizite Markierung gab es nicht.

**Entscheidung:** jMolecules-Annotationen (`org.jmolecules:jmolecules-ddd`,
`org.jmolecules:jmolecules-onion-architecture`) markieren die Bausteine im
Code: `@AggregateRoot` (`Room`), `@Entity` + `@Identity` auf dem ID-Feld
(`Player`, `Round`), `@ValueObject` (alle Identitäts- und Mengen-Typen,
`Bet`/`Outcome`/`Pick`/`Params`/`Phase`), `@Service` (`Settlement`). Die
Onion-Ringe aus ADR-024 tragen zusätzlich `@DomainModelRing`,
`@DomainServiceRing`, `@ApplicationServiceRing`, `@InfrastructureRing`
(Variante „classical", weil sie exakt auf `domain/model`, `domain/service`,
`application`, `adapter`+`config` passt) auf den jeweiligen
`package-info.java`.

Reine Marker-Annotationen ohne Laufzeitverhalten, wie JSpecify (ADR-026) —
die Durchsetzung übernimmt `ArchitectureTest` mit eigenen ArchUnit-Regeln:

- `jederDomaenentypTraegtEinenBaustein`: jeder öffentliche Typ in
  `domain.model` trägt genau einen der drei DDD-Bausteine. Zwei explizite
  Ausnahmen: `RoomSnapshot` (das Dateiformat für die Platte, ADR-023, kein
  Modellbaustein) und `Bets` (ein statischer Katalog, ADR-017, kein Objekt
  mit Identität oder Wert).
- `domainServicesSindZustandslos`: `@Service`-Typen haben keine
  Instanzfelder — die Behauptung „reine Funktion" aus dem Javadoc von
  `Settlement` ist jetzt geprüft, nicht nur geschrieben.
- `keineOeffentlichenSetterAufEntities` /
  `keineOeffentlichenSetterAufDemAggregateRoot`: `@Entity`- und
  `@AggregateRoot`-Typen haben keine öffentliche `set*`-Methode — die
  Aggregatgrenze aus ADR-025 als Regel, nicht nur als paket-privater
  Modifier, den man leicht übersieht.
- `ringeTragenIhreAnnotation`: jede `package-info.java` trägt genau die nach
  ADR-024 vorgesehene Ring-Annotation, keine andere.

**Konsequenzen:**
- **`Room` trägt bewusst kein `@Identity`.** Nach ADR-005 gibt es genau eine
  Instanz, nie mehr, kein Sharding — eine Identität würde eine
  Unterscheidung vortäuschen, die es in diesem System nicht gibt. Die
  vorgefertigte jMolecules-Regel `annotatedEntitiesAndAggregatesNeedToHaveAnIdentifier()`
  hätte das als Fehler gewertet; sie wird deshalb ohnehin nicht verwendet
  (siehe nächster Punkt).
- **Die vorgefertigten jMolecules-ArchUnit-Regeln
  (`org.jmolecules.integrations:jmolecules-archunit`) werden NICHT
  verwendet.** Die neueste verfügbare Version (1.6.0, Stand 2022) ist gegen
  ArchUnit 0.23.1 gebaut. Mit der in diesem Projekt laufenden Version
  (1.3.0) wirft `JMoleculesArchitectureRules` einen `NoSuchMethodError`
  (`Architectures.layeredArchitecture()`-Signatur geändert),
  `JMoleculesDddRules` einen `AbstractMethodError` — beides erst beim
  Testlauf, nicht beim Kompilieren. Ein Downgrade auf ArchUnit 0.23.1 wurde
  probiert und verworfen: `@AnalyzeClasses` fand in der hier verwendeten
  Umgebung (Gradle 9, JDK 21) danach überhaupt keine Klassen mehr. Die
  Stereotyp-Annotationen selbst sind davon unberührt — nur die Bibliothek,
  die sie vorgefertigt prüfen wollte, ist es. Die Ersatz-Regeln oben
  benutzen ausschließlich die ArchUnit-Bausteine, die der Rest von
  `ArchitectureTest` schon verwendet (`classes()`, `methods()`,
  `ArchCondition`) und die seit 0.23.1 stabil sind.
- **`allowEmptyShould(true)`** an beiden Setter-Regeln: Aktuell hat weder
  `Room` noch eine Entity eine `set*`-Methode — der Normalfall bei korrektem
  Design ist „nichts zu beanstanden", nicht die Ausnahme. Ohne das Flag
  würde ArchUnit eine Regel, die nichts zum Prüfen findet, selbst als
  Fehlschlag werten.
- Gegenprobe für jede der vier neuen Regeln gemacht: ein fehlender
  Baustein, ein Instanzfeld an `Settlement`, ein öffentlicher Setter an
  `Room`, eine entfernte Ring-Annotation — jedes Mal schlägt genau die
  zuständige Regel fehl, sonst keine, und nach dem Zurücknehmen ist wieder
  alles grün.

## ADR-028: Repo öffentlich, Default-Branch `main`, Business Source License

**Status:** Akzeptiert

**Kontext:** Das Repo war privat und der Default-Branch hieß `master`. Das
Projekt soll künftig als Demo/Beispielanwendung für Workshops dienen und
dafür öffentlich einsehbar sein. Gleichzeitig soll der Code später auch
Grundlage für ein eigenes Produkt werden, das nicht von Dritten als
Konkurrenzprodukt verkauft werden soll — eine reine permissive Lizenz (MIT/
Apache) würde genau das erlauben.

**Entscheidung:**
- Default-Branch heißt ab jetzt `main` statt `master` (nur Namenswechsel,
  keine inhaltliche Änderung; alle Referenzen in `.github/workflows/
  release.yml`, `.releaserc.json` und `README.md` sind mitgezogen).
- Das Repo ist öffentlich. Da `ajFourteen` weiterhin einziger Collaborator
  ist, ändert das an Invariante 6 (genau eine Server-Instanz, ein Betreiber)
  nichts — Dritte können forken und Pull Requests vorschlagen, aber nicht
  direkt in dieses Repo pushen.
- Lizenz ist die Business Source License 1.1 (`LICENSE`): Quellcode frei
  einsehbar, änderbar und für nicht-kommerzielle Zwecke (Lernen, Workshops,
  private Nutzung) nutzbar, aber nicht als Produkt oder Dienstleistung
  verkaufbar. Change Date 2036-08-05, danach automatisch Apache License 2.0.
  Als Change License ist strenggenommen laut MariaDBs eigenem Covenant nur
  GPLv2-kompatibles vorgesehen; Apache 2.0 wurde trotzdem gewählt (üblich in
  der Praxis, z. B. bei Sentry), weil hier niemand die Namensrechte an
  „Business Source License" beansprucht.

**Konsequenzen:**
- Der alte `master`-Branch wird nach dem Umbenennen remote gelöscht, damit
  nicht zwei Stände nebeneinander existieren.
- Wer das Repo forkt, bekommt automatisch dieselbe Lizenz — die BSL gilt
  auch für abgeleitete Kopien.
- Diese Entscheidung betrifft nur Repo-Hülle und Lizenz, keine der übrigen
  Invarianten oder die Architektur.

---

## ADR-029: Java 25 durchgehend, mit den dafür nötigen Versionssprüngen

**Status:** Akzeptiert

**Kontext:** Der Stack stand auf Java 21; die Entwicklungsumgebung bringt
inzwischen JDK 25 mit. Der Build brach damit ab — mit der nichtssagenden
Meldung `What went wrong: 25.0.3`. Die Ursache lag nicht im Code und auch
nicht an der Toolchain-Einstellung: **Gradle 8.10.2 kann selbst nicht auf
JDK 25 laufen**, es kennt Java nur bis 23.

Zwei Java-Versionen nebeneinander sind kein tragfähiger Zustand. Wer das
Repo auscheckt, bekommt denselben Abbruch, und in der Pipeline, die nach
`teststrategie.md` künftig Tests ausführen soll, wäre es dieselbe Falle —
nur unbemerkt, weil dort niemand danebensteht.

**Entscheidung:** Java 25 durchgehend — Toolchain, Gradle-Daemon,
Docker-Build-Image und Laufzeit-Image. Keine zwei Versionen nebeneinander.

Das zog eine Kette nach sich. Jedes Glied ist durch einen Testlauf belegt,
nicht durch eine Kompatibilitätstabelle:

1. **Gradle 8.10.2 → 9.6.1.** 8.10 läuft nicht auf JDK 25.
2. **ArchUnit 1.3.0 → 1.4.1.** Das mitgelieferte ASM liest Klassendateien
   der Version 69 nicht (`Unsupported class file major version 69`) — alle
   acht Architekturregeln fielen aus.
3. **Spring Boot 3.3.4 → 3.5.16.** Spring Framework 6.1 bringt ein eigenes,
   repackagtes ASM mit demselben Problem; der `@SpringBootTest` scheiterte
   beim Lesen der Klassen-Metadaten. Das ist der Punkt, an dem „läuft schon
   irgendwie" nicht mehr trägt: Boot 3.3 unterstützt Java 25 nicht.

**Konsequenzen:**
- Alle 95 Tests grün, `bootJar` läuft.
- Der Sprung auf Boot 4.x (Spring Framework 7) war **nicht** nötig und ist
  bewusst unterblieben. 3.5 ist die letzte 3.x-Linie und der deutlich
  kleinere Eingriff; ein Framework-Major gehört nicht als Beifang in eine
  JDK-Anhebung.
- Die Gradle-9-Umstellung machte eine Deprecation im Build-Skript sichtbar
  (`val x by tasks.registering(...)`, in Gradle 10 entfernt). Umgestellt auf
  `tasks.register<Exec>("name")`; der Build ist damit warnungsfrei.
- Dockerfile: `gradle:9.6-jdk25` zum Bauen, `eclipse-temurin:25-jre-alpine`
  zur Laufzeit. An ADR-018 (512-MB-Maschine, `MaxRAMPercentage`) ändert das
  nichts.
- Die Pipeline muss die Java-Version ausdrücklich setzen, statt zu nehmen,
  was der Runner zufällig mitbringt.
- **Beobachtet, nicht erklärt:** `RestoreTest.wiederherstellungMitOffener‐
  RundeInDerZukunft…` war in einem von zehn Läufen rot und ließ sich in
  neun weiteren Läufen (fünf isoliert, drei voll, zwei nach `clean`) nicht
  reproduzieren. Der Test wartet auf einen Schreibvorgang, der nach ADR-023
  auf einem eigenen Thread läuft — das ist ein Verdacht, kein Befund. Nach
  `teststrategie.md` ist ein sporadisch roter Test ein Fehlschlag und kein
  Wiederholungsfall; aufgelöst wird er beim Umbau von `RestoreTest`.

---

## ADR-030: Teststrategie: Ebenen über JGiven-Tags, Sprachausnahme fürs Stufen-Paket

**Status:** Akzeptiert

**Kontext:** `docs/teststrategie.md` legt fest, was auf welcher Ebene
geprüft wird (Domäne, Port-to-Port, Adapter, API, Struktur), mit JGiven als
Report- und Szenariowerkzeug, jqwik für Property-Tests und einem deutschen
Gherkin-Dialekt für den Report, weil dessen Zielleser eine Fachabteilung ist,
die `anforderungen.md` kennt und keinen Code liest. Diese Strategie ist seit
Abschnitt 12 dort vollständig festgelegt; offen war nur ihre Umsetzung
(`docs/teststrategie-umsetzung.md`, Phase 1 "Gerüst").

Der deutsche Dialekt ist eine bewusste, eng begrenzte Ausnahme von der
Konvention "Bezeichner englisch, Kommentare und Dokumentation deutsch"
(CLAUDE.md): Eine JGiven-`Stage` ist Reporttext in Java-Syntax, kein
Bezeichner im gewöhnlichen Sinn. Ohne eine strukturelle Grenze wäre "deutsche
Bezeichner, weil es eine Stage ist" aber eine Ermessensfrage, die mit jeder
neuen Klasse neu verhandelt würde.

Offen war außerdem eine Detailfrage aus Phase 1: ob sich auch die
*Abschnittsüberschriften* des JGiven-HTML-Reports (die feste UI-Chrome wie
"Given"/"When"/"Then"/"Scenarios", nicht die Schritttexte) auf Deutsch
umstellen lassen. Geprüft durch Zerlegen des ausgelieferten
`app.bundle.js` (JGiven 2.0.3, `jgiven-html-app`): Die einzigen Treffer für
`i18n`/`locale`/`language` gehören zu moment.js, einer Bibliothek für die
Zeitstempel-Anzeige, nicht zum Report selbst. Es gibt keinen Lokalisierungs-
Hook für die feste Oberfläche.

**Entscheidung:**

1. **Fünf Ebenen, fünf Verantwortlichkeiten**, wie in `teststrategie.md`
   Abschnitt 1 beschrieben: Domäne (`unit`), Port-to-Port (`port`), Adapter
   (`adapter`), API (`api`), Struktur (`arch`, ArchUnit — kein JGiven, siehe
   Tabelle in Abschnitt 1). Getrennt wird über JUnit-Tags in einem
   gemeinsamen Quellbaum, nicht über eigene Source Sets, damit die
   handgeschriebenen Test Doubles (`FakeClock`, `FakeScheduler`,
   `NoSnapshots`, `RecordingClientGateway`) auf jeder Ebene dieselben
   bleiben. `RecordingClientGateway`, `NoSnapshots` und
   `RoomActor.awaitIdle()` wurden dafür von paket-privat auf `public`
   angehoben — das Stufen-Paket (Punkt 3) liegt in einem anderen Paket als
   `application` und braucht sie von dort.
2. **Vier Meta-Annotationen** (`@UnitTest`, `@PortTest`, `@AdapterTest`,
   `@ApiTest` in `de.fourteen.watchparty.teststrategy`) tragen den
   JUnit-Tag und den JGiven-`@IsTag`-Report-Tag zusammen, damit beides nicht
   auseinanderläuft — Wortlaut wie im Beispiel aus `teststrategie.md`
   Abschnitt 1. Vier Gradle-Tasks werten sie aus: `test` (unit, port — der
   schnelle Lauf), `adapterTest`, `apiTest`, `archTest`; `check` hängt alle
   vier ein, dazu einen einzigen `jgivenTestReport`, der die JGiven-Ergebnisse
   von `test`/`adapterTest`/`apiTest` zusammenführt (das Gradle-Plugin hängt
   Report-Tasks nur an Test-Tasks, die beim Anwenden des Plugins schon
   existieren — `test` tut das, die anderen als später im Skript definierte
   Tasks nicht; sie schreiben deshalb in denselben Ergebnisordner, statt
   getrennte Reports zu erzeugen). **Nachtrag aus Phase 3.1:** jqwik hat
   einen eigenen `net.jqwik.api.Tag`, unabhängig von JUnit Jupiters `@Tag`
   — die Property-Tests aus Abschnitt 4 blieben mit nur dem JUnit-Tag für
   den `--include-tag`-Filter unsichtbar (BUILD SUCCESSFUL, aber 0
   ausgeführte Tests, ohne jede Fehlermeldung). Alle vier Meta-Annotationen
   tragen deshalb zusätzlich den passenden `net.jqwik.api.Tag`. **Nachtrag
   aus Phase 4:** `archunit-junit5-engine:1.4.1` implementiert `getTags()`
   auf keinem seiner `TestDescriptor`-Knoten (durch Bytecode-Inspektion und
   einen eigenständigen `LauncherDiscoveryRequest` gegen dieselbe
   Test-Klasse verifiziert, unabhängig von Gradle). Jeder
   JUnit-Platform-`TagFilter` — gleich welche Tags, gleich ob Paket- oder
   Klassenauswahl — sortiert dadurch ausnahmslos alle ArchUnit-Tests aus.
   `ArchitectureTest` trug zwar `@Tag("arch")` und lief scheinbar unauffällig
   mit (`BUILD SUCCESSFUL`, kein Hinweis auf 0 Tests wie beim jqwik-Fund),
   wurde aber nie tatsächlich ausgeführt, seit `arch` erstmals in den
   Tag-Filter der Phase-1-Umsetzung aufgenommen wurde — ein durch
   Tag-Filterung leeres Ergebnis sieht identisch aus wie ein bestandener
   Lauf. Struktur läuft seither in einem eigenen Task `archTest`, ausgewählt
   über `includeEngines("archunit")` statt über einen Tag; `@Tag("arch")`
   bleibt auf `ArchitectureTest`/`TeststrategyArchitectureTest` als
   Dokumentation stehen, ist für die Task-Auswahl aber wirkungslos.
3. **Die Sprachausnahme ist strukturell eingehegt.** `DeutschesSzenario`
   (`teststrategy`) stellt `angenommen()`/`wenn()`/`dann()` bereit,
   `DeutscheStufe` (`teststrategy.stufen`) `und()` — beide nur dünne
   Übersetzer auf JGivens `given()`/`when()`/`then()`/`and()`. Jede
   JGiven-`Stage` muss im Paket `de.fourteen.watchparty.teststrategy.stufen`
   liegen; `TeststrategyArchitectureTest` (mit
   `ImportOption.OnlyIncludeTests`, komplementär zu
   `ArchitectureTest#ringeZeigenNachInnen`, das bewusst nur Produktivcode
   analysiert) hält das nach. "Deutsche Bezeichner" ist damit eine Frage des
   Pakets, nicht des Augenmaßes.
4. **Die Report-Abschnittsüberschriften bleiben Englisch.** Es gibt keinen
   Lokalisierungs-Hook in `jgiven-html-app` 2.0.3 (siehe Kontext) — nur die
   Schritttexte selbst sind Deutsch, weil sie aus den deutschen
   Stage-Methodennamen entstehen. Kosmetisch unschön, inhaltlich unkritisch,
   wie in `teststrategie.md` Abschnitt 8 vorgesehen.
5. **Zwei `@SpringBootTest`-Klassen auf der API-Ebene teilen sich sonst
   denselben Room.** Invariante 6 (genau eine Server-Instanz) bedeutet einen
   einzigen `Room` als Singleton-Bean; Spring cacht den Testkontext über
   Testklassen mit identischer Konfiguration hinweg. Jede
   `@SpringBootTest`-Klasse auf der API-Ebene trägt deshalb
   `@DirtiesContext(classMode = AFTER_CLASS)`.

**Konsequenzen:**
- Vier Pilotszenarien beweisen das Gerüst statt es zu behaupten: 8.1-c
  (gekappte Strafe, Domäne), 8.1-b (eingefrorener Teilnehmerkreis,
  Port-to-Port), der Snapshot-Round-Trip (Adapter), ein vollständiger
  Rundenablauf über echten Socket (API). Die vollständige Nachrüstung aller
  60 `backend`-Regeln ist Phase 3 von `teststrategie-umsetzung.md`, nicht
  Teil dieser Entscheidung.
- `RoomActor.awaitIdle()` ist jetzt öffentlich statt paket-privat;
  `getRoomForTest()` bleibt vorerst paket-privat und wird erst mit dem
  Umbau der bestehenden Actor-Tests in Phase 3.3 entfernt.
- jqwik ist eingebunden, aber noch ungenutzt — die Property-Tests aus
  Abschnitt 4 der Strategie sind ebenfalls Phase 3.1.

---

## ADR-031: Teststrategie: Metriken scharf gestellt — JaCoCo, Ebenen-Disjunktheit, PIT, Ausnahmenregister

**Status:** Akzeptiert

**Kontext:** Nach der Nachrüstung aller 60 `backend`-Regeln (Phase 3 von
`teststrategie-umsetzung.md`) fehlten noch die in `teststrategie.md`
Abschnitt 7 verlangten Metriken: Zeilenabdeckung je Ebene, die
Ebenen-Disjunktheit aus Abschnitt 7.4, ein scharf gestellter
Mutationstest (Abschnitt 7.2) und ein Ausnahmenregister für äquivalente
Mutanten (Abschnitt 10). Bis dahin war Abdeckung nur über die
`abdeckung`-Anforderungszählung sichtbar (ADR-030), nicht über tatsächlich
ausgeführte Code-Zeilen oder überlebende Mutanten.

**Entscheidung:**

1. **JaCoCo je Ebene, ohne Prozentschranke** (Abschnitt 7.3): drei
   `JacocoReport`-Tasks (`jacocoTestReport`, `jacocoAdapterTestReport`,
   `jacocoApiTestReport`), je eigene Ausführungsdaten aus `test`/
   `adapterTest`/`apiTest`, in `check` verdrahtet. Als Zielgröße erzeugt
   Abdeckung Tests, die für die Zahl geschrieben werden — hier dient sie
   nur als Artefakt und als Grundlage für Punkt 2.
2. **Ebenen-Disjunktheit als automatisiertes Gate, nicht nur als Bericht**
   (Abschnitt 7.4 verlangt das ausdrücklich "von Anfang an automatisiert").
   Task `ebenenDisjunktheit` vergleicht die drei JaCoCo-XML-Berichte
   zeilenweise, nur für `domain/`-Pakete: Deckt `adapterTest`/`apiTest`
   zusammen eine Domänenzeile ab, die `test` (unit+port) nicht selbst
   erreicht, ist das eine Lücke weiter innen, kein Verdienst der äußeren
   Ebene. Per Gegenprobe verifiziert (unit/port testweise ausgeschaltet:
   324 nur-äußerlich gedeckte Zeilen gemeldet), nicht nur für den
   Idealfall geprüft.
3. **PIT nur auf `HIGH`-Klassen, Schwelle 99 %, Testmenge nur `unit`/`port`**
   (Abschnitt 7.2) — kein Spring, kein Socket, kein Reportschreiben in der
   Mutantenschleife, sonst wird der Lauf unbenutzbar. `info.solidsoft.pitest`
   1.19.0 (1.15.0 scheitert an Gradle 9.6.1, entfernte
   `reporting.baseDir`-Property). Dabei ein echter Versionskonflikt
   gefunden und behoben: `jgiven-junit5` und `jqwik-engine` hängen direkt
   höhere `org.junit.platform`-Versionen an als Spring Boots
   Dependency-Management für die übrigen JUnit-Module durchsetzt (5.12.2 /
   1.12.2) — für die meisten Module gewinnt Spring Boots Verwaltung den
   Konflikt, aber `junit-platform-launcher` verwaltet Spring Boot gar
   nicht selbst, dort gewinnt unwidersprochen die höhere Anfrage. Ergebnis
   ohne Gegenmittel: Launcher (1.13.x) und -engine/-commons (1.12.x)
   laufen auseinander, ein `NoSuchMethodError` beim Testlauf.
   `configurations.all { resolutionStrategy.eachDependency { ... } }`
   erzwingt denselben Stand überall, greift vor der Konfliktauflösung
   selbst statt nur bei explizit anderslautender Anfrage
   (`resolutionStrategy.force` reichte dafür nicht). Dazu ein ArchUnit-Test,
   der die Menge der `@Criticality(HIGH)`-Klassen gegen die
   `pitest.targetClasses`-Konfiguration abgleicht, weil beide sich nicht
   automatisch synchron halten können.
4. **Ausnahmenregister über eine eigene Annotation, nicht über
   Konfiguration allein** (Abschnitt 10): PIT kennt keine
   `excludedAnnotations`-Eigenschaft (weder im Gradle-Plugin noch im
   Kern-CLI), wohl aber ein eingebautes, standardmäßig aktives
   Annotationsfilter-Plugin ("FANN"), über `pitest.features` angesprochen.
   Neue Annotation `de.fourteen.watchparty.mutationtest.AequivalenterMutant`
   (eigenes kleines Markerpaket, analog zu `criticality`) macht die
   Unterdrückung im Code sichtbar; `docs/test-ausnahmen.md` ist das
   dazugehörige, mit Datum geführte Register. Aktueller Stand: kein
   Eintrag, PIT steht bei 100 %.

**Ein echter, gravierender Fund bei der Verifikation von Punkt 3:**
`archunit-junit5-engine:1.4.1` implementiert `getTags()` auf keinem seiner
`TestDescriptor`-Knoten. Jeder JUnit-Platform-`TagFilter` — unabhängig von
den konkreten Tags, unabhängig davon, ob per Paket oder per Klasse
ausgewählt wird — sortiert dadurch ausnahmslos alle ArchUnit-Tests aus,
ohne jede Fehlermeldung. `ArchitectureTest` trug `@Tag("arch")` und lief
dadurch bei **keinem einzigen** `test`/`check`-Lauf seit Phase 1 von
`teststrategie-umsetzung.md` tatsächlich mit, obwohl jede bisherige
Verifikation `BUILD SUCCESSFUL` zeigte — ein durch Tag-Filterung leeres
Ergebnis sieht identisch aus wie ein bestandener Lauf. Nachtrag zu
ADR-030 (dort ausführlich beschrieben): Struktur (`arch`) läuft seither in
einem eigenen Task `archTest`, ausgewählt über `includeEngines("archunit")`
statt über einen Tag.

**Konsequenzen:**
- `check` hängt jetzt von `jacocoTestReport`, `jacocoAdapterTestReport`,
  `jacocoApiTestReport`, `ebenenDisjunktheit` und `archTest` ab, zusätzlich
  zu den bereits aus ADR-030 bekannten Tasks.
- `./gradlew pitest` ist bewusst **nicht** Teil von `check` — ein
  Mutationslauf für zwei Klassen dauert spürbar länger als der übrige
  schnelle Lauf und würde `check` für den alltäglichen Gebrauch
  verlangsamen. Er läuft eigenständig und ist die Grundlage für Punkt 3.
- Laufzeit gemessen: `./gradlew clean check pitest -PskipFrontend` von
  Grund auf, ohne Daemon, 59 Sekunden — weit unter dem 10-Minuten-Budget
  aus Abschnitt 10, keine Gegensteuerung nötig.
- `docs/teststrategie-umsetzung.md` (temporärer Arbeitsplan) verzeichnet
  alle Einzelfunde dieser Phase im Detail; dieses ADR fasst nur die
  bleibenden Entscheidungen zusammen.

## ADR-032: Screen Wake Lock als Best-Effort-Komfort, ohne automatisierten Test

**Status:** Akzeptiert

**Kontext:** ADR-021 notiert am Rand: Die Host-Rolle wandert asymmetrisch,
weil verbundene Spieler ihr Handy sperren — „das notierte Wake-Lock würde
das zusätzlich beruhigen". `offene-entscheidungen.md` führte es seitdem als
offene Idee. Dieselbe Ursache trifft auch normale Spieler: ein gesperrtes
Handy verpasst ein Wettfenster und läuft in die Nicht-Tipper-Strafe (8.1),
ohne dass es jemand merkt (`probelauf.md`, Abschnitt „Handys").

**Entscheidung:** Solange ein Spieler beigetreten ist, fordert das Frontend
über die Screen-Wake-Lock-API (`navigator.wakeLock`) einen Lock an. Die
Spezifikation gibt den Lock beim Verstecken des Tabs automatisch frei —
`useWakeLock` fängt das über `visibilitychange` ab und fordert ihn beim
Zurückkommen erneut an. Kennt der Browser die API nicht oder schlägt die
Anfrage fehl (wenig Akku, ältere iOS-Version), bleibt die App unverändert
nutzbar: **best effort, kein Fehlerzustand.**

Bewusst **kein** automatisiertes Testszenario dafür — `teststrategie.md`
§11 nennt Wake Lock ausdrücklich als etwas, das erst am Spielabend
beobachtbar ist, nicht als Backend- oder Frontend-Testfall. Ein
JGiven-Szenario würde eine Prüftiefe vortäuschen, die es nicht gibt: Eine
echte Bildschirmsperre lässt sich im Test nicht herstellen, nur die
Existenz des API-Aufrufs. Das Feature-Dokument (`docs/features/001-wake-lock.md`)
hält die vier Akzeptanzkriterien stattdessen als von Hand nachvollzogene
Prosa-Szenarien fest, wie es die Kritikalität `LOW` (reiner Client-Komfort,
keine Punkteverrechnung betroffen) rechtfertigt.

**Konsequenzen:**
- `frontend/src/useWakeLock.js`, verdrahtet in `App.jsx` über `joined` —
  aktiv vom Beitritt bis zum Verlassen des Raums (RESET oder Schließen der
  App), nicht schon auf dem Beitrittsbildschirm.
- Kein Effekt auf Backend, Snapshot oder eine der harten Invarianten aus
  `CLAUDE.md` — rein clientseitig, keine neue Nachricht im Protokoll.
- Ob der Lock die Beobachtung aus ADR-021 tatsächlich entschärft, zeigt
  erst der erste Probelauf; bis dahin bleibt es eine unbestätigte Annahme,
  keine Zusage.

## ADR-033: Mehrere Watchpartys gleichzeitig auf einer Instanz

**Status:** Akzeptiert

**Kontext:** `anforderungen.md` §11 und `offene-entscheidungen.md` schlossen
mehrere parallele Räume bisher bewusst aus — der Server *war* der Raum, ein
Prozess, ein Zustand. Der Wunsch, dass mehrere Freundesgruppen an
unterschiedlichen Orten gleichzeitig, aber unabhängig voneinander spielen
können, macht diesen Ausschluss rückgängig. Innerhalb einer Watchparty bleibt
es bei genau einer Runde gleichzeitig (Anforderung 1-b) — das war nie die
Frage, die zur Debatte stand.

**Entscheidung:** Ein Server-Prozess hält mehrere Watchpartys gleichzeitig,
jede mit eigenem Zustand, eigenem Snapshot und einem vierstelligen
alphanumerischen Code als Adresse. Ein Beitritt ohne Code erzeugt eine neue
Watchparty; ihr Ersteller wird ihr Host (ADR-016, unverändert). Ein Beitritt
mit Code tritt einer bestehenden bei. Details, Akzeptanzkriterien und
Szenarien stehen in `docs/features/004-mehrere-watchpartys.md`.

Bewusst **nicht** angetastet:

- **ADR-005 (genau eine Server-Instanz).** Die Instanz bleibt Singular, nur
  das, was sie hält, wird zur Menge. Kein Sharding, kein Routing zwischen
  Prozessen — genau die Eigenschaft, die ADR-005 sichert, macht diesen
  Umbau ohne verteilten Zustand möglich.
- **Invariante 1 (ein Thread, aller Zustand).** Ein gemeinsamer Raum-Thread
  bedient alle Watchpartys über eine Zuordnung Code → Zustand, statt eines
  Threads je Watchparty. Damit bleibt die Invariante wörtlich wahr, statt
  durch eine zweite Nebenläufigkeitsstrategie ersetzt zu werden, und die
  Threadzahl wächst nicht mit der Zahl gleichzeitiger Watchpartys.
- **Die Kapazitätsgrenzen in `fly.toml`.** 512 MB und 200 Verbindungen
  gelten weiter für die Instanz insgesamt, nicht neu je Watchparty. Bei
  Ausschöpfung scheitert der Verbindungsaufbau für alle gleichermaßen —
  akzeptiert für den erwarteten Gebrauch unter Freunden, siehe
  `docs/features/004-mehrere-watchpartys.md`, Abschnitt „Bewusste
  Festlegungen".

**Konsequenzen:**
- `anforderungen.md`: Anforderung 1-b umformuliert, §11 (out of scope) um
  den Eintrag „keine mehreren parallelen Räume" gekürzt, Anhang A um 1-g
  bis 1-l ergänzt. `offene-entscheidungen.md` verliert denselben Eintrag
  unter „bewusst ausgeschlossen".
- Die Formulierung „zwei Instanzen wären zwei getrennte Räume" (ADR-005,
  CLAUDE.md, `fly.toml`) wird zu „zwei getrennte Mengen von Watchpartys,
  mit Sitzungen, die zufällig auf der falschen landen" — das Argument
  bleibt, nur der Gegenstand der Trennung ändert sich.
- Neue Invariante in CLAUDE.md: Watchpartys sind vollständig voneinander
  getrennt — keine Nachricht, kein Kommando, kein Token wirkt über die
  Grenze einer Watchparty hinweg. Das ist die zentrale, geprüfte Zusicherung
  dieses Umbaus (`docs/features/004-mehrere-watchpartys.md`, Kritikalität
  HIGH), nicht ein Nebeneffekt der Mehrfachhaltung.
- `RoomId` wird neues Value Object; `Room` bekommt ein `@Identity`-Feld, das
  es zuvor mit ausdrücklicher Begründung nicht trug (ADR-025/027 blieben
  unberührt, nur die Instanzzahl von `Room` ändert sich).
- Der Snapshot-Pfad aus ADR-023 wird von einer Datei zu einem Verzeichnis,
  eine Datei je Watchparty; die sechs Stunden Verfallszeit gelten fortan je
  Watchparty und räumen zusätzlich leere, inaktive Watchpartys komplett ab
  (vorher nur ein Filter beim Laden).
- `/join/CODE` ist eine neue, dauerhafte URL-Form und verlangt eine
  Weiterleitung auf `index.html` im Backend, die es bisher nur für `/` gibt.
