# Offene Entscheidungen

Diese Punkte sind bewusst noch nicht entschieden. Bitte nicht stillschweigend
festlegen — bei Bedarf nachfragen und danach hier sowie in `docs/adrs.md`
bzw. `docs/anforderungen.md` nachziehen.

## Fachlich

**Weitere Markttypen.**
Nur „Ausgang des nächsten Drives" ist definiert. Welche Märkte als Nächstes
kommen und wie ihre Optionen aussehen, ist offen. Die Struktur ist laut
ADR-017 darauf vorbereitet.

## Technisch

**JSON-Nachrichtenschema.**
Die erlaubten Ereignisse je Zustand stehen jetzt in ADR-020, das konkrete
Schema der Nachrichten ist daraus noch abzuleiten. Richtung steht in
`mvp-plan.md`, Etappe 4: ein vollständiges `STATE` mit phasenabhängigem
Inhalt, dazu ein gezieltes `YOUR_BET` an die einzelne Session, damit der
eigene Tipp sichtbar ist, ohne die verdeckte Phase aufzubrechen.

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
