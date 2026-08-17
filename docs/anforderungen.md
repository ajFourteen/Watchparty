# Fachliche Anforderungen — Live-Wetten zur Watchparty und Tippspiel über die Saison

## Zwei Spielmodi

Die Anwendung trägt zwei gleichwertige Spielmodi. Keiner ist der Normalfall,
von dem der andere abweicht:

- **Live-Wetten** — der Abend vor dem Fernseher: Kapitel 1 bis 10, dazu die so
  vermerkten Punkte in 11 und 12. Das ist alles, was heute gebaut ist.
- **Tippspiel** — die Liga über eine ganze Saison: Kapitel 13, dazu die so
  vermerkten Punkte in 11 und 12. Beschlossen am
  2026-08-17 (`docs/features/005-tippspiel-liga.md`), noch nicht gebaut. Die
  Abschnitte entstehen stufenweise mit der Umsetzung, nicht vorab — eine Regel
  mit der Marke `backend` ohne grünes Szenario ist ein Fehlschlag im Build
  (`teststrategie.md` 7.1).

**Keine Anforderung ohne Geltungsvermerk.** Jedes Kapitel nennt seinen
Spielmodus direkt unter der Überschrift, jede Regel in Anhang A in einer
eigenen Spalte: `Live-Wetten`, `Tippspiel` oder `beide`. Wo die Punkte eines
Kapitels sich unterscheiden, trägt jeder Punkt seinen eigenen Vermerk.
Unterabschnitte teilen die Geltung ihres Kapitels, solange sie keine eigene
nennen — das ist die einzige Vererbung, und sie steht hier, statt sich von
selbst zu verstehen.

Dass mehr Kapitel `Live-Wetten` tragen als `Tippspiel`, ist Baureihenfolge und
keine Rangordnung. Es heißt auch nicht, dass die Live-Wetten der Grundfall
wären, von dem das Tippspiel Ausnahmen macht: Für die Regeln des einen Modus
ist der andere schlicht nicht zuständig.

Die Nummerierung bleibt davon unberührt. Die IDs aus Anhang A sind aus dem
Code, den ADRs und `probelauf.md` verlinkt und werden nicht umgeschrieben, nur
weil ein zweiter Spielmodus dazukommt.

## 1. Zweck und Kontext

*Geltung: Live-Wetten.*

Die App richtet sich an Freunde, die gemeinsam vor Ort ein Football-Spiel schauen und das Zuschauen spannender machen wollen. Über ihre Handys wetten sie live auf Football-Ereignisse — der Ausgang des nächsten Drives, ein Big Play, ein einzelner Kick. Es geht um Spaß und ein gemeinsames Punkte-Ranking, nicht um echtes Geld.

**Rahmenbedingungen:**
- Alle Teilnehmer sitzen vor demselben Fernseher (nur Vor-Ort-Nutzung, kein Remote-Play).
- Mehrere, voneinander getrennte Watchpartys können gleichzeitig laufen; innerhalb einer Watchparty läuft immer nur eine Runde gleichzeitig.
- Keine Persistenz über Spielabende hinweg. Jeder Abend beginnt frisch. Ein
  Snapshot übersteht seit ADR-023 einen Neustart *innerhalb* desselben
  Abends (Deploy, Absturz) — das ändert an dieser Anforderung nichts, er
  verfällt spätestens nach sechs Stunden.
- Teilnahme ohne Installation und ohne Account: Link öffnen, Name eingeben, dabei.

## 2. Wett-Grundprinzip (Pari-mutuel / Totalisator)

*Geltung: Live-Wetten.*

Es gibt keinen Buchmacher und kein Wahrscheinlichkeitsmodell. Die Wett-Ökonomie funktioniert nach dem Totalisator-Prinzip:

- Alle Einsätze einer Runde wandern in einen gemeinsamen Pool.
- Wer richtig liegt, teilt sich den Pool.
- Die Auszahlung entsteht aus dem tatsächlichen Wettverhalten der Gruppe: Ein Ausgang, den kaum jemand getippt hat, zahlt viel; ein Ausgang, auf den alle setzen, zahlt wenig. Damit belohnt das System automatisch, das Spiel besser zu lesen als die Mitspieler — „unwahrscheinlich → mehr Punkte" ergibt sich von selbst.

**Nullsummen-Eigenschaft:** Punkte entstehen und verschwinden nicht, sie werden nur umverteilt. Der Pool ist stets exakt die Summe aller Einsätze plus aller Strafen. Diese Eigenschaft ist bindend.

## 3. Spieler und Punktekonten

*Geltung: Live-Wetten.*

- Jeder Spieler startet mit einem festen Punkte-Startguthaben.
- Punkte sind ganzzahlig. Es gibt keine Bruchteile von Punkten.
- Ein Leaderboard zeigt die aktuellen Kontostände.
- Ein Konto wird nie negativ.

### 3.1 Parameter

| Parameter | Wert |
|---|---|
| Startguthaben | 1000 |
| Mindesteinsatz | 25 |
| Nicht-Tipper-Strafe | 25 |

