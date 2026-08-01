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

Lokal im Container:

```bash
docker build -t watchparty .
docker run -p 8080:8080 watchparty
```

Produktiv läuft die App auf Fly.io unter
`https://watchparty.fourteen-it.de` (ADR-018).

### Automatischer Deploy (Semantic Release)

Normalfall ist nicht der manuelle Befehl, sondern ein Merge nach `master`
(ADR-019). `.github/workflows/release.yml` wertet die Commit-Messages aus:

- `fix: …` → Patch-Release
- `feat: …` → Minor-Release
- `feat!: …` oder ein `BREAKING CHANGE:`-Footer → Major-Release
- `chore: …`, `docs: …`, `refactor: …` (ohne Verhaltensänderung) usw. →
  **kein** Release, **kein** Deploy

Nur bei einem tatsächlich veröffentlichten Release folgt automatisch
`flyctl deploy --ha=false`. Damit gilt die Deploy-Regel von oben weiterhin,
nur verschoben: **Ein release-relevanter Commit (`fix:`/`feat:`) darf nicht
am Spieltag nach `master` gemerged werden** — der Deploy folgt dann
automatisch und sofort.

Einmalige Einrichtung, zwei Repo-Secrets unter *Settings → Secrets and
variables → Actions*:

- `FLY_API_TOKEN` — ein auf diese App beschränkter Token:
  ```bash
  fly tokens create deploy -a watchparty-fourteen -x 8760h
  ```
  (Ablauf hier auf ein Jahr begrenzt, statt der 20-Jahre-Voreinstellung.
  Rechtzeitig vor Ablauf erneuern.)
- `GITHUB_TOKEN` wird von GitHub Actions automatisch bereitgestellt, dafür
  ist nichts zu tun.

### Manueller Deploy (Notfall)

```bash
fly deploy --ha=false
fly machines list          # muss genau eine Maschine zeigen
```

Für einen dringenden Fix ohne Versionsbump, oder wenn die Automation gerade
nicht verfügbar ist. Die Konfiguration steht in `fly.toml`. Die Werte dort
sind keine Tuning-Parameter: `min_machines_running = 1` und
`auto_stop_machines = false` setzen ADR-005 um. Zwei Instanzen wären zwei
getrennte Räume, und eine gestoppte Maschine verliert den kompletten
Raumzustand (ADR-004).

**`--ha=false` ist nicht optional** — gilt für beide Wege. `fly deploy` legt
sonst von sich aus eine zweite Maschine für High Availability an, auch bei
`min_machines_running = 1`; die Einstellung verhindert das nicht. Beim
allerersten Deploy ist das genau so passiert. Der GitHub-Actions-Workflow
hat `--ha=false` fest verdrahtet; beim manuellen Befehl nicht vergessen.
Nach jedem Deploy zur Sicherheit `fly machines list` prüfen; steht dort mehr
als eine Maschine, korrigiert `fly scale count 1` das. Zwei Maschinen heißt:
Die Spieler landen zufällig in einem von zwei getrennten Räumen und sehen
sich gegenseitig nicht.

**Betriebsregel: nicht am Spieltag deployen.** Ein Deploy ist ein Neustart,
und ein Neustart kostet nicht die laufende Runde, sondern die Punktestände
des ganzen Abends. Die Tokens im localStorage zeigen danach ins Leere. Beim
automatischen Weg heißt das konkret: **release-relevante Commits
(`fix:`/`feat:`) nicht am Spieltag nach `master` mergen** — der Merge selbst
ist bereits der Auslöser.

### Domain einrichten (einmalig)

Bei IONOS in der DNS-Verwaltung von `fourteen-it.de` einen Record anlegen:

| Feld | Wert |
|---|---|
| Typ | CNAME |
| Hostname | `watchparty` |
| Zeigt auf | `watchparty-fourteen.fly.dev` |

Nur das Label `watchparty` eintragen — IONOS hängt die Domain selbst an.
Nicht die separate „Subdomain"-Funktion mit Weiterleitung benutzen, eine
HTTP-Weiterleitung würde die WebSocket-Verbindung zerlegen. Danach:

```bash
fly certs add watchparty.fourteen-it.de
fly certs show watchparty.fourteen-it.de
```

Fly holt das Let's-Encrypt-Zertifikat selbst und erneuert es automatisch.

Der belastbare Test ist nicht das grüne Schloss, sondern zwei Handys, die
über die Domain joinen und sich gegenseitig in der Teilnehmerliste sehen.
Kommt der WebSocket-Upgrade nicht durch, lädt die Seite trotzdem und bleibt
nur leer.

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
