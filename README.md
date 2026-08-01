# Watchparty — Walking Skeleton

Lauffähiges Gerüst für die Live-Wett-App. Es enthält noch keine Wett-Logik,
sondern durchsticht einmal alle Schichten, die laut ADRs tragend sind:

- Spring Boot liefert Assets **und** WebSocket aus einem Prozess (ADR-002/003)
- Raumzustand im Arbeitsspeicher (ADR-004)
- Single-Thread-Eventloop als Raum-Actor (ADR-009)
- Senden vom Raum-Thread entkoppelt (ADR-012)
- Reconnect über Token im localStorage (ADR-014)
- Erster Joiner wird Host

**Was es kann:** Namen eingeben, beitreten, alle sehen die aktualisierte
Teilnehmerliste mit Punktekonten. Der Host sieht einen Steuerknopf, dessen
Klick serverseitig geprüft und an alle verteilt wird.

## Voraussetzungen

- JDK 21
- Node 20+ (für den Frontend-Build)
- Gradle (oder einmalig `gradle wrapper` ausführen, um den Wrapper anzulegen)

## Entwickeln

Zwei Terminals:

```bash
# Terminal 1 — Backend auf :8080
gradle bootRun -PskipFrontend

# Terminal 2 — Frontend auf :5173 mit Hot Reload
cd frontend && npm install && npm run dev
```

Dann `http://localhost:5173` öffnen. Vite proxyt `/ws` auf das Backend, damit
die WebSocket-URL in Dev und Prod identisch ist.

Zum Testen mehrerer Spieler: mehrere Browser-Profile oder Inkognito-Fenster
verwenden. Ein normaler zweiter Tab teilt sich den localStorage und würde
denselben Spieler wiederherstellen.

## Produktionsnah bauen

```bash
gradle bootJar     # baut Frontend mit und packt es ins Jar
java -jar build/libs/watchparty-0.1.0.jar
```

Dann `http://localhost:8080`.

## Deployen

```bash
docker build -t watchparty .
docker run -p 8080:8080 watchparty
```

Auf Fly.io / Railway / Render: **Instanzanzahl fest auf 1 setzen und
Autoscaling deaktivieren** (ADR-005). Zwei Instanzen wären zwei getrennte
Räume. Außerdem prüfen, ob die Plattform WebSockets durchreicht und wie lang
ihr Idle-Timeout ist.

## Protokoll (Stand Skeleton)

Client → Server:

```json
{ "type": "JOIN", "name": "Andreas", "token": "…optional…" }
{ "type": "HOST_ACTION" }
```

Server → Client:

```json
{ "type": "WELCOME", "playerId": "…", "token": "…" }
{ "type": "STATE", "players": [{ "id": "…", "name": "…", "points": 1000,
  "connected": true, "host": true }], "hostPlayerId": "…", "hostActionCount": 0 }
{ "type": "ERROR", "message": "…" }
```

Das vollständige Schema entsteht mit dem Zustandsautomaten
(IDLE → OPEN → CLOSED → RESOLVED).

## Wo die nächste Arbeit ansetzt

- `RoomActor` — hier kommen `HOST_OPEN`, `PLACE_BET`, `CLOSE`, `HOST_RESOLVE`
  hinein. Die Queue existiert bereits, es sind neue `handle*`-Methoden.
- `Room` — Zustandsautomat, aktuelle Runde, Runden-ID (ADR-010).
- `Messages` — Aufdeckung bei Schluss und Ergebnis beim Auflösen (ADR-013).