Das sind 40 Mindesteinsätze Puffer bei etwa 25 Drives pro Abend; ein echter Bankrott ist damit unwahrscheinlich. Strafe gleich Mindesteinsatz sorgt dafür, dass Aussitzen strikt dominiert ist: gleicher Preis wie ein Mindest-Tipp, aber ohne Gewinnchance. Ein Einsatz von 100 bis 200 ist damit eine sichtbare Ansage.

Die Werte sind am realen Spielgefühl zu justieren; sie stehen an einer Stelle im Code, nicht verstreut.

## 4. Wetten

*Geltung: Live-Wetten.*

- Eine **Wette** ist fachlich eine **Frage mit einer festen Liste möglicher Ausgänge** und einer späteren Auflösung. Sie wird als eigenständige Struktur behandelt, nicht als Sonderfall im Code, damit weitere Wetten ohne Umbau ergänzt werden können.
- Der Wettkatalog steht auf dem Server. Der Host wählt beim Öffnen aus, welche Wette läuft; welche gerade passt, sieht nur er vor dem Fernseher.
- Die Ausgänge einer Wette sind **lückenlos und überschneidungsfrei**: Jeder reale Verlauf fällt in genau einen Eimer. Wo das nicht offensichtlich ist, gehört die Abgrenzung als Anmerkung dazu — der Host löst von Hand auf und braucht eine Regel, über die am Tisch nicht gestritten wird. Diese Anmerkungen müssen in der Oberfläche sichtbar sein.

### 4.1 Ausgang des nächsten Drives

Die Grundwette, die über den Abend am häufigsten läuft.

| Ausgang | Anmerkung |
|---|---|
| Touchdown | |
| Field Goal | nur bei erfolgreichem Kick |
| Punt | |
| Turnover | Interception oder verlorener Fumble |
| Turnover on Downs | umfasst auch den verschossenen Field Goal (Gegner übernimmt am Ort) |
| Safety | |
| End of Half / Game | Drive läuft mit Halbzeit- oder Spielende aus |

### 4.2 Big Play im nächsten Drive?

Ja / Nein. **Big Play = ein einzelner Spielzug mit Lauf ab 20, Pass ab 30 oder Return ab 50 Yards.**

Die drei Schwellen sind eine gesetzte Konvention, keine Liga-Statistik: Sie sind am Tisch merkbar und werden im Fernsehen eingeblendet. Getrennt nach Spielzugart, weil zwanzig Yards am Boden etwas anderes wert sind als zwanzig durch die Luft.

### 4.3 Field Goal: gut?

Gut / Kein Field Goal. Verschossen oder geblockt zählt als „Kein Field Goal".

### 4.4 Versuch nach dem Touchdown?

| Ausgang | Anmerkung |
|---|---|
| Extrapunkt gut | der Kick sitzt, 1 Punkt |
| Extrapunkt vergeben | verschossen, geblockt oder durch Strafe vertan |
| Two-Point gut | 2 Punkte |
| Two-Point gescheitert | auch wenn die Verteidigung den Ball zurückträgt |

**Kick und Two-Point sind eine Wette, nicht zwei.** Beim Öffnen weiß niemand, welche Variante kommt — genau das ist die Frage. Zwei getrennte Wetten hätten den Host gezwungen, die Entscheidung des Teams vorwegzunehmen; läge er falsch, gäbe es keinen passenden Ausgang. Nebeneffekt: Die beiden Two-Point-Ausgänge werden selten getippt und zahlen deshalb gut.

**Verantwortung des Hosts:** Die Field-Goal-Wette setzt weiterhin die Situation voraus, auf die sie sich bezieht — sie gehört auf den Field-Goal-Versuch, nicht auf gut Glück.

## 5. Wettfenster und Timing

*Geltung: Live-Wetten.*

- Der **Host** entscheidet, welche Wette wann öffnet.
- Nach dem Öffnen bleibt das Fenster **15 Sekunden** offen und schließt dann automatisch.
- Zusätzlich hat der Host einen **„Jetzt schließen"-Knopf** als Notbremse. Das Fenster schließt bei Ablauf der 15 Sekunden **oder** beim Host-Klick — je nachdem, was zuerst eintritt.
- Das Fenster schließt außerdem **sofort, sobald alle Teilnehmer der Runde getippt haben**: Es kommt kein Tipp mehr, und weiter zu warten verzögert nur die Aufdeckung. Maßgeblich ist der beim Öffnen eingefrorene Teilnehmerkreis (8.1) — wer erst danach beigetreten ist, hält die Runde nicht auf.
- Verantwortung des Hosts: das Fenster so öffnen, dass die 15 Sekunden **vor dem Snap** ablaufen. Danach läuft der Spielzug, es wird nicht mehr getippt.

## 6. Wettmechanik

*Geltung: Live-Wetten.*

