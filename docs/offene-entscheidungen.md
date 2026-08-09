# Offene Entscheidungen

Diese Punkte sind bewusst noch nicht entschieden. Bitte nicht stillschweigend
festlegen, sondern nachfragen.

**Ein geklärter Punkt verschwindet hier.** Die Antwort wird in `docs/adrs.md`
bzw. `docs/anforderungen.md` nachgezogen — dort steht sie dann als geltender
Stand, und Anhang A hält sie über die Feature-Abdeckung nach. Wird aus der
Frage ein dauerhafter Ausschluss, wandert sie nach unten in „bewusst
ausgeschlossen" — andernfalls bleibt kein Eintrag zurück. Ein Protokoll
gefallener Entscheidungen führt dieses Dokument nicht: Es wäre nach einer
Saison überwiegend eine Liste geschlossener Fragen und verfehlte damit
seinen Zweck.

Offen heißt: Es steht eine Entscheidung aus. Eine bloße Beobachtung ohne
anstehende Entscheidung gehört auf den Bogen in `docs/probelauf.md`, nicht
hierher.

## Fachlich

**Kalibrierung der drei Parameter (3.1) am echten Spielabend.**
Startguthaben 1000, Mindesteinsatz 25, Strafe 25 sind gesetzt und
implementiert, aber noch nicht an einem echten Abend gegen das tatsächliche
Spielgefühl geprüft (`probelauf.md`). Bis dahin gelten sie als
vorläufig, auch wenn sie in `anforderungen.md` schon als feste Werte stehen.

**Länge des Wettfensters je Wette.**
Die 15 Sekunden aus Anforderung 5 gelten für alle Wetten gleich. Ob ein Kick
ein kürzeres und der Drive-Ausgang ein längeres Fenster braucht, zeigt sich
erst am Spielabend. Bis dahin bleibt es bei einem Wert für alle.

## Technisch

**Verhalten bei sehr kleiner Runde.**
Bei drei bis vier Spielern ist die Varianz hoch. Bewusst als Feature
akzeptiert; falls es zu wild wird, wären eine Mindestteilnehmerzahl pro
Ausgang oder ein kleiner Grundpool denkbare Stellschrauben. Aktuell nicht
umgesetzt.

## Nicht offen — bewusst ausgeschlossen

Damit diese Fragen nicht versehentlich wieder aufgemacht werden: aktiv aus
dem Scope genommen, nicht (mehr) gebaut, und das soll so bleiben.

- Kein Remote-Play über mehrere Orte.
- Keine mehreren parallelen Räume.
- Keine Persistenz über Spielabende hinweg, keine Datenbank. Innerhalb
  eines Abends dagegen schon: Ein Snapshot übersteht seit ADR-023 einen
  Neustart (entschieden am 2026-08-02, umgesetzt am selben Tag). Der
  Ausschluss meint ab jetzt ausdrücklich die Abende, nicht den einzelnen
  Neustart — und weiterhin keine Datenbank.
- Keine automatische Ergebnis-Erkennung per Datenfeed. Der Host löst manuell
  auf — bewusst, um die Broadcast-Verzögerung zu umgehen und synchron zum
  Fernsehbild im Raum zu bleiben.
- Kein echtes Geld.
- Der Begriff „Markt" (ADR-022). Es heißt Wette.
- Was passiert, wenn die offene Wette nicht mehr zum Spiel passt: Der Host
  annulliert die Runde (Anforderung 8.6).
