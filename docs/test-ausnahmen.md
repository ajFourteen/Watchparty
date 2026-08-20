# Ausnahmenregister

Hier stehen alle bewussten Unterdrückungen an einer Stelle, mit Begründung
und Datum (`docs/teststrategie.md`, Abschnitt 10) — äquivalente Mutanten
unten im ersten Abschnitt, übersprungene Tests im zweiten.

## Äquivalente Mutanten

Der Mutationstest (`docs/teststrategie.md`, Abschnitt 7.2) verlangt 99 %
statt 100 %, weil äquivalente Mutanten unvermeidbar sind — semantisch
identische Änderungen, die kein Test sinnvoll töten kann, ohne
Implementierungsdetails statt Verhalten zu prüfen. Wo ein solcher Mutant
auftritt, wird er hier benannt und begründet, nicht durch einen Test
bekämpft, der nichts Fachliches mehr prüft.

**Aktueller Stand: keine Ausnahme eingetragen.** Der Mutationstest steht bei
100 % (41 von 41 Mutanten getötet, Stand 2026-08-06) — die Schranke von 99 %
lässt Spielraum für künftige äquivalente Mutanten, ohne dass jeder einzelne
sofort die Pipeline anhält.

## Mechanik

Ausschluss läuft über eine eigene Annotation,
[`AequivalenterMutant`](../src/main/java/de/fourteen/watchparty/mutationtest/AequivalenterMutant.java),
nicht über Konfiguration allein — die Unterdrückung muss im Code sichtbar
sein, an der Stelle, die sie betrifft:

```java
@AequivalenterMutant("kurze Begruendung, warum kein Test das sinnvoll toeten kann")
private static Share requireShare(Map<PlayerId, Share> shareOf, PlayerId playerId) {
    ...
}
```

PIT schließt annotierte Klassen/Methoden über das eingebaute FANN-Plugin
("Filter ANNotations") von der Mutation aus — konfiguriert in
`build.gradle.kts` (`pitest.features`). Die Annotation ist auf Typen *und*
Methoden anwendbar, damit sich ein Ausschluss so eng wie möglich fassen
lässt: eine ganze Klasse auszuschließen verdeckt auch die Mutanten in ihren
anderen Methoden, die durchaus einen Test verdienen.

## Wann eine Ausnahme berechtigt ist

Nur wenn eine Änderung am Code das **Verhalten nicht ändert** — nicht, wenn
sie es ändert, aber kein bestehender Test es bemerkt. Letzteres ist eine
Testlücke, keine Ausnahme: Als beim Einrichten dieses Mechanismus (ADR-031)
`RoomView.catalog()` als nur über den Umweg des WELCOME-Frames geprüft
auffiel, bekam es einen neuen Test (`RoomViewCatalogTest`), keine Ausnahme
hier.

## Wie ein neuer Eintrag angelegt wird

1. Die Methode oder Klasse mit `@AequivalenterMutant("Begründung")`
   annotieren — die Begründung steht direkt im Code.
2. Hier einen Eintrag ergänzen: Klasse/Methode, welcher Mutant (Mutator und
   Zeile aus dem PIT-Report), Begründung, Datum.
3. `./gradlew pitest` erneut laufen lassen und prüfen, dass der Mutant nicht
   mehr als `SURVIVED`/`NO_COVERAGE` erscheint, sondern gar nicht mehr
   generiert wird.

| Klasse/Methode | Mutator | Begründung | Datum |
|---|---|---|---|
| _(keine Einträge)_ | | | |

## Übersprungene Tests

Ein `@Disabled`-Test ist die zweite Form der Unterdrückung: Er entzieht dem
Bau eine Prüfung, ohne ihn rot zu machen. Deshalb gilt für ihn dieselbe
Regel wie für einen äquivalenten Mutanten — benannt, begründet, datiert.

Ein abgeschalteter Test ist fast immer die falsche Antwort: Prüft er etwas
Falsches, gehört er korrigiert; prüft er etwas Richtiges, das gerade nicht
gilt, ist das ein Fund. `@Disabled` bleibt für den Fall, dass eine Prüfung
nachweislich von etwas abhängt, das in dieser Umgebung nicht herstellbar ist
— dann steht genau das hier als Begründung.

| Test | Begründung | Datum |
|---|---|---|
| _(keine Einträge)_ | |

## Wie geprüft wird

Der Gradle-Task `ausnahmenregister` (an `check` gehängt) sammelt alle
`@AequivalenterMutant`- und `@Disabled`-Vorkommen aus den kompilierten
Klassen ein und gleicht sie mit den Tabellen oben ab. Der Abgleich läuft in
beide Richtungen:

- Eine Unterdrückung ohne Eintrag bricht den Build ab. Das ist die Regel aus
  Abschnitt 10 der Teststrategie, die bis dahin niemand geprüft hat.
- Ein Eintrag ohne Unterdrückung ebenfalls. Ein Register, in dem
  Karteileichen stehen bleiben, verliert seinen Zweck genauso wie eines, in
  dem Einträge fehlen.

Der Bezeichner in der ersten Spalte ist der einfache Klassenname, bei einer
Methode zusätzlich `.methodenname` — also `Settlement.requireShare`, nicht
der voll qualifizierte Name. Die Tabelle wird von Menschen gepflegt und soll
lesbar bleiben.
