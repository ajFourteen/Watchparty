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
005-tippspiel-liga.md): Verwaltetes Postgres statt SQLite auf dem Volume,
ausdrücklich wegen der Sicherung.

**Einrichten (wenn es so weit ist).**
```bash
fly postgres create --name watchparty-league-db -r fra --vm-size shared-cpu-1x --volume-size 1
fly postgres attach watchparty-league-db -a watchparty-fourteen
```
`attach` setzt `DATABASE_URL` als Fly-Secret auf der App — das ist nicht
dasselbe Property wie `watchparty.league.db.url`; die Zuordnung
(`WATCHPARTY_LEAGUE_DB_URL=$DATABASE_URL` o. ä.) gehört in die Secrets-Arbeit,
nicht hierher.

**Automatische Sicherung.** Flys verwaltetes Postgres sichert per
WAL-Archivierung fortlaufend; der genaue Mechanismus und die Aufbewahrungs­
frist stehen in der aktuellen Fly-Dokumentation und sind vor der Einrichtung
zu prüfen, nicht hier festzuschreiben — das ändert sich außerhalb dieses
Projekts. Wichtig ist nicht die Existenz der Sicherung, sondern:

**Die Rückspielprobe.** Eine Sicherung, die nie zurückgespielt wurde, ist
eine Vermutung (Feature-Dokument, „Was dieses Feature ins Projekt holt").
Vorgehen für eine Probe, wiederholbar und ohne die Produktivdatenbank
anzufassen:

1. Neue, temporäre Fly-Postgres-Instanz aus dem jüngsten Snapshot/Backup
   erzeugen (`fly postgres create` mit Wiederherstellungsoption, oder
   `pg_dump`/`pg_restore` gegen einen manuellen Dump — je nachdem, welchen
   Mechanismus die konkrete Fly-Postgres-Variante zum Zeitpunkt der
   Einrichtung anbietet).
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
Exception ab und loggt auf WARN-Niveau
(„Feed nicht erreichbar für {} — letzter bekannter Stand bleibt stehen",
Kriterium 11) — der Sync-Takt läuft unbeeinflusst weiter, kein Absturz, kein
Datenverlust. Einzelne unlesbare Spiel-Einträge innerhalb einer Antwort
werden in `EspnScheduleFeed.mapEvent` übersprungen und ebenfalls nur
geloggt. Es gibt aktuell **keinen aktiven Alarm** — ein Ausfall ist im Log
sichtbar, aber niemand wird benachrichtigt.

**Was „Überwachung" hier bedeutet.** Zwei unterscheidbare Fehlerbilder, beide
über den Log-Text zu erkennen:
- *Feed nicht erreichbar* (Netzwerk/HTTP-Fehler, ESPN antwortet nicht) —
  Log-Zeile beginnt mit „Feed nicht erreichbar fuer".
- *Feed liefert Unsinn* (Format geändert, Pflichtfelder fehlen) — Log-Zeilen
  beginnen mit „Ueberspringe" (`EspnScheduleFeed`). Einzelne übersprungene
  Spiele sind unauffällig; viele auf einmal deuten auf eine geänderte
  ESPN-Antwortstruktur hin (das angenommene Risiko aus ADR-037).

**Vorschlag für einen Alarm, ohne Produktivcode dafür zu bauen:** Ein
log-basierter Alert auf Fly (`fly logs` lässt sich an einen externen
Log-Drain weiterreichen, z. B. an einen Dienst mit Text-Matching/Grep-Alert)
mit zwei Mustern:
1. `"Feed nicht erreichbar"` mehrfach hintereinander (z. B. drei
   aufeinanderfolgende Sync-Läufe = 45 Minuten) — der Feed ist wirklich
   unten, nicht nur ein einzelner Netz-Hänger.
2. `"Ueberspringe"` gehäuft innerhalb eines Sync-Laufs (Schwellwert grob:
   mehr als die Hälfte eines Spieltags) — Format-Bruch statt Einzelfall.

Ein eigener Health- oder Status-Endpunkt („wann war der letzte erfolgreiche
Sync") wäre die sauberere Lösung, ist aber neuer Produktivcode und damit
kein Bestandteil dieses Runbooks — bei Bedarf ein eigenes, klein
geschnittenes Feature.

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