- **Ein Tipp pro Spieler pro Runde.** Kein Aufteilen des Einsatzes auf mehrere Ausgänge, kein Nachbessern.
- **Wetten sind verdeckt**, solange das Fenster offen ist. Während der offenen Phase ist nur sichtbar, *wie viele* schon getippt haben, nicht *was*.
- Es gibt einen **Mindesteinsatz**. Der Mindesteinsatz ist der Standard-Einsatz: Ein einzelner Tipp auf einen Ausgang setzt automatisch den Mindesteinsatz. Wer will, erhöht den Einsatz vor dem Bestätigen.
- Einsätze sind beliebige ganze Zahlen ab dem Mindesteinsatz bis zum eigenen Kontostand.
- **Spieler mit weniger Punkten als dem Mindesteinsatz** können trotzdem mitwetten und gehen dabei zwangsweise All-in (auch mit 0 Punkten, siehe 8.3).

## 7. Auszahlung

*Geltung: Live-Wetten.*

Die Auszahlung trennt zwei Dinge: die **echten Punkte** im Pool und die **Anteile**, nach denen der Pool verteilt wird.

### 7.1 Anteile statt reiner Einsätze

- Für die Verteilung zählt jeder Gewinner mindestens mit dem Anteil, der dem Mindesteinsatz entspricht — **auch wenn er weniger oder 0 Punkte gesetzt hat**. So bekommt auch ein Spieler mit 0 Punkten eine echte Auszahlung und kann sich erholen.
- Wer mehr als den Mindesteinsatz gesetzt hat, erhält entsprechend mehr Anteile.
- Als Formel: **Anteil = max(Einsatz, Mindesteinsatz)**. Ist der garantierte Mindest-Anteil größer als der tatsächliche Einsatz, zählt der Mindest-Anteil, sonst der Einsatz.
- Dadurch sind „gesetzte Punkte" und „Anteile am Gewinn" entkoppelt. Der Pool aus echten Punkten bleibt fix; die Anteile bestimmen nur die Aufteilung. Die Nullsumme bleibt erhalten — größere Scheiben Einzelner gehen zulasten der anderen Gewinner, nicht aus dem Nichts.

### 7.2 Ganzzahlige Verteilung

- Auszahlungen werden ganzzahlig verteilt.
- Der beim Teilen entstehende Rest wird nach dem **Größte-Reste-Verfahren (Hamilton)** vergeben: Jeder Gewinner erhält seinen abgerundeten Anteil; die übrigen einzelnen Punkte gehen an die Gewinner mit dem größten Nachkomma-Rest. Die Summe der Auszahlungen entspricht damit exakt dem Pool.

## 8. Strafen, Sonder- und Randfälle

*Geltung: Live-Wetten.*

### 8.1 Nicht-Tipper-Strafe
- Wer in einer Runde gar nicht tippt, zahlt eine kleine Strafe, die in den Pool fließt.
- Die Strafe trifft jeden im Raum, der nicht getippt hat — unabhängig vom Grund (auch bei eingeschlafenem Handy). Innerhalb des 15-Sekunden-Fensters ist der Grund nicht unterscheidbar.

**Wer zum Teilnehmerkreis gehört, wird beim Öffnen der Wette eingefroren.** Wer während des offenen Fensters dazukommt, darf tippen und gewinnen, wird aber nicht bestraft. Niemand zahlt für eine Runde, die schon lief, als er kam.

**Die Strafe wird auf den Kontostand gekappt.** Eingesammelt wird `min(Strafe, Kontostand)`; der Pool besteht aus dem, was tatsächlich eingesammelt wurde. Damit bleibt die Nullsumme exakt erhalten und ein Konto wird nie negativ. Ein Spieler bei 0 Punkten zahlt faktisch nichts mehr — die Strafe darf die Null nicht doch zu einem absorbierenden Zustand machen (siehe 8.3).

**Ein getrennter Spieler pausiert ab der dritten verpassten Runde.** Er zahlt für die erste und zweite verpasste Runde die Strafe; danach fällt er aus dem Teilnehmerkreis und zahlt nicht mehr. Bei Reconnect ist er sofort wieder dabei, der Zähler beginnt von vorn. Damit zahlt das eingeschlafene Handy weiterhin — Wegdösen ist nicht die günstigste Strategie —, aber wer früh nach Hause geht, blutet nicht über zwanzig Runden aus und verzerrt das Leaderboard. Die Pause greift ausdrücklich nur bei getrennter Verbindung: Wer verbunden ist und nicht tippt, zahlt jede Runde.

### 8.2 Verteilung bei „niemand liegt richtig" (Push)
- Tippt niemand den Gewinner-Ausgang, gibt es keine Gewinner. Alle Wetter bekommen ihren Einsatz zurück.
- Die eingezahlten **Strafen** werden in diesem Fall anteilig auf **alle Spieler verteilt, die überhaupt getippt haben** (egal ob richtig oder falsch). Auf einem Push ist „überhaupt getippt zu haben" die einzige belohnbare Leistung.
- „Anteilig" meint dieselben Anteile wie in 7.1, also `max(Einsatz, Mindesteinsatz)`, Rest nach dem Größte-Reste-Verfahren. Eine Anteilsdefinition für beide Fälle statt zweier — und der All-in-Spieler mit 0 Punkten bekommt auch beim Push etwas ab.

