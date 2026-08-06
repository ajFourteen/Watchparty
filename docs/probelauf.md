# Der erste Probelauf

Alles, was am Schreibtisch entschieden werden konnte, ist entschieden und
gebaut. Was jetzt noch offen ist, lässt sich nur an einem echten Spielabend
klären: ob sich die Zahlen aus Anforderung 3.1 richtig anfühlen, ob 15
Sekunden reichen und ob der Host nebenbei überhaupt bedienen kann, was wir
ihm hingelegt haben.

Diese Datei ist die Vorbereitung und der Beobachtungsbogen für diesen Abend.
Fachliche Grundlage ist `anforderungen.md`, technische `adrs.md`. Was der
Abend ergibt, wird danach in `offene-entscheidungen.md` gestrichen und in die
Quelldokumente übernommen.

„Worauf zu achten ist" führt genau die Fragen, die in Anhang A von
`anforderungen.md` als `beobachtung` markiert sind (Stand: 3.1-b, 5-f) —
jede solche Zeile bekommt hier einen eigenen Abschnitt, sonst driften die
beiden Listen auseinander. Die übrigen Abschnitte (Wettkatalog, Annullieren,
kleine Runde, Handys) sind zusätzliche, nicht in Anhang A verzeichnete
Beobachtungen für denselben Abend.

## Vorher

- **Deployment steht.** Über die echte Domain öffnen, nicht über localhost —
  die Wake-Lock-API und der WebSocket-Upgrade hängen an TLS (ADR-002, ADR-018).
- **Ein Test mit mehreren echten Handys.** Die automatisierten Reconnect-Tests
  decken jede Phase ab (`ReconnectTest`), aber Tab-Suspend und
  Bildschirmsperre auf iOS und Android lassen sich nicht simulieren. Zwei bis
  drei verschiedene Geräte reichen, um die Eigenheiten zu sehen.
- **Der Host weiß, was er tut.** Er wählt die Wette und öffnet so, dass die 15
  Sekunden *vor* dem Snap ablaufen. Das ist die einzige Regel, die nicht im
  Code steht, sondern am Tisch gilt.
- **Kurz erklären oder erklären lassen.** Die Kurzanleitung geht beim ersten
  Beitreten von selbst auf. Ob das reicht, ist selbst eine Beobachtung.

## Worauf zu achten ist

### Die drei Parameter (Anforderung 3.1, Anhang A 3.1-b)

Startguthaben 1000, Mindesteinsatz 25, Strafe 25. Die Herleitung war: 40
Mindesteinsätze Puffer bei etwa 25 Drives pro Abend, und Strafe gleich
Mindesteinsatz macht Aussitzen strikt dominiert.

- Steht am Ende des Abends jemand bei null? Wie hat es sich für ihn angefühlt
  — wieder reingekommen oder abgehängt?
- Wird der Einsatz überhaupt erhöht, oder tippt faktisch jeder den
  Mindesteinsatz? Wenn der Regler nie bewegt wird, ist der Mindesteinsatz zu
  nah am Startguthaben oder die Auszahlung zu flach.
- Tut die Strafe weh genug, dass niemand eine Runde auslässt, ohne es zu
  merken?

### Das Wettfenster (Anhang A 5-f)

15 Sekunden, für alle Wetten gleich.

- Kommen alle rechtzeitig durch, oder tippt regelmäßig jemand ins Leere?
- Braucht der Kick ein kürzeres und der Drive-Ausgang ein längeres Fenster?
  Steht als offene Frage in `offene-entscheidungen.md`.
- Wie oft braucht der Host die Notbremse?

### Der Wettkatalog (Anforderung 4)

Vier Wetten: Drive-Ausgang, Big Play, Field Goal, Versuch nach dem Touchdown.

- Ist die Auswahl zu lang? Der Host hat vor dem Snap keine Sekunden zu
  verschenken. Wenn er sucht statt tippt, ist der Katalog zu groß oder falsch
  sortiert.
- Werden alle vier benutzt, oder läuft faktisch nur der Drive-Ausgang?
- **Big Play:** Sind Lauf ab 20, Pass ab 30, Return ab 50 Yards am Tisch
  eindeutig, oder wird über Yards gestritten?
- **Field Goal:** Wie oft öffnet der Host sie und das Team geht dann doch auf
  den vierten Versuch? Dafür gibt es den Abbruch (Anforderung 8.6) — aber wenn
  er jedes zweite Mal nötig ist, stimmt etwas mit der Wette nicht, nicht mit
  dem Knopf.

### Runde annullieren

- Wie oft wird der Abbruch gebraucht, und wie oft aus Versehen gedrückt? Er
  sitzt bewusst unscheinbar unter „Jetzt schließen".
- Verstehen die Spieler, was passiert ist? Auf ihren Handys steht nur, dass
  der Host abgebrochen hat.

### Kleine Runde

Bei drei bis vier Spielern ist die Varianz hoch. Bewusst als Feature
akzeptiert (ADR-001) — aber wenn eine einzige Runde das Leaderboard
umwirft und das den Abend killt statt ihn zu tragen, wären eine
Mindestteilnehmerzahl pro Ausgang oder ein kleiner Grundpool die
Stellschrauben.

### Handys

- Wie oft fällt jemand raus, ohne es zu merken?
- Greift die Pausenregel nach der dritten verpassten Runde sichtbar richtig
  (Anforderung 8.1) — zahlt das eingeschlafene Handy, aber blutet der früh
  Gegangene nicht aus?
- Wandert die Host-Rolle unbemerkt weg, wenn jemand sein Handy sperrt
  (ADR-021)? Das ist der Fall, für den das Wake Lock notiert ist.

## Danach

- Ergebnisse in `offene-entscheidungen.md` eintragen und die dort geklärten
  Punkte streichen.
- Geänderte Parameter in `anforderungen.md` nachziehen — sie stehen im Code an
  genau einer Stelle (`Params.DEFAULT`).
- Was eine echte Entscheidung war, bekommt einen ADR.
