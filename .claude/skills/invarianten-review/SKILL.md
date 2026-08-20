---
name: invarianten-review
description: Prüft eine Änderung gezielt gegen die sieben harten Invarianten aus CLAUDE.md — die Regeln, die ein generischer Code-Review nicht kennt und die meisten Tests nicht automatisch abfangen.
---

# Invarianten-Review

Vor jedem Commit, der `Room`, `Player`, `Round`, `RoomActor`,
`ClientGateway`, `ClientSession` oder `SnapshotStore` berührt — also fast
jede Änderung an der Live-Wetten-Seite. Ein generischer Review erkennt
Stil- und Logikfehler; diese sieben Regeln sind projektspezifisch und
stehen nur in `CLAUDE.md`.

## Die sieben Fragen an den Diff

1. **Zustand nur auf dem Raum-Thread.** Fasst der Diff `Room`/`Player`/
   `Round` außerhalb von `RoomActor`/`RoomCommands` an? Seit 2026-08-20
   durch `ArchitectureTest` erzwungen (`domaeneOhneNebenlaeufigkeit`,
   `domaeneOhneSynchronisierteMethoden`, `domaeneOhneVolatileFelder` —
   `synchronized`, `volatile` und `java.util.concurrent` in `domain` sind
   damit ein roter `archTest`, kein Diff-Lesen mehr). Diese Frage bleibt
   trotzdem hier stehen, weil `application` (der `RoomActor` selbst) davon
   ausdrücklich nicht erfasst ist — er braucht seinen `ExecutorService`.

2. **Der Raum-Thread blockiert nie.** Läuft Datei- oder Netz-I/O direkt im
   `RoomActor` statt über `ClientGateway`s Ausgangs-Queue oder
   `SnapshotStore`s eigenen Thread? Teilweise durch `ArchitectureTest`
   (`anwendungsringBlockiertNicht`) erzwungen — die dort gelisteten
   blockierenden Aufrufe, mit der begründeten Ausnahme `awaitIdle`. Ein neu
   eingeführter blockierender Aufruf, der (noch) nicht auf dieser Liste
   steht, fällt trotzdem nur hier auf.

3. **Der Server ist die einzige Quelle der Wahrheit.** Rechnet das
   Frontend etwas selbst aus, das der Server entscheiden sollte (Punkte,
   Fensterschluss, Verlauf)?

4. **Verdeckte Tipps sind eine Anforderung an die Leitung.** Sendet der
   Server, solange ein Wettfenster offen ist, irgendwo einen einzelnen Tipp
   statt nur den Zähler?

5. **Punkte sind ganzzahlig und nullsumme.** Neue Fließkommazahlen für
   Punkte/Anteile/Strafen? Wird eine Strafe auf den Kontostand gekappt,
   statt ihn negativ werden zu lassen? Stimmt die Summe aller Auszahlungen
   exakt mit dem Pool überein?

6. **Genau eine Server-Instanz.** Entsteht Zustand, der ein zweites
   Replikat oder Sharding voraussetzen würde?

7. **Watchpartys sind vollständig voneinander getrennt** (ADR-033). Wirkt
   ein Kommando oder eine Nachricht über die eigene Watchparty hinaus —
   auch indirekt, über eine gemeinsame Datenstruktur?

## Vorgehen

Diff lesen, jede Frage einzeln mit Ja/Nein/Unsicher beantworten. Bei
„Unsicher": den betreffenden Abschnitt in `CLAUDE.md` zitieren und
nachfragen, statt eine Annahme zu treffen — diese Regeln sind das Ergebnis
expliziter Entscheidungen, kein Stilideal.