### 8.3 Spieler mit 0 Punkten
- Auch mit 0 Punkten darf jeder mitwetten (All-in mit 0).
- Über die Mindest-Anteils-Regel (7.1) erhält auch ein solcher Spieler bei richtigem Tipp eine echte Auszahlung und kann so zurück ins Spiel kommen. Die Null ist damit **kein** absorbierender Zustand.

### 8.4 Niemand tippt
- Tippt in einer Runde überhaupt niemand, wird die Runde annulliert: keine Strafen, keine Auszahlung (mangels Empfänger).

### 8.5 Alle tippen denselben, richtigen Ausgang
- Der Pool besteht dann nur aus den eigenen Einsätzen der Gewinner; jeder bekommt näherungsweise seinen Einsatz zurück (netto ≈ null). Das ist gewollt: In dieser Situation gab es nichts zu gewinnen.

### 8.6 Runde annullieren (Host)

Der Host kann eine laufende Runde abbrechen, solange sie **offen oder geschlossen** ist. Dann passiert nichts: keine Einsätze, keine Strafen, keine Auszahlung, kein Eintrag auf dem Verpasste-Runden-Zähler. Die Runde hat nicht stattgefunden.

Das ist der Ausweg für den Fall, dass die offene Wette nicht mehr zum Spiel passt — etwa wenn das Team statt des Field Goals doch auf den vierten Versuch geht. Ohne ihn müsste der Host einen Ausgang wählen, den es nicht gegeben hat.

**Nach dem Auflösen geht es nicht mehr.** Ab da sind die Punkte verrechnet; ein Abbruch wäre eine Rückabwicklung und keine Notbremse. Vorher ist er dagegen billig: Punkte werden ohnehin erst beim Auflösen bewegt, es gibt also nichts zurückzurechnen und die Nullsumme kann gar nicht kaputtgehen.

### 8.7 Raum zurücksetzen (Host)

Der Host kann den gesamten Raum zurücksetzen — anders als 8.6 nicht nur die laufende Runde, sondern alle Spieler, Punktestände und die laufende Runde in einem Schritt, in jeder Phase möglich.

Nötig geworden mit ADR-023: Bisher war ein Server-Neustart implizit das Zurücksetzen des Raums (ADR-004). Seit der Zustand einen Neustart übersteht, braucht der Host einen expliziten Weg dafür — etwa um Testrunden vom Aufbau loszuwerden oder einen doppelt beigetretenen Spieler zu entfernen.

Wer weiterspielen will, tritt mit neuem Namen erneut bei; kein automatisches Wiederbeitreten, sonst wäre das Zurücksetzen nur Anzeige.

**Der Knopf ist bewusst unscheinbar.** Er ist die Ausnahme, und ein Fehlgriff kostet allen die Runde.

## 9. Ablauf einer Runde (fachliche Sicht)

*Geltung: Live-Wetten.*

1. **Leerlauf** — der Host kann eine Wette öffnen.
2. **Öffnen** — Host wählt eine Wette aus dem Katalog und öffnet sie; die 15-Sekunden-Uhr läuft.
3. **Tippen (verdeckt)** — Spieler tippen; sichtbar ist nur die Anzahl der abgegebenen Tipps.
4. **Schließen** — nach 15 Sekunden, per Host-Notbremse oder sobald alle Teilnehmer getippt haben. **Ab jetzt werden alle abgegebenen Tipps offen angezeigt.** Es kann nicht mehr getippt werden. Der Spielzug läuft im Fernsehen.
5. **Auflösen** — der Host wählt den tatsächlichen Ausgang. **Erst jetzt** werden Punkte verrechnet: Pool bilden, Strafen einsammeln, Gewinner nach Anteilen auszahlen, Leaderboard aktualisieren.
6. Zurück zu Leerlauf.

**Wichtig:** Aufdeckung der Tipps erfolgt beim **Schließen** (Schritt 4). Die **Punkte-Verrechnung** erfolgt getrennt davon erst beim **Auflösen** (Schritt 5).

## 10. Rollen

*Geltung: Live-Wetten.*

- **Host:** hat zusätzlich die Steuerknöpfe (Wette auswählen und öffnen, jetzt schließen, Ausgang auflösen, Runde annullieren, Raum zurücksetzen). Ansonsten normaler Spieler.
- **Spieler:** tippen, sehen Countdown, aufgedeckte Tipps, Ergebnisse und Leaderboard.

### 10.1 Wer Host ist

**Host ist immer der am frühesten beigetretene verbundene Spieler.** Beim Start des Abends ist das der erste Beitretende; verliert er die Verbindung, wandert die Rolle weiter, sonst wäre der Raum steuerlos. Es braucht dafür keinen zusätzlichen Einstiegsschritt — keine eigene Host-URL, kein Kennwort. „Erster Beitretender wird Host" gilt damit nicht nur einmal beim Start, sondern dauerhaft, und jede Kombination aus Weggehen und Zurückkommen ist abgedeckt.

Die Übergabe ist asymmetrisch:

