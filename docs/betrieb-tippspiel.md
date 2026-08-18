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

## Feed: Überwachung

**Ausgangslage.** `ScheduleSyncJob` ruft `syncSeason` alle
`watchparty.league.schedule.sync-interval-minutes` (Default 15) auf.
Schlägt der Abruf eines Spieltags fehl, fängt `ScheduleSyncService` die
Exception ab, loggt auf WARN-Niveau
(„Feed nicht erreichbar für {} — letzter bekannter Stand bleibt stehen",
Kriterium 11) und meldet den Fehlschlag an `ScheduleSyncJob` zurück — der
Sync-Takt selbst läuft unbeeinflusst weiter, kein Absturz, kein
Datenverlust. Einzelne unlesbare Spiel-Einträge innerhalb einer Antwort
werden in `EspnScheduleFeed.mapEvent` übersprungen und nur geloggt (dazu
gleich mehr).

**Alarm bei andauerndem Ausfall (seit 2026-08-18 umgesetzt).**
`ScheduleSyncJob` zählt aufeinanderfolgende Läufe, in denen der Feed für
mindestens einen Spieltag nicht erreichbar war. Ab drei Läufen in Folge
(Default-Takt: 45 Minuten) ruft er `AlertSender.feedUnreachable` auf — genau
einmal pro Ausfallserie, nicht bei jedem weiteren Lauf; ein erfolgreicher
Lauf setzt den Zähler zurück und macht einen neuen Ausfall wieder alarmfähig.
Produktiv (`AlertMailSender`, an dieselbe IONOS-Konfiguration wie der
Anmeldelink angehängt) geht die Mail an `watchparty.league.alert.email`
(vorgesehen: `info@fourteen-it.de`); ohne gesetzte SMTP-Zugangsdaten
übernimmt `LoggingAlertSender` und schreibt nur ins Log, dieselbe
Rückfallebene wie beim Anmeldelink (`LoggingMailSender`).

Wichtig für Invariante 2 (CLAUDE.md): `ScheduleSyncJob` läuft auf demselben
geteilten Scheduler-Thread wie das Auto-Close der Live-Wetten. Der Aufruf
von `AlertSender.feedUnreachable` darf diesen Thread deshalb nicht
blockieren — `AlertMailSender` reiht den SMTP-Versand nur in einen eigenen
Thread ein und kehrt sofort zurück, dieselbe Bauweise wie der Schreib-Thread
in `SnapshotStore`.

**Was der Alarm nicht abdeckt.** Das zweite Fehlerbild — der Feed liefert
Daten, aber in kaputtem Format (`EspnScheduleFeed.mapEvent` überspringt
Einträge, Log-Zeilen beginnen mit „Ueberspringe") — löst keinen Alarm aus.
Ein Format-Bruch ist schwerer zuverlässig zu erkennen (wie viele
übersprungene Spiele sind noch normal, z. B. durch Bye-Wochen?) und bleibt
bewusst ein log-basiertes, manuelles Beobachtungsfeld: `fly logs` an einen
externen Log-Drain mit Text-Matching auf „Ueberspringe", gehäuft innerhalb
eines Sync-Laufs. Kein Produktivcode dafür — bei Bedarf ein eigenes, klein
geschnittenes Feature mit einer belastbaren Schwelle.

Ein eigener Health- oder Status-Endpunkt („wann war der letzte erfolgreiche
Sync") bliebe darüber hinaus eine sinnvolle Ergänzung, ist aber ebenfalls
kein Bestandteil dieser Umsetzung.

**Handeintrag als Auffangnetz.** Auch ohne Alarm bleibt Kriterium 11 die
Rückfallebene: Der letzte bekannte Stand steht, nichts wird stillschweigend
falsch. Der Handeintrag-Notweg (Kriterium 14, `offene-entscheidungen.md`
bzw. jetzt `005-tippspiel-liga.md`/ADR-036) fängt den Fall auf, in dem der
Feed dauerhaft ausfällt oder falsch liegt — vorausgesetzt, jemand hat
gemerkt, dass er ausgefallen ist. Deshalb ist ein Alarm kein „Nice-to-have",
sondern die einzige Verbindung zwischen den beiden.

## Was hier bewusst fehlt

Secrets (Postgres-URL, SMTP-Zugangsdaten, `watchparty.league.admin.email`)
und die Datenschutzerklärung sind eigene Punkte in Stufe 8 und nicht Teil
dieses Dokuments — Secrets, weil sie ausschließlich in Fly-Secrets gehören
und keine Planung brauchen, sondern Ausführung mit echtem Fly-Zugriff; die
Datenschutzerklärung, weil sie Angaben braucht, die nur der Betreiber liefern
kann (Impressum, Kontakt, Serverstandort).
