---
name: schneiden
description: Wenn feststeht, dass aus einer Idee ein Feature wird, und sie in umsetzbare Stücke zerfallen muss: macht daraus vertikale Schnitte — je einer dünn durch alle Ringe, je einer für sich benutzbar, je einer mit genau einer Kritikalität. Beantwortet „wie zerlege ich es", nicht „wohin gehört das" (dafür: triage). Läuft vor feature.
---

# Schneiden

Zwischen „ich hätte da eine Idee" und `docs/features/NNN-….md` fehlte bis
zum 2026-08-21 ein Schritt. Ohne ihn entsteht, was Feature 005 geworden
ist: ein Dokument mit 38 Akzeptanzkriterien, acht Kritikalitätsbereichen
und einer eigenen Neun-Stufen-Bautabelle — die Teilung hat stattgefunden,
aber erst *im* Dokument und entlang der falschen Achse.

Dieser Skill läuft **vor** `feature`. Er erzeugt keinen Code und kein
Feature-Dokument, aber einen **Schnittplan**: eine Datei unter
`docs/schnitte/<kurzname>.md`, die die Liste der Schnitte über das Ende
des Gesprächs hinaus festhält. Erst `feature` legt je Schnitt ein eigenes
Dokument unter `docs/features/` an — nicht für alle auf einmal, sondern
einen nach dem anderen, bei Bedarf auch in späteren, unabhängigen
Sitzungen: `feature` liest dafür nur den Schnittplan, keinen Chatverlauf.

## Der Fehler, den es zu vermeiden gilt

Feature 005 wurde in neun Stufen gebaut: Wertung, Persistenz, Konten,
Spieldaten, Tippen, Ligen, Oberfläche, Betrieb. Das Dokument behauptet,
jede Stufe sei „für sich abgeschlossen und einzeln einsetzbar". Für die
Stufen 1 bis 6 stimmt das nicht: Bis Stufe 7 („Oberfläche", als einzige
mit Umfang L, weil dort die Bedienung für *alles* auf einmal entstand)
konnte kein Mensch irgendetwas tun.

Das sind Schichten, keine Scheiben. Das Erkennungszeichen ist der Name:
`Wertung`, `Persistenz`, `Oberfläche` sind Bauteile. `Ein Spiel tippen`
ist eine Scheibe.

## Vorgehen

1. **Den Satz aufschreiben, den ein Mensch danach sagen kann.**
   Nicht „das System kennt Spielpläne", sondern „ich kann für den nächsten
   Spieltag tippen". Wer diesen Satz nicht hinbekommt, hat noch keine
   Idee, sondern einen Bauteilwunsch.

2. **Nach Fähigkeiten zerlegen, nicht nach Bauteilen.**
   Jeder Schnitt bekommt einen Namen in der Form *Person tut etwas*.
   Kommt ein Ringname (`domain`, Repository, Adapter, Frontend) oder ein
   Schichtwort (Persistenz, Oberfläche, Anbindung) im Namen vor, ist der
   Schnitt horizontal — noch einmal.

3. **Den ersten Schnitt so dünn wie möglich durch alle Ringe legen.**
   Alles, was nicht der Kern dieser einen Fähigkeit ist, wird durch den
   billigsten Behelf ersetzt: Handeintrag statt Feed, ein einzelnes fest
   angelegtes Spiel statt Spielplan, Log-Ausgabe statt Mailversand, eine
   nackte Liste statt einer gestalteten Ansicht.
   Bei 005 gab es diese Behelfe sogar schon (`LoggingMailSender`, der
   Handeintrag) — sie wurden als Rückfallebene gebaut statt als erste
   Scheibe benutzt.

4. **Jeder weitere Schnitt ersetzt einen Behelf oder fügt eine Fähigkeit
   hinzu.** Nie „fügt eine Schicht hinzu".

5. **Abbruchprobe.** Für jeden Schnitt: Wenn hier Schluss wäre — hätte
   jemand etwas davon? Lautet die Antwort „nein, das kommt mit dem
   nächsten", war der Schnitt horizontal.