- **Verlieren wirkt sofort, in jeder Phase.** Sonst wäre der Raum mitten im offenen Fenster ohne Steuerung — genau dann, wenn jemand schließen können muss.
- **Zurückholen wirkt erst im Leerlauf oder nach dem Auflösen.** Kehrt ein früher beigetretener Spieler während eines offenen oder geschlossenen Fensters zurück, wird die Übergabe vorgemerkt und erst danach ausgeführt. Sonst rutschen dem Vertreter die Steuerknöpfe mitten in einer laufenden Runde weg.

Die Rolle kann über den Abend zwischen den Runden mehrfach wandern, wenn Handys ein- und aufwachen. Sie landet dabei immer bei dem, der am längsten dabei ist — in der Praxis der, der die Fernbedienung hat. Solange ein Spieler beigetreten ist, hält die Oberfläche den Bildschirm über einen Screen Wake Lock wach, um genau dieses unbemerkte Wandern seltener zu machen (ADR-032) — best effort, ohne Fehlermeldung, falls der Browser das nicht unterstützt.

## 11. Bewusst nicht enthalten (out of scope)

*Geltung: je Punkt vermerkt.*

Ein Ausschluss ohne Geltungsvermerk wäre hier besonders teuer: Er sperrt sonst
eine Frage, die für den anderen Spielmodus offen sein muss.

- **(Live-Wetten)** Kein Remote-/Online-Play über mehrere Orte hinweg — jede Watchparty bleibt an einen Ort gebunden (1-a), auch wenn mehrere Watchpartys gleichzeitig laufen können (Feature 004).
- **(Live-Wetten)** Keine Persistenz / keine Saison über mehrere Abende (ADR-023 überbrückt nur einen Neustart innerhalb desselben Abends, mit Verfallszeit).
- **(Live-Wetten)** Keine automatische Ergebnis-Erkennung per Datenfeed; der Host löst manuell auf — bewusst, um die Broadcast-Verzögerung zu umgehen und synchron zum Fernsehbild im Raum zu bleiben.
- **(Tippspiel)** Keine Live-Komponente: kein Tippen während des Spiels, keine Zwischenstände, keine Wette auf einen einzelnen Spielzug. Gewertet wird ausschließlich das Endergebnis. Wer live wetten will, nimmt den anderen Spielmodus.
- **(Tippspiel)** Kein Handeintrag von Spielplan und Ergebnissen als Regelfall; von Hand geht nur die Korrektur eines falschen Ergebnisses. Die Regel dazu entsteht mit Kapitel 13.
- **(beide)** Kein echtes Geld.

## 12. Offene Punkte / spätere Erweiterungen

*Geltung: je Punkt vermerkt.*

- **(Live-Wetten)** Weitere Wetten über die vier aus Abschnitt 4 hinaus.
- **(Live-Wetten)** Nachjustierung der Parameter aus 3.1 am realen Spielgefühl.
- **(Tippspiel)** Das Tippspiel über die Saison ist beschlossen und entsteht
  als Kapitel 13; was dafür nötig ist, steht in
  `docs/features/005-tippspiel-liga.md`.
- **(Tippspiel)** Playoffs: Die erste Saison endet mit der Regular Season. Ob
  die Playoffs danach dazukommen, wird im Januar mit echten Daten entschieden —
  kein dauerhafter Ausschluss, sondern ein verschobener Zuschnitt.

---

## Anhang A: Atomare Regeln und Prüfbarkeit

Der Fließtext oben ist die fachliche Wahrheit; dieser Anhang zerlegt ihn in
einzeln prüfbare Regeln und sagt für jede, **wo sie geprüft wird**. Er ist
die Referenz für die Feature-Abdeckung aus `teststrategie.md`: Jedes
Testszenario zeigt über den Tag `@Anforderung("…")` auf eine ID von hier,
und jede Regel mit der Marke `backend` ohne grünes Szenario ist ein
Fehlschlag im Build.

**Zur Nummerierung.** Bestehende Nummern (3.1, 4.1, 7.2, 8.6 …) bleiben
unverändert — sie sind aus CLAUDE.md, den ADRs und `probelauf.md` verlinkt.
Regeln, die im Fließtext als Aufzählungspunkt ohne eigene Nummer stehen,
bekommen einen Buchstaben-Suffix (`6-a`, `8.1-c`). Beide Formen sind
gleichwertige IDs; die Buchstabenform sagt nur, dass die Regel im Text
keine eigene Überschrift hat. Die Regeln des Tippspiels tragen die Nummern
aus Kapitel 13 (`13.5-a` …) und kommen mit dem Bau dazu.

**Zur Geltung.** Jede Regel nennt ihren Spielmodus: `Live-Wetten`, `Tippspiel`
oder `beide`. Die Spalte ist Pflicht und hat keinen Vorgabewert — eine Regel
ohne Geltung ist eine unfertige Regel, keine Regel für die Live-Wetten. Sie
gilt auch nie stillschweigend für den jeweils anderen Modus. Sie steht **vor**
der Marke, weil beide
Auswerter die Marke als letzte Spalte lesen (`AnhangA` im Testcode und der
Gradle-Task `abdeckung`); eine Spalte dahinter würde die Feature-Abdeckung
stillschweigend leer laufen lassen.

