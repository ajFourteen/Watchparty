# Betrieb Tippspiel — Sicherung und Feed-Überwachung (Stufe 8)

Vorbereitung für die beiden Punkte aus Stufe 8
(`docs/features/005-tippspiel-liga.md`), die sich am Schreibtisch planen
lassen. Das eigentliche Einrichten (Postgres anlegen, Alert-Kanal
konfigurieren) ist damit noch nicht getan — das bleibt eine echte
Infra-Aktion mit Fly-Zugriff. Secrets und Datenschutzerklärung sind bewusst
nicht Teil dieses Dokuments (siehe Feature-Dokument, Stufe 8).

## Datenbank: Sicherung und Rückspielprobe

**Ausgangslage.** `LeagueDatabaseConfig` erwartet `watchparty.league.db.url`
und wendet die Flyway-Migrationen unter
`src/main/resources/db/league/migration` beim Start an. Es gibt noch keine
angebundene Postgres-Instanz (`fly.toml` kennt bisher nur das
Snapshot-Volume der Live-Wetten, ADR-023 — davon unberührt). Anders als
dieses Volume ist die Ligadatenbank kein „Abzug", sondern der einzige
Bestand einer ganzen Saison (Begründung in „Bewusste Festlegungen",
005-tippspiel-liga.md).

**Anbieter: unmanaged Fly Postgres, nicht Managed Postgres (Rückfrage vom
2026-08-18, ADR-035-Nachtrag).** Zwei Alternativen wurden geprüft und
verworfen: Ein Postgres bei einem externen Anbieter (Neon/Supabase/Aiven)
wäre eine zusätzliche Außenabhängigkeit; die in Stratos Hosting-Paket
enthaltene Datenbank ist MySQL/MariaDB statt Postgres und ihr Hostname löst
auf eine private IP auf — von Fly.io aus nicht erreichbar (getestet). Flys
klassisches, selbst betriebenes Postgres (`fly postgres create`, ein
Cluster aus gewöhnlichen Fly Machines) kostet für diese Größenordnung
etwa **$2–3/Monat** (shared-cpu-1x mit 256 MB + 1 GB Volume) — deutlich
günstiger als Flys separates Produkt „Managed Postgres" (MPG), das dafür
eine höhere, eigene Preisstruktur hat.

**Einrichten.**
```bash
fly postgres create --name watchparty-league-db -r fra --vm-size shared-cpu-1x --volume-size 1
fly postgres attach watchparty-league-db -a watchparty-fourteen
```
`attach` setzt `DATABASE_URL` als Fly-Secret auf der App — das ist nicht
dasselbe Property wie `watchparty.league.db.url`; die Zuordnung
(`WATCHPARTY_LEAGUE_DB_URL=$DATABASE_URL` o. ä.) gehört in die Secrets-Arbeit,
nicht hierher.

**Automatische Sicherung, und ihre Grenze.** Unmanaged Fly Postgres
erzeugt täglich einen Snapshot des Datenbank-Volumes, aufbewahrt für 5
Tage — automatisch, ohne Zutun. Das ist **kein georedundantes Backup**:
Snapshot und Datenbank hängen am selben Fly-Volume, ein Verlust der
zugrundeliegenden Maschine oder Region kann im ungünstigsten Fall beide
gleichzeitig treffen. Für dieses Projekt bewusst akzeptiertes Risiko
(ADR-035-Nachtrag) — wer das nicht will, braucht zusätzlich einen eigenen
Offsite-Dump (z. B. ein periodischer `pg_dump`, weggeschrieben außerhalb
von Fly) oder einen vollständig verwalteten Dienst.