6. **Größenprobe je Schnitt**, bevor `feature` startet:
   - genau **eine** Kritikalitätsstufe. Zwei Stufen heißen: weiter teilen.
   - höchstens **zwölf** Akzeptanzkriterien.
   - keine eigene Bautabelle. Wer im Dokument Stufen aufzählt, hat
     mehrere Features vor sich und nicht eins.

   Diese drei prüft `featuredoku` beim Bau nach — hier verhindern sie den
   Fehlschlag, dort fangen sie ihn.

7. **Schnittplan schreiben.** Unter `docs/schnitte/<kurzname>.md`, mit
   dieser Tabelle:

   | # | Schnitt | Behelf | Kritikalität | Status | Feature-Dokument |
   |---|---|---|---|---|---|

   `<kurzname>` beschreibt die Idee, nicht den ersten Schnitt, und trägt
   **keine** Nummer — die vergibt erst `feature`, je Schnitt neu, an
   `docs/features/`. `Status` ist eines von `offen`, `in Arbeit`,
   `blockiert` (mit Begründung in der Zelle, meist ein Verweis auf
   `docs/offene-entscheidungen.md`) oder `fertig`. Alle Zeilen starten als
   `offen`, außer eine Entscheidung fehlt bereits jetzt erkennbar (dann
   `blockiert`, siehe unten). `feature` pflegt Status und
   Feature-Dokument-Spalte selbst nach jedem grün gebauten Schnitt — diese
   Datei danach nicht mehr von Hand anfassen, sonst laufen Schnittplan und
   Baustand auseinander.

## Wenn Risiko und Benutzbarkeit sich widersprechen

Der begründete Einwand aus 005: Der HIGH-kritische Kern (`Scoring`) wurde
bewusst zuerst gebaut, „ohne jede Infrastruktur … solange nichts drumherum
ablenkt". Das ist eine gute Reihenfolge und bleibt erlaubt — aber als
Reihenfolge **innerhalb** des ersten Schnitts, nicht als eigener Schnitt
davor. Eine reine Funktion ohne Weg nach außen ist kein Ergebnis, das
jemand benutzen kann; sie zuerst zu schreiben ist trotzdem richtig.

## Beispiel: 005, richtig geschnitten

| # | Schnitt | Behelf | Kritikalität |
|---|---|---|---|
| 1 | Ich kann ein von Hand eingetragenes Spiel tippen | kein Feed, kein Mailversand (Log), keine Liga | HIGH (Konto/Sitzung) |
| 2 | Ich sehe, was mein Tipp wert war | — | HIGH (Wertung) |
| 3 | Ich sehe die Tipps der anderen erst ab Anstoß | — | HIGH (Sichtbarkeit) |
| 4 | Ich kann eine Liga anlegen und beitreten, mit Rangliste | Spiele weiter von Hand | MEDIUM |
| 5 | Die Spiele kommen von selbst aus dem Feed | Handeintrag bleibt als Notweg | MEDIUM |
| 6 | Ich bekomme den Anmeldelink per Mail | — | HIGH (personenbezogen) |

Sechs Dokumente mit je einer Stufe statt einem mit acht Bereichen — und
nach Schnitt 1 hätte jemand tippen können, statt nach Stufe 6 noch immer
nicht.

## Danach

Für den **ersten** Schnitt: Skill `feature` — er nimmt sich die oberste
`offen`-Zeile aus dem gerade geschriebenen Schnittplan, auch noch in
derselben Sitzung. Ein Schnitt, der selbst eine Entscheidung offenlässt
(wie Schnitt 5 im Beispiel unten), geht zusätzlich über Skill `triage`
nach `docs/offene-entscheidungen.md` und bekommt in der Status-Spalte
`blockiert` mit einem Verweis auf den dortigen Eintrag — `feature`
überspringt eine blockierte Zeile und nimmt die nächste `offen`e.

Die übrigen Schnitte bekommen ihr eigenes Feature-Dokument erst, wenn sie
dran sind, nicht auf Vorrat — der Schnittplan ist die Warteschlange dafür,
kein Ersatz für `docs/features/`.
