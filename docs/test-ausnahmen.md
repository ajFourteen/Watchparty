# Ausnahmenregister für Mutationstests

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
