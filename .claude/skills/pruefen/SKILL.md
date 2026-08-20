---
name: pruefen
description: Gestufte lokale Rückkopplung statt Blindflug — compileJava vor test vor archTest vor vollem check, Abbruch beim ersten Rot.
---

# Prüfen

Kein neuer Prüfumfang gegenüber `gradle check` — nur eine andere
Reihenfolge, damit ein Fehler in Sekunden auffällt statt erst nach dem
vollen, mehrminütigen Lauf inklusive Mutationstests.

## Stufen, in dieser Reihenfolge

1. **`./gradlew compileJava -PskipFrontend`** — NullAway, Sekunden.
   Rot? Nullness- oder Compile-Fehler beheben, hier stehenbleiben. Noch
   nicht weiter unten prüfen.

2. **`./gradlew test -PskipFrontend`** — schnelle Unit-/Domain-Ebene
   (JUnit, JGiven-Szenarien der Domäne, jqwik-Properties).

3. **`./gradlew archTest`** — Ringe (ADR-024), DDD-Stereotypen
   (ADR-025/027), Kritikalitätsregeln (§6.2).

4. **`./gradlew check`** — voller Lauf: zusätzlich `adapterTest`,
   `apiTest`, JaCoCo je Ebene, `ebenenDisjunktheit`, `pitest`,
   `abdeckung`, `aufbaudoku`, `protokollvertrag`, `ausnahmenregister`,
   `jgivenTestReport`.

## Die Regel

Jede Stufe nur starten, wenn die vorige grün war. Nicht `check` direkt
aufrufen und anschließend durch eine lange, gemischte Fehlerliste wühlen —
die meisten Fehler, die `check` insgesamt meldet, wären schon bei Stufe 1
oder 2 sichtbar geworden.

Bei Rot: den Bericht der jeweiligen Stufe lesen (z. B.
`build/reports/abdeckung.txt`, `build/reports/protokollvertrag.txt`,
`build/reports/aufbaudoku.txt`), nicht aus der Konsolenausgabe raten — die
Berichte sind für genau diesen Zweck geschrieben.
