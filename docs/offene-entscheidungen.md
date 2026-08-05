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

**Die Host-Rolle hat keine Anforderung.**
Aufgefallen beim Zerlegen der Anforderungen in Anhang A (2026-08-05). Dass
der erste Beitretende Host wird und die Rolle bei Verlust der Verbindung
weiterwandert, ist fachliches Verhalten, steht aber nur in ADR-016 und
ADR-021. Abschnitt 10 der Anforderungen sagt lediglich, *was* der Host darf,
nicht *wer* es wird. Damit gibt es dafür keine ID in Anhang A und keine
Feature-Abdeckung, obwohl das Verhalten umgesetzt und beobachtbar ist.

Nach `teststrategie.md` (9.2) wird eine solche Lücke nicht aus dem Code
beantwortet, sondern vom Fachexperten. Zu klären ist, ob die Regel als
Anforderung nachgetragen wird (dann mit IDs in Anhang A und Szenarien) oder
ob sie bewusst eine rein technische Festlegung bleiben soll.

## Technisch

**Verhalten bei sehr kleiner Runde.**
Bei drei bis vier Spielern ist die Varianz hoch. Bewusst als Feature
akzeptiert; falls es zu wild wird, wären eine Mindestteilnehmerzahl pro
Ausgang oder ein kleiner Grundpool denkbare Stellschrauben. Aktuell nicht
umgesetzt.

**Wake Lock.**
Als Idee notiert (Screen wach halten während einer Runde), noch nicht
implementiert.

## Nicht offen — bewusst ausgeschlossen

Damit diese Fragen nicht versehentlich wieder aufgemacht werden:

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
