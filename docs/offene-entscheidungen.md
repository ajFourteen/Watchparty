# Offene Entscheidungen

Diese Punkte sind bewusst noch nicht entschieden. Bitte nicht stillschweigend
festlegen — bei Bedarf nachfragen und danach hier sowie in `docs/adrs.md`
bzw. `docs/anforderungen.md` nachziehen.

## Fachlich

**Zahlenwerte für Startguthaben, Mindesteinsatz und Strafe.**
Noch nicht kalibriert. Randbedingungen: Die Strafe sollte ungefähr in der
Größenordnung des Mindesteinsatzes liegen, damit Aussitzen strikt dominiert
ist (gleicher Preis wie ein Min-Tipp, aber ohne Gewinnchance). Die Parameter
sollten so gewählt sein, dass ein echter Bankrott an einem Abend selten ist.
Am besten am realen Spielgefühl justieren.

**Grenzfall bei der Anteils-Regel.**
Jeder Gewinner zählt mindestens mit dem Anteil, der dem Mindesteinsatz
entspricht. Der Fall, dass der garantierte Mindest-Anteil größer ist als der
tatsächliche Einsatz eines Gewinners, ist so gemeint: Dann zählt der
Mindest-Anteil, sonst der Einsatz. Diese Lesart ist noch nicht ausdrücklich
bestätigt.

**Weitere Markttypen.**
Nur „Ausgang des nächsten Drives" ist definiert. Welche Märkte als Nächstes
kommen und wie ihre Optionen aussehen, ist offen. Die Struktur ist laut
ADR-017 darauf vorbereitet.

## Technisch

**Host-Rolle nach Reconnect.**
Verliert der Host die Verbindung, wandert die Rolle weiter (ADR-016). Kommt
er per Token zurück, bekommt er sie derzeit nicht zurück. Alternative: die
Rolle klebt am Token. Beides vertretbar, noch nicht entschieden.

**Zustandsautomat.**
`IDLE → OPEN → CLOSED → RESOLVED → IDLE` ist als Richtung gesetzt, aber
weder die erlaubten Ereignisse je Zustand noch das JSON-Nachrichtenschema
sind ausformuliert. Das ist der nächste Arbeitsschritt.

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
