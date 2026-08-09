# Offene Entscheidungen

Diese Punkte sind bewusst noch nicht entschieden. Bitte nicht stillschweigend
festlegen — bei Bedarf nachfragen und danach hier sowie in `docs/adrs.md`
bzw. `docs/anforderungen.md` nachziehen.

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

## Nicht offen — entschieden und umgesetzt

Anders als oben: Diese Fragen waren offen und sind jetzt geklärt, weil das
Feature bzw. der Fix umgesetzt wurde — kein Ausschluss, sondern eine
gefallene Entscheidung.

- 3.1-a: Das Startguthaben stand getrennt von Mindesteinsatz und Strafe
  (`Room.STARTING_POINTS` statt `Params`). Beim Nachrüsten der Szenarien für
  Anhang A aufgefallen, entschieden am 2026-08-06: `Params` ist jetzt die
  einzige Quelle für alle drei Werte, `Room.addPlayer` nimmt das
  Startguthaben als Parameter statt einer eigenen Konstante.
- Wake Lock: entschieden am 2026-08-09, umgesetzt am selben Tag (ADR-032,
  `docs/features/001-wake-lock.md`). Best effort, ohne automatisierten Test
  (`teststrategie.md` §11). Ob er die Beobachtung aus ADR-021 (Host-Rolle
  wandert unbemerkt) tatsächlich entschärft, bleibt offen bis zum ersten
  Probelauf — das ist keine neue offene Entscheidung, sondern dieselbe
  Beobachtung, die schon in `probelauf.md` unter „Handys" steht.
