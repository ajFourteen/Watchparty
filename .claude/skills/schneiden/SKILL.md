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
Dokument, sondern eine Liste von Schnitten. Erst danach legt `feature` für
den **ersten** davon ein Dokument an — nicht für alle.

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

Für den **ersten** Schnitt: Skill `feature`. Die übrigen bleiben eine
Liste in der Antwort oder — wenn sie eine Entscheidung offenlassen —
gehen über Skill `triage` nach `docs/offene-entscheidungen.md`. Sie
bekommen ihr Dokument, wenn sie dran sind, nicht auf Vorrat.
