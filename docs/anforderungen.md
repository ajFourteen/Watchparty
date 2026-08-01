# Fachliche Anforderungen — Live-Wett-App für Football-Watchpartys

## 1. Zweck und Kontext

Die App richtet sich an Freunde, die gemeinsam vor Ort ein Football-Spiel schauen und das Zuschauen spannender machen wollen. Über ihre Handys wetten sie live auf Football-Ereignisse (zunächst: der Ausgang des nächsten Drives). Es geht um Spaß und ein gemeinsames Punkte-Ranking, nicht um echtes Geld.

**Rahmenbedingungen:**
- Alle Teilnehmer sitzen vor demselben Fernseher (nur Vor-Ort-Nutzung, kein Remote-Play).
- Es gibt genau einen Spielraum; immer nur eine Runde gleichzeitig.
- Keine Persistenz über Spielabende hinweg. Jeder Abend beginnt frisch.
- Teilnahme ohne Installation und ohne Account: Link öffnen, Name eingeben, dabei.

## 2. Wett-Grundprinzip (Pari-mutuel / Totalisator)

Es gibt keinen Buchmacher und kein Wahrscheinlichkeitsmodell. Die Wett-Ökonomie funktioniert nach dem Totalisator-Prinzip:

- Alle Einsätze einer Runde wandern in einen gemeinsamen Pool.
- Wer richtig liegt, teilt sich den Pool.
- Die Auszahlung entsteht aus dem tatsächlichen Wettverhalten der Gruppe: Ein Ausgang, den kaum jemand getippt hat, zahlt viel; ein Ausgang, auf den alle setzen, zahlt wenig. Damit belohnt das System automatisch, das Spiel besser zu lesen als die Mitspieler — „unwahrscheinlich → mehr Punkte" ergibt sich von selbst.

**Nullsummen-Eigenschaft:** Punkte entstehen und verschwinden nicht, sie werden nur umverteilt. Der Pool ist stets exakt die Summe aller Einsätze plus aller Strafen. Diese Eigenschaft ist bindend.

## 3. Spieler und Punktekonten

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

## 4. Märkte

- Ein Markt ist fachlich eine **Frage mit einer festen Liste möglicher Ausgänge** und einer späteren Auflösung. Er wird als eigenständige Struktur behandelt, nicht als Sonderfall im Code, damit später weitere Markttypen ohne Umbau ergänzt werden können.
- **Zum Start gibt es genau einen Markttyp: „Ausgang des nächsten Drives".**
- Weitere feste Märkte folgen später.

### 4.1 Kanonische Ausgänge „Ausgang des nächsten Drives"

Jeder reale Drive-Ausgang fällt in genau einen Eimer (lückenlos und überschneidungsfrei):

| Ausgang | Anmerkung |
|---|---|
| Touchdown | |
| Field Goal | nur bei erfolgreichem Kick |
| Punt | |
| Turnover | Interception oder verlorener Fumble |
| Turnover on Downs | umfasst auch den verschossenen Field Goal (Gegner übernimmt am Ort) |
| Safety | |
| End of Half / Game | Drive läuft mit Halbzeit- oder Spielende aus |

Diese Zuordnung ist eine festgelegte Konvention und muss in der Oberfläche sichtbar sein, damit es beim Auflösen keinen Streit gibt.

## 5. Wettfenster und Timing

- Der **Host** entscheidet, wann ein Markt öffnet.
- Nach dem Öffnen bleibt das Fenster **15 Sekunden** offen und schließt dann automatisch.
- Zusätzlich hat der Host einen **„Jetzt schließen"-Knopf** als Notbremse. Das Fenster schließt bei Ablauf der 15 Sekunden **oder** beim Host-Klick — je nachdem, was zuerst eintritt.
- Verantwortung des Hosts: das Fenster so öffnen, dass die 15 Sekunden **vor dem Snap** des Drives ablaufen. Danach läuft der Drive, es wird nicht mehr getippt.

## 6. Wettmechanik

- **Ein Tipp pro Spieler pro Runde.** Kein Aufteilen des Einsatzes auf mehrere Ausgänge, kein Nachbessern.
- **Wetten sind verdeckt**, solange das Fenster offen ist. Während der offenen Phase ist nur sichtbar, *wie viele* schon getippt haben, nicht *was*.
- Es gibt einen **Mindesteinsatz**. Der Mindesteinsatz ist der Standard-Einsatz: Ein einzelner Tipp auf einen Ausgang setzt automatisch den Mindesteinsatz. Wer will, erhöht den Einsatz vor dem Bestätigen.
- Einsätze sind beliebige ganze Zahlen ab dem Mindesteinsatz bis zum eigenen Kontostand.
- **Spieler mit weniger Punkten als dem Mindesteinsatz** können trotzdem mitwetten und gehen dabei zwangsweise All-in (auch mit 0 Punkten, siehe 8.3).

## 7. Auszahlung

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

### 8.1 Nicht-Tipper-Strafe
- Wer in einer Runde gar nicht tippt, zahlt eine kleine Strafe, die in den Pool fließt.
- Die Strafe trifft jeden im Raum, der nicht getippt hat — unabhängig vom Grund (auch bei eingeschlafenem Handy). Innerhalb des 15-Sekunden-Fensters ist der Grund nicht unterscheidbar.

**Wer zum Teilnehmerkreis gehört, wird beim Öffnen des Markts eingefroren.** Wer während des offenen Fensters dazukommt, darf tippen und gewinnen, wird aber nicht bestraft. Niemand zahlt für eine Runde, die schon lief, als er kam.

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

## 9. Ablauf einer Runde (fachliche Sicht)

1. **Leerlauf** — der Host kann einen Markt öffnen.
2. **Öffnen** — Host öffnet den Markt; die 15-Sekunden-Uhr läuft.
3. **Tippen (verdeckt)** — Spieler tippen; sichtbar ist nur die Anzahl der abgegebenen Tipps.
4. **Schließen** — nach 15 Sekunden oder per Host-Notbremse. **Ab jetzt werden alle abgegebenen Tipps offen angezeigt.** Es kann nicht mehr getippt werden. Der Drive läuft im Fernsehen.
5. **Auflösen** — der Host wählt den tatsächlichen Ausgang. **Erst jetzt** werden Punkte verrechnet: Pool bilden, Strafen einsammeln, Gewinner nach Anteilen auszahlen, Leaderboard aktualisieren.
6. Zurück zu Leerlauf.

**Wichtig:** Aufdeckung der Tipps erfolgt bei **Marktschluss** (Schritt 4). Die **Punkte-Verrechnung** erfolgt getrennt davon erst beim **Auflösen** (Schritt 5).

## 10. Rollen

- **Host:** hat zusätzlich die Steuerknöpfe (Markt öffnen, Markt jetzt schließen, Ausgang auflösen). Ansonsten normaler Spieler.
- **Spieler:** tippen, sehen Countdown, aufgedeckte Tipps, Ergebnisse und Leaderboard.

## 11. Bewusst nicht enthalten (out of scope)

- Kein Remote-/Online-Play über mehrere Orte hinweg.
- Keine mehreren parallelen Räume.
- Keine Persistenz / keine Saison über mehrere Abende.
- Keine automatische Ergebnis-Erkennung per Datenfeed; der Host löst manuell auf (bewusst, um die Broadcast-Verzögerung zu umgehen und synchron zum Fernsehbild im Raum zu bleiben).
- Kein echtes Geld.

## 12. Offene Punkte / spätere Erweiterungen

- Weitere feste Markttypen über den Drive-Ausgang hinaus.
- Nachjustierung der Parameter aus 3.1 am realen Spielgefühl.
