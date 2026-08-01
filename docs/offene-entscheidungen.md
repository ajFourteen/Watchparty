# Offene Entscheidungen

Diese Punkte sind bewusst noch nicht entschieden. Bitte nicht stillschweigend
festlegen — bei Bedarf nachfragen und danach hier sowie in `docs/adrs.md`
bzw. `docs/anforderungen.md` nachziehen.

## Fachlich

**Kalibrierung der drei Parameter (3.1) am echten Spielabend.**
Startguthaben 1000, Mindesteinsatz 25, Strafe 25 sind gesetzt und
implementiert, aber noch nicht an einem echten Abend gegen das tatsächliche
Spielgefühl geprüft (mvp-plan.md, Etappe 6). Bis dahin gelten sie als
vorläufig, auch wenn sie in `anforderungen.md` schon als feste Werte stehen.

**Weitere Markttypen.**
Nur „Ausgang des nächsten Drives" ist definiert. Welche Märkte als Nächstes
kommen und wie ihre Optionen aussehen, ist offen. Die Struktur ist laut
ADR-017 darauf vorbereitet.

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
- Keine Persistenz über Spielabende hinweg, keine Datenbank.
- Keine automatische Ergebnis-Erkennung per Datenfeed. Der Host löst manuell
  auf — bewusst, um die Broadcast-Verzögerung zu umgehen und synchron zum
  Fernsehbild im Raum zu bleiben.
- Kein echtes Geld.