**Die Rückspielprobe.** Eine Sicherung, die nie zurückgespielt wurde, ist
eine Vermutung (Feature-Dokument, „Was dieses Feature ins Projekt holt").
Die 5-Tage-Aufbewahrung bedeutet: Eine Probe kann nicht beliebig
nachgeholt werden, sie gehört in einen festen Rhythmus, nicht auf „mach
ich mal irgendwann". Vorgehen, wiederholbar und ohne die
Produktivdatenbank anzufassen:

1. Neue, temporäre Fly-Postgres-Instanz aus dem jüngsten Snapshot
   erzeugen (`fly postgres create` mit Wiederherstellungsoption, siehe
   `fly postgres backup list`/`fly postgres backup restore` in der
   aktuellen Fly-Dokumentation).
2. Gegen die wiederhergestellte Instanz stichprobenartig prüfen: Anzahl
   Konten, Anzahl Ligen, ein bekannter Tipp mit erwarteter Punktzahl.
3. Temporäre Instanz löschen.
4. Ergebnis (Datum, Dauer, Befund) in `docs/probelauf.md` oder einem
   Betriebslog festhalten — nicht hier, das ist keine einmalige Checkliste,
   sondern wiederkehrende Praxis über die Saison.

**Wann.** Mindestens einmal vor dem ersten Spieltag mit echten Nutzern, und
danach in einem Rhythmus, der zur Kritikalität passt (z. B. monatlich
während der Saison) — die genaue Kadenz ist eine Betriebsentscheidung, keine
technische.

## Feed: Zugriff über einen Relay statt direkt aus der Anwendung

**Warum.** ESPN blockiert Zugriffe aus Fly.ios IP-Bereich mit `403
Forbidden` (Akamai, festgestellt am 2026-08-18) — der interne, alle 15
Minuten selbst nachplanende `ScheduleSyncJob` schlug seitdem bei jedem
einzelnen Spieltag fehl. Kein Format- oder Header-Problem: Von anderen
Netzen aus liefert derselbe ESPN-Endpunkt weiterhin `200`. Das ist
vermutlich eine IP-Reputationssperre gegen Rechenzentrums-Adressen, keine
gezielte Sperre gegen dieses Projekt — und nicht zuverlässig durch
Header-Tricks zu umgehen (das wäre ohnehin ein Wettlauf gegen Akamais
Bot-Erkennung, den dieses Projekt nicht führen will). `ScheduleSyncJob` ist
deshalb entfernt worden (ADR-037-Nachtrag), nicht nur pausiert.

**Wie es jetzt läuft.** Ein täglicher GitHub-Actions-Workflow
(`.github/workflows/schedule-relay.yml`, Cron 08:00 UTC) ruft ESPN direkt
von einem GitHub-Runner ab — anderes Netz, nicht blockiert — für alle 18
Spieltage der Regular Season. Die rohe Antwort schickt er per `POST` an
`/api/league/feed-relay/{season}/{week}`, authentifiziert über den Header
`X-Relay-Token` (geteiltes Secret, nicht der Admin-Magic-Link — hier meldet
sich keine Person an, sondern eine Maschine). Der Endpunkt
(`ScheduleController`) nutzt dieselbe Parse- und Abgleichlogik wie zuvor
(`EspnScheduleFeed.parse`, jetzt über `ScheduleFeed.parseExternalResponse`
erreichbar) — Kriterium 11 (letzter bekannter Stand bleibt bei Ausfall
stehen) und das Überspringen einzelner kaputter Einträge gelten unverändert,
nur ohne die eigene, blockierte Netzwerkverbindung.

**Secrets, die dafür beidseitig gesetzt sein müssen** (derselbe Wert):
- Fly: `WATCHPARTY_LEAGUE_SCHEDULE_RELAY_TOKEN`
- GitHub-Actions-Repository-Secret: `SCHEDULE_RELAY_TOKEN`

**Überwachung.** Kein eigener Alarm-Mechanismus mehr in der Anwendung (der
`AlertSender`/`AlertMailSender`-Mechanismus vom selben Tag ist mit dem Job
entfernt worden, siehe ADR-037-Nachtrag) — GitHub benachrichtigt bei einem
fehlgeschlagenen Scheduled Workflow bereits von sich aus (Standard-Mail an
Repo-Beobachter). Der Workflow selbst lässt einen einzelnen fehlgeschlagenen
Spieltag die übrigen nicht aufhalten (derselbe Grundsatz wie Kriterium 11),
meldet am Ende aber einen Fehler, wenn mindestens einer scheiterte — das
reicht als Signal, um GitHubs Benachrichtigung auszulösen.

**Was weiterhin nicht abgedeckt ist.** Ein Format-Bruch bei ESPN (einzelne
Einträge werden übersprungen, Log-Zeilen beginnen mit „Ueberspringe") löst
weiterhin keinen aktiven Alarm aus — bleibt ein log-basiertes, manuelles
Beobachtungsfeld (`fly logs`), aus denselben Gründen wie zuvor: schwer
zuverlässig von normalen Bye-Wochen zu unterscheiden.

**Handeintrag als Auffangnetz.** Auch wenn der tägliche Relay einmal
ausfällt, bleibt Kriterium 11 die Rückfallebene: Der letzte bekannte Stand
steht, nichts wird stillschweigend falsch. Der Handeintrag-Notweg
(Kriterium 14, ADR-036) fängt den Fall auf, in dem Ergebnisse gebraucht
werden, bevor der nächste Relay-Lauf sie nachträgt.

## Was hier bewusst fehlt

Secrets (Postgres-URL, SMTP-Zugangsdaten, `watchparty.league.admin.email`)
und die Datenschutzerklärung sind eigene Punkte in Stufe 8 und nicht Teil
dieses Dokuments — Secrets, weil sie ausschließlich in Fly-Secrets gehören
und keine Planung brauchen, sondern Ausführung mit echtem Fly-Zugriff; die
Datenschutzerklärung, weil sie Angaben braucht, die nur der Betreiber liefern
kann (Impressum, Kontakt, Serverstandort).
