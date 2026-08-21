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

**Jeder Eintrag nennt seinen Spielmodus** — Live-Wetten, Tippspiel oder beide
(`anforderungen.md`, „Zwei Spielmodi"). Das gilt gerade für die Ausschlüsse
unten: Mehrere davon sind seit dem Beschluss zum Tippspiel nur noch für die
Live-Wetten wahr, und ein Ausschluss, der zu weit greift, sperrt die falsche
Frage.

## Fachlich

**Kalibrierung der drei Parameter (3.1) am echten Spielabend.** (Live-Wetten)
Startguthaben 1000, Mindesteinsatz 25, Strafe 25 sind gesetzt und
implementiert, aber noch nicht an einem echten Abend gegen das tatsächliche
Spielgefühl geprüft (`probelauf.md`). Bis dahin gelten sie als
vorläufig, auch wenn sie in `anforderungen.md` schon als feste Werte stehen.

**Länge des Wettfensters je Wette.** (Live-Wetten)
Die 15 Sekunden aus Anforderung 5 gelten für alle Wetten gleich. Ob ein Kick
ein kürzeres und der Drive-Ausgang ein längeres Fenster braucht, zeigt sich
erst am Spielabend. Bis dahin bleibt es bei einem Wert für alle.

**Spieltags-Report per Mail: Auslöser, Empfängerkreis, Abmeldung.** (Tippspiel)
Dass es den Spieltags-Report nicht nur als Seite, sondern auch per Mail geben
soll, steht seit dem 2026-08-21 fest. Offen ist dreierlei. Erstens der
Auslöser: Seit dem ADR-037-Nachtrag gibt es keinen internen Nachführ-Job
mehr, der Feed kommt über einen täglichen GitHub-Actions-Relay — ein
Versand hinge also an demselben Relay, an einem zweiten Auslöser oder am
Übergang des letzten Spiels eines Spieltags auf FINAL. Zweitens der
Empfängerkreis: alle Mitglieder einer Liga, nur wer getippt hat, oder nur
wer den Versand bestellt hat. Drittens die Abmeldung: Ohne sie wäre der
Report ungefragter Versand an eine personenbezogene Adresse (13.8). Bis das
entschieden ist, entsteht der Report als Seite zum Abruf.

## Technisch

**Verhalten bei sehr kleiner Runde.** (Live-Wetten)
Bei drei bis vier Spielern ist die Varianz hoch. Bewusst als Feature
akzeptiert; falls es zu wild wird, wären eine Mindestteilnehmerzahl pro
Ausgang oder ein kleiner Grundpool denkbare Stellschrauben. Aktuell nicht
umgesetzt.

## Nicht offen — bewusst ausgeschlossen

Damit diese Fragen nicht versehentlich wieder aufgemacht werden: aktiv aus
dem Scope genommen, nicht (mehr) gebaut, und das soll so bleiben.

- **(Live-Wetten)** Kein Remote-Play über mehrere Orte — das gilt je
  Watchparty (ADR-033), nicht mehr für die Anwendung insgesamt. Das Tippspiel
  ist ortsunabhängig; dort gibt es keinen gemeinsamen Fernseher.
- **(Live-Wetten)** Keine Persistenz über Spielabende hinweg, keine Datenbank.
  Innerhalb eines Abends dagegen schon: Ein Snapshot übersteht seit ADR-023
  einen Neustart (entschieden am 2026-08-02, umgesetzt am selben Tag). Der
  Ausschluss meint ausdrücklich die Abende, nicht den einzelnen Neustart. Er
  meint ebenso ausdrücklich **nur die Live-Wetten**: Das Tippspiel hat eine
  eigene Datenbank (Feature 005), und der Raumzustand bleibt trotzdem im
  Arbeitsspeicher — die Datenbank ist kein Angebot, ihn dorthin zu verlegen.
- **(Live-Wetten)** Keine automatische Ergebnis-Erkennung per Datenfeed. Der
  Host löst manuell auf — bewusst, um die Broadcast-Verzögerung zu umgehen und
  synchron zum Fernsehbild im Raum zu bleiben. Für das Tippspiel trägt diese
  Begründung nicht: Ein Endergebnis nach Spielschluss wartet auf niemanden im
  Wohnzimmer und kommt aus dem Feed.
- **(beide)** Kein echtes Geld.
- **(beide)** Der Begriff „Markt" (ADR-022). Es heißt Wette. Im Tippspiel
  heißt es Ergebnistipp, Wertung und Rangliste — die Begriffe der Live-Wetten
  werden dort nicht ein zweites Mal mit anderer Bedeutung verwendet.
- **(Live-Wetten)** Was passiert, wenn die offene Wette nicht mehr zum Spiel
  passt: Der Host annulliert die Runde (Anforderung 8.6).