**Zu den Marken.**

| Marke | Bedeutung |
|---|---|
| `backend` | im Backend prüfbar, zählt zur Feature-Abdeckung |
| `frontend` | Sache der Oberfläche, außerhalb der Backend-Teststrategie |
| `organisatorisch` | Verantwortung von Menschen oder des Betriebs; kein Programm kann das prüfen |
| `beobachtung` | erst am echten Spielabend feststellbar; gehört auf den Beobachtungsbogen in `probelauf.md` |

Eine Regel trägt genau eine Marke. Wo eine Aussage im Text zwei Seiten hat —
der Server liefert etwas, die Oberfläche zeigt es —, ist sie hier in zwei
Regeln zerlegt. Die Unschärfe gehört in die Anforderung aufgelöst, nicht in
die Marke.

### 1. Zweck und Kontext

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 1-a | Alle Teilnehmer sitzen vor demselben Fernseher; nur Vor-Ort-Nutzung. | Live-Wetten | organisatorisch |
| 1-b | Innerhalb einer Watchparty läuft immer nur eine Runde gleichzeitig. | Live-Wetten | backend |
| 1-c | Keine Persistenz über Spielabende hinweg; jeder Abend beginnt frisch. | Live-Wetten | backend |
| 1-d | Ein Snapshot übersteht einen Neustart innerhalb desselben Abends und verfällt spätestens nach sechs Stunden. | Live-Wetten | backend |
| 1-e | Der Beitritt verlangt einen Namen und, wer einer bestehenden Watchparty beitritt, deren Code — kein Account, keine Anmeldung. | Live-Wetten | backend |
| 1-f | Teilnahme ohne Installation: Link im Handy-Browser öffnen genügt. | Live-Wetten | frontend |
| 1-g | Ein Beitritt ohne Code erzeugt eine neue Watchparty; wer sie erzeugt, ist ihr Host. | Live-Wetten | backend |
| 1-h | Der Code einer Watchparty ist vierstellig alphanumerisch und wird unabhängig von Groß-/Kleinschreibung angenommen. | Live-Wetten | backend |
| 1-i | Watchpartys sind vollständig getrennt: Keine Nachricht und kein Kommando einer Watchparty wirkt auf eine andere. | Live-Wetten | backend |
| 1-j | Eine Watchparty ohne Aktivität wird nach sechs Stunden verworfen, samt ihrem Snapshot. | Live-Wetten | backend |
| 1-k | Der Code der eigenen Watchparty ist ständig sichtbar. | Live-Wetten | frontend |
| 1-l | `/join/CODE` füllt das Code-Feld vor. | Live-Wetten | frontend |

### 2. Wett-Grundprinzip

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 2-a | Alle Einsätze einer Runde wandern in einen gemeinsamen Pool. | Live-Wetten | backend |
| 2-b | Wer richtig liegt, teilt sich den Pool. | Live-Wetten | backend |
| 2-c | Ein selten getippter Ausgang zahlt pro Gewinner mehr als ein häufig getippter. | Live-Wetten | backend |
| 2-d | Nullsumme: Punkte entstehen und verschwinden nicht. Der Pool ist exakt die Summe aller Einsätze plus der tatsächlich eingesammelten Strafen. | Live-Wetten | backend |

### 3. Spieler und Punktekonten

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 3-a | Jeder Spieler startet mit einem festen Punkte-Startguthaben. | Live-Wetten | backend |
| 3-b | Punkte sind ganzzahlig; es gibt keine Bruchteile. | Live-Wetten | backend |
| 3-c | Der Server liefert die aktuellen Kontostände aller Spieler. | Live-Wetten | backend |
| 3-d | Ein Leaderboard zeigt die Kontostände an. | Live-Wetten | frontend |
| 3-e | Ein Konto wird nie negativ. | Live-Wetten | backend |
| 3.1 | Startguthaben 1000, Mindesteinsatz 25, Nicht-Tipper-Strafe 25. | Live-Wetten | backend |
| 3.1-a | Die drei Werte stehen an einer Stelle im Code, nicht verstreut. | Live-Wetten | backend |
| 3.1-b | Ob die drei Werte sich am realen Spielgefühl bewähren. | Live-Wetten | beobachtung |
| 3.1-c | Der Server nennt dem Client die drei Werte beim Beitritt; der Client hält keine eigene Kopie. | Live-Wetten | backend |

