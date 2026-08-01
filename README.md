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
  Ablauf hier auf ein Jahr begrenzt, statt der 20-Jahre-Voreinstellung.
  **Aktuelles Token gesetzt am 2026-08-01, läuft ca. 2027-08-01 ab.** Läuft
  es aus, scheitert nur der `deploy`-Job beim nächsten `fix:`/`feat:`-Merge
  (die laufende App ist davon nicht betroffen) — es gibt aber keine
  automatische Erinnerung dafür, also selbst vormerken. Erneuern mit
  demselben Befehl, danach den neuen Wert unter *Settings → Secrets and
  variables → Actions* über `FLY_API_TOKEN` setzen.
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

### Rollback

Ein automatischer Deploy ist trotzdem ein Neustart — auch ein Rollback
kostet den laufenden Raumzustand (ADR-004), genau wie das kaputte Release,
das er ersetzt. Es gibt hier nichts, das eine laufende Runde retten könnte;
das Ziel ist nur, schnell wieder eine funktionierende Version live zu haben.

```bash
fly releases -a watchparty-fourteen        # Versionen durchsehen
fly deploy --ha=false --image <ImageRef-der-letzten-guten-Version>
fly machines list                          # muss genau eine Maschine zeigen
```

`ImageRef` steht in der Ausgabe von `fly releases --json` je Version (Feld
`ImageRef`, Format `registry.fly.io/watchparty-fourteen:deployment-…`).
`fly apps rollback` existiert ebenfalls, wählt die Vorversion aber implizit
aus — bei nur einer Maschine und der Bedeutung jedes Neustarts hier lieber
explizit das Image angeben, damit klar ist, welche Version tatsächlich
läuft.

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

## Protokoll

Der Zustandsautomat ist ADR-020 (IDLE → OPEN → CLOSED → RESOLVED).

Client → Server:

```json
{ "type": "JOIN", "name": "Andreas", "token": "…optional…" }
{ "type": "OPEN_MARKET" }
{ "type": "PLACE_BET", "outcomeId": "touchdown", "stake": 100 }
{ "type": "CLOSE_MARKET" }
{ "type": "RESOLVE", "outcomeId": "touchdown" }
```

`OPEN_MARKET`, `CLOSE_MARKET` und `RESOLVE` sind Host-Aktionen (ADR-021). Bei
`PLACE_BET` ist `stake` optional — ohne Angabe gilt der Mindesteinsatz; wer
weniger Punkte als den Mindesteinsatz hat, geht serverseitig zwangsweise
All-in (Anforderung 6/8.3), unabhängig vom angefragten Wert.

Server → Client:

```json
{ "type": "WELCOME", "playerId": "…", "token": "…" }
{ "type": "YOUR_BET", "outcomeId": "touchdown", "stake": 100 }
{ "type": "ERROR", "message": "…" }
{
  "type": "STATE",
  "players": [{ "id": "…", "name": "…", "points": 1000,
    "connected": true, "paused": false, "host": true }],
  "hostPlayerId": "…",
  "phase": "OPEN",
  "roundId": 1,
  "market": { "id": "drive-outcome", "question": "…", "outcomes": [ … ] },
  "closesAt": 1785624019729,
  "serverNow": 1785624004738,
  "betCount": 1,
  "participantCount": 2,
  "revealedBets": [{ "playerId": "…", "outcomeId": "…", "stake": 100 }],
  "winningOutcomeId": "touchdown",
  "pool": 150,
  "annulled": false,
  "deltas": { "playerId": 50 }
}
```

`STATE` ist immer vollständig (Reconnect zieht den kompletten Zustand neu,
ADR-014), aber phasenabhängig gefüllt: `closesAt`/`betCount`/
`participantCount` nur in OPEN, `revealedBets` ab CLOSED, `winningOutcomeId`/
`pool`/`annulled`/`deltas` nur in RESOLVED. Solange OPEN läuft, verlässt kein
einzelner Tipp den Server außer im gezielten `YOUR_BET` an die eigene Session
(ADR-013) — wer die Frames mitliest, erfährt nur den Zähler.

**Breaking Change gegenüber dem Walking Skeleton:** `HOST_ACTION` und
`hostActionCount` sind ersatzlos entfallen; sie waren nur ein Platzhalter, um
zu beweisen, dass eine Host-Aktion serverseitig ankommt. Jeder Client, der
noch dagegen spricht, muss auf die vier echten Aktionen umgestellt werden.