### 4. Wetten

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 4-a | Eine Wette ist eine Frage mit fester Liste möglicher Ausgänge und späterer Auflösung, als eigenständige Struktur — weitere Wetten sind ohne Umbau ergänzbar. | Live-Wetten | backend |
| 4-b | Der Wettkatalog steht auf dem Server; der Host wählt beim Öffnen aus. | Live-Wetten | backend |
| 4-c | Jede Wette hat mindestens zwei Ausgänge mit eindeutigen Kennungen. | Live-Wetten | backend |
| 4-d | Die Ausgänge sind lückenlos und überschneidungsfrei: Jeder reale Verlauf fällt in genau einen Eimer. | Live-Wetten | organisatorisch |
| 4-e | Wo die Abgrenzung nicht offensichtlich ist, liefert der Server die Anmerkung mit. | Live-Wetten | backend |
| 4-f | Die Anmerkungen sind in der Oberfläche sichtbar. | Live-Wetten | frontend |
| 4.1 | Der Katalog enthält „Ausgang des nächsten Drives" mit den sieben genannten Ausgängen und ihren Anmerkungen. | Live-Wetten | backend |
| 4.2 | Der Katalog enthält „Big Play im nächsten Drive?" mit Ja/Nein und der Schwellen-Anmerkung. | Live-Wetten | backend |
| 4.3 | Der Katalog enthält „Field Goal: gut?" mit Gut / Kein Field Goal. | Live-Wetten | backend |
| 4.4 | Der Katalog enthält „Versuch nach dem Touchdown?" mit den vier genannten Ausgängen. | Live-Wetten | backend |
| 4.4-a | Kick und Two-Point sind eine Wette, nicht zwei. | Live-Wetten | backend |
| 4.4-b | Verantwortung des Hosts: Die Field-Goal-Wette gehört auf den Field-Goal-Versuch, nicht auf gut Glück. | Live-Wetten | organisatorisch |

### 5. Wettfenster und Timing

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 5-a | Der Host entscheidet, welche Wette wann öffnet. | Live-Wetten | backend |
| 5-b | Nach dem Öffnen bleibt das Fenster 15 Sekunden offen und schließt dann automatisch. | Live-Wetten | backend |
| 5-c | Der Host hat einen „Jetzt schließen"-Knopf als Notbremse. | Live-Wetten | backend |
| 5-d | Das Fenster schließt bei Ablauf der 15 Sekunden oder beim Host-Klick — je nachdem, was zuerst eintritt. | Live-Wetten | backend |
| 5-e | Verantwortung des Hosts: das Fenster so öffnen, dass die 15 Sekunden vor dem Snap ablaufen. | Live-Wetten | organisatorisch |
| 5-f | Ob 15 Sekunden für alle Wetten die richtige Länge sind. | Live-Wetten | beobachtung |
| 5-g | Das Fenster schließt sofort, sobald alle Teilnehmer des eingefrorenen Kreises getippt haben. | Live-Wetten | backend |
| 5-h | Die Oberfläche benennt beim Schließen, dass alle getippt haben. | Live-Wetten | frontend |

### 6. Wettmechanik

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 6-a | Ein Tipp pro Spieler pro Runde: kein Aufteilen des Einsatzes, kein Nachbessern. | Live-Wetten | backend |
| 6-b | Solange das Fenster offen ist, ist nur sichtbar, *wie viele* getippt haben, nicht *was*. | Live-Wetten | backend |
| 6-c | Ein Tipp ohne ausdrücklichen Einsatz setzt den Mindesteinsatz. | Live-Wetten | backend |
| 6-d | Die Oberfläche erlaubt, den Einsatz vor dem Bestätigen zu erhöhen. | Live-Wetten | frontend |
| 6-e | Einsätze sind ganze Zahlen ab dem Mindesteinsatz bis zum eigenen Kontostand. | Live-Wetten | backend |
| 6-f | Wer weniger Punkte hat als den Mindesteinsatz, darf mitwetten und geht zwangsweise All-in — auch mit 0 Punkten. | Live-Wetten | backend |

### 7. Auszahlung

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 7.1 | Anteil = max(Einsatz, Mindesteinsatz). Jeder Gewinner zählt mindestens mit dem Mindest-Anteil, auch bei weniger oder 0 gesetzten Punkten. | Live-Wetten | backend |
| 7.1-a | Gesetzte Punkte und Anteile am Gewinn sind entkoppelt; größere Scheiben Einzelner gehen zulasten der anderen Gewinner, nicht aus dem Nichts. | Live-Wetten | backend |
| 7.2 | Auszahlungen sind ganzzahlig; der Rest wird nach dem Größte-Reste-Verfahren vergeben, und die Summe entspricht exakt dem Pool. | Live-Wetten | backend |

### 8. Strafen, Sonder- und Randfälle

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 8.1 | Wer in einer Runde gar nicht tippt, zahlt eine Strafe, die in den Pool fließt. | Live-Wetten | backend |
| 8.1-a | Die Strafe trifft jeden im Teilnehmerkreis, der nicht getippt hat — unabhängig vom Grund. | Live-Wetten | backend |
| 8.1-b | Der Teilnehmerkreis wird beim Öffnen eingefroren: Wer während des offenen Fensters dazukommt, darf tippen und gewinnen, wird aber nicht bestraft. | Live-Wetten | backend |
| 8.1-c | Die Strafe wird auf den Kontostand gekappt; eingesammelt wird min(Strafe, Kontostand). | Live-Wetten | backend |
| 8.1-d | Ein getrennter Spieler zahlt für die erste und zweite verpasste Runde und pausiert ab der dritten; bei Reconnect beginnt der Zähler von vorn. | Live-Wetten | backend |
| 8.1-e | Die Pause greift nur bei getrennter Verbindung: Wer verbunden ist und nicht tippt, zahlt jede Runde. | Live-Wetten | backend |
| 8.1-f | Ab dem Schließen nennt der Zustand die Teilnehmer ohne Tipp; solange das Fenster offen ist, nicht. | Live-Wetten | backend |
| 8.1-g | Die Oberfläche zeigt ab dem Schließen hervorgehoben, wer nicht getippt hat, und nennt die Strafe. | Live-Wetten | frontend |
| 8.2 | Tippt niemand den Gewinner-Ausgang, bekommen alle Wetter ihren Einsatz zurück. | Live-Wetten | backend |
| 8.2-a | Beim Push werden die eingezahlten Strafen anteilig auf alle verteilt, die überhaupt getippt haben. | Live-Wetten | backend |
| 8.3 | Auch mit 0 Punkten darf jeder mitwetten und kann über den Mindest-Anteil zurück ins Spiel kommen; die Null ist kein absorbierender Zustand. | Live-Wetten | backend |
| 8.4 | Tippt überhaupt niemand, wird die Runde annulliert: keine Strafen, keine Auszahlung. | Live-Wetten | backend |
| 8.5 | Tippen alle denselben, richtigen Ausgang, bekommt jeder näherungsweise seinen Einsatz zurück. | Live-Wetten | backend |
| 8.6 | Der Host kann eine laufende Runde annullieren, solange sie offen oder geschlossen ist. | Live-Wetten | backend |
| 8.6-a | Beim Annullieren passiert nichts: keine Einsätze, keine Strafen, keine Auszahlung, kein Eintrag auf dem Verpasste-Runden-Zähler. | Live-Wetten | backend |
| 8.6-b | Nach dem Auflösen ist Annullieren nicht mehr möglich. | Live-Wetten | backend |
| 8.7 | Der Host kann den Raum zurücksetzen — Spieler, Punktestände und laufende Runde in einem Schritt, in jeder Phase. | Live-Wetten | backend |
| 8.7-a | Nach dem Zurücksetzen gibt es kein automatisches Wiederbeitreten. | Live-Wetten | backend |
| 8.7-b | Der Zurücksetzen-Knopf ist bewusst unscheinbar. | Live-Wetten | frontend |

### 9. Ablauf einer Runde

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 9-a | Der Ablauf durchläuft Leerlauf → Öffnen → Tippen → Schließen → Auflösen → Leerlauf. | Live-Wetten | backend |
| 9-b | Ab dem Schließen werden alle abgegebenen Tipps offen angezeigt, und es kann nicht mehr getippt werden. | Live-Wetten | backend |
| 9-c | Erst beim Auflösen werden Punkte verrechnet: Pool bilden, Strafen einsammeln, Gewinner nach Anteilen auszahlen, Kontostände aktualisieren. | Live-Wetten | backend |
| 9-d | Das Ergebnis zeigt zu jedem Tipp den Einsatz und hebt die eigene Zeile hervor. | Live-Wetten | frontend |

### 10. Rollen

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 10-a | Der Host hat zusätzlich die Steuerknöpfe: Wette öffnen, jetzt schließen, auflösen, annullieren, zurücksetzen. Ansonsten ist er normaler Spieler. | Live-Wetten | backend |
| 10-b | Ein Spieler ohne Host-Rolle kann diese Kommandos nicht auslösen. | Live-Wetten | backend |
| 10-c | Spieler sehen Countdown, aufgedeckte Tipps, Ergebnisse und Leaderboard. | Live-Wetten | frontend |
| 10-d | Der Countdown ist zusätzlich als ablaufender Rahmen sichtbar; Phasenwechsel sind animiert. | Live-Wetten | frontend |
| 10.1 | Host ist immer der am frühesten beigetretene verbundene Spieler. | Live-Wetten | backend |
| 10.1-a | Verliert der Host die Verbindung, wandert die Rolle sofort weiter — in jeder Phase. | Live-Wetten | backend |
| 10.1-b | Kehrt ein früher beigetretener Spieler während eines offenen oder geschlossenen Fensters zurück, wird die Übergabe vorgemerkt und erst im Leerlauf bzw. nach dem Auflösen ausgeführt. | Live-Wetten | backend |
| 10.1-c | Die Host-Rolle verlangt keinen zusätzlichen Einstiegsschritt: keine eigene URL, kein Kennwort. | Live-Wetten | backend |
| 10.1-d | Solange ein Spieler beigetreten ist, hält die Oberfläche den Bildschirm wach (Screen Wake Lock), best effort. | Live-Wetten | frontend |
| 10.1-e | Die Host-Rolle ist an einer eigenen Farbe erkennbar: Chip neben dem Namen. | Live-Wetten | frontend |

### 11. Betrieb

| ID | Regel | Geltung | Marke |
|---|---|---|---|
| 11-a | Genau eine Server-Instanz; kein Autoscaling, kein Sharding. | beide | organisatorisch |
