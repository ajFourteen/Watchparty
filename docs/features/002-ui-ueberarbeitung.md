# 002 — Überarbeitung von Oberfläche und Bedienung

## Anlass

Acht Beobachtungen aus der Bedienung der App, gesammelt vor dem ersten
Probelauf. Sie betreffen fast alle nur die Oberfläche — zwei davon aber
nicht: Wer nicht getippt hat, lässt sich im Client nicht ausrechnen (der
Teilnehmerkreis ist beim Öffnen eingefroren, 8.1-b/8.1-d), und der
Mindesteinsatz steht bisher als eigene Konstante im Frontend, also ein
zweites Mal neben `Params` (3.1-a).

Die acht Punkte im Einzelnen: (1) Sektionen klarer trennen und die
Punktestände der anderen ausklappbar unter den eigenen legen, (2) das
Einsatzfeld sichtbarer machen, beim Antippen selektieren und die
Zahlentastatur öffnen, (3) im geschlossenen Fenster zeigen, wer nicht
getippt hat, (4) den Einsatz im Ergebnis mit anzeigen, (5) die eigene
Zeile im Ergebnis hervorheben, (6) Phasenwechsel animieren, (7) die
Host-Rolle durch eine eigene Farbe sichtbar machen, (8) den Countdown
zusätzlich als ablaufenden Rahmen zeigen.

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 3.1-a | bestehend | Parameter an einer Stelle |
| 6-b | bestehend | verdeckte Tipps |
| 6-d | bestehend | Einsatz vor dem Bestätigen erhöhen |
| 8.1-a | bestehend | Teilnehmerkreis |
| 8.1-b | bestehend | Strafe |
| 9-b | bestehend | ab dem Schließen liegen die Tipps offen |
| 10-c | bestehend | Spieler sehen Countdown, Tipps, Ergebnisse, Leaderboard |
| 3.1-c | neu | Der Client bekommt die drei Parameter vom Server, statt eine eigene Kopie zu halten. Zieht 3.1-a über die Protokollgrenze. Marke `backend`. |
| 8.1-f | neu | Ab dem Schließen nennt der Zustand die Teilnehmer ohne Tipp — vorher nicht. Das ist eine Protokollzusage, keine Frage der Darstellung. Marke `backend`. |
| 8.1-g | neu | Die Oberfläche zeigt sie hervorgehoben an und nennt die Strafe. Marke `frontend`. |
| 9-d | neu | Das Ergebnis zeigt zu jedem Tipp den Einsatz und hebt die eigene Zeile hervor. Marke `frontend`. |
| 10-d | neu | Der Countdown ist zusätzlich als ablaufender Rahmen sichtbar; Phasenwechsel sind animiert. Marke `frontend`. |
| 10.1-e | neu | Die Host-Rolle ist an einer eigenen Farbe erkennbar (Rahmen und Chip). Marke `frontend`. |

Nicht in der Tabelle, weil keine Anhang-A-ID: Invariante 4 aus `CLAUDE.md`
(verdeckte Tipps) ist von 6-b nicht zu trennen und wird durch dieses
Feature ausdrücklich nicht angetastet.

## Akzeptanzkriterien

**Parameter über das Protokoll (3.1-c)**

1. `WELCOME` trägt Startguthaben, Mindesteinsatz und Strafe aus `Params`.
2. Das Frontend hält keine eigene Konstante für den Mindesteinsatz mehr;
   die angezeigten Werte stammen ausschließlich aus `WELCOME`.

**Nicht-Tipper (8.1-f, 8.1-g)**

3. In `CLOSED` und `RESOLVED` nennt `STATE` die Teilnehmer des
   eingefrorenen Kreises, die nicht getippt haben.
4. In `IDLE` und `OPEN` ist dieses Feld nicht gesetzt. Während des offenen
   Fensters wäre es die Umkehrung des Pick-Zählers und damit ein Bruch von
   Invariante 4 — wer nicht in der Liste steht, hat getippt.
5. Wer erst nach dem Öffnen beigetreten ist, steht nicht in der Liste
   (8.1-b), ein pausierter Spieler ebenso wenig (8.1-d).
6. Die Oberfläche zeigt diese Spieler ab dem Schließen in der Aufdeckung,
   sichtbar abgesetzt von den abgegebenen Tipps.
7. In `CLOSED` nennt sie die Strafe als *drohend*, nicht als gefallen: Sie
   wird auf den Kontostand gekappt (8.1-c) und entfällt ganz, wenn der Host
   annulliert (8.6-a). In `RESOLVED` steht stattdessen der tatsächliche
   Betrag aus den Deltas.

**Oberfläche (1, 2, 4, 5, 6, 7, 8)**

8. Die Reihenfolge ist: eigener Punktestand, dann die Punkte der anderen
   (ausklappbar, standardmäßig zu), dann abgesetzt die Wette, dann die
   Host-Steuerung.
9. Der Aufklapp-Zustand des Leaderboards überlebt einen Reload (pro Gerät).
10. Das Einsatzfeld zeigt seinen Wert in kräftiger Farbe, selektiert ihn
    beim Fokus vollständig und öffnet auf dem Handy die Zahlentastatur.
11. Im Ergebnis steht zu jedem Tipp der Einsatz, und die eigene Zeile ist
    hervorgehoben — in der Aufdeckung ebenso, damit sie beim Phasenwechsel
    nicht erst erscheint.
12. Ein Phasenwechsel wird animiert, und zwar sichtbar unterschiedlich für
    Gewinn und Verlust. Die Animation läuft einmal pro Runde und Phase,
    nicht bei jedem `STATE` — der Server sendet ihn bei jedem abgegebenen
    Tipp neu.
13. Bei `prefers-reduced-motion: reduce` entfallen die Animationen; die
    Information bleibt ohne sie vollständig lesbar.
14. Der Host erkennt seine Rolle an einem dünnen Rahmen um die Anwendung
    und einem Chip in derselben Farbe.
15. Im offenen Fenster läuft ein roter Rahmen ab. Er gewinnt gegen den
    Host-Rahmen, solange das Fenster offen ist.
16. Der ablaufende Rahmen synchronisiert sich nach einem Reconnect aus
    `closesAt` und dem Uhren-Offset (ADR-003), nicht aus einer eigenen
    Zeitrechnung im Client.

## Szenarien

Als JGiven-Szenarien (Port-to-Port-Ebene, `teststrategie.md` Abschnitt 2.2)
entstehen die Kriterien 1 bis 5 — sie liegen hinter der Protokollgrenze und
sind dort prüfbar:

**Die Parameter kommen mit dem Beitritt.**
Angenommen ein Spieler ist noch nicht im Raum.
Wenn er beitritt.
Dann nennt ihm das WELCOME Startguthaben, Mindesteinsatz und Strafe, und
zwar dieselben Werte, mit denen der Server rechnet.

**Ab dem Schließen ist sichtbar, wer nicht getippt hat.**
Angenommen ein Host und Anna sind im Raum und der Host hat eine Wette
geöffnet.
Wenn Anna tippt und der Host das Fenster schließt.
Dann nennt der Zustand den Host als Teilnehmer ohne Tipp, Anna nicht.

**Solange das Fenster offen ist, verrät der Zustand die Nicht-Tipper nicht.**
Angenommen ein Host und Anna sind im Raum und der Host hat eine Wette
geöffnet.
Wenn Anna tippt.
Dann ist im Zustand kein Feld gesetzt, das die Nicht-Tipper nennt — nur der
Zähler (Invariante 4).

**Wer zu spät kommt, steht nicht in der Liste.**
Angenommen ein Host und Ben sind im Raum, der Host hat eine Wette geöffnet,
und Anna tritt erst danach bei.
Wenn der Host tippt und das Fenster schließt.
Dann nennt der Zustand Ben als Teilnehmer ohne Tipp, Anna nicht — sie
gehört nicht zum eingefrorenen Kreis (8.1-b).

Die Kriterien 6 bis 16 sind Oberfläche und liegen nach
`teststrategie.md` §11 außerhalb der Teststrategie. Sie werden von Hand
nachvollzogen, wie schon bei 001: Kriterium 6 bis 11 durch Durchspielen
einer Runde mit zwei Browsern (einer tippt nicht), Kriterium 12 durch
Beobachten, dass die Animation bei einem zweiten eingehenden Tipp *nicht*
erneut läuft, Kriterium 13 über die DevTools-Emulation von
`prefers-reduced-motion`, Kriterium 14 und 15 durch Wechsel der Host-Rolle
per Trennen des ersten Tabs, Kriterium 16 durch Reload mitten im offenen
Fenster.

## Kritikalität

**Stufe:** MEDIUM

Mit einer Stelle, die den Ausschlag gibt.

Der Löwenanteil ist reine Darstellung — `LOW` für sich genommen: kein
Einfluss auf Punkte, Serverzustand oder Rundenablauf, und ein Fehler fällt
am Tisch sofort auf.

Das neue Feld an `Messages.State` ist es nicht. `RoomView` ist nach
`teststrategie.md` 6.4 als `HIGH` eingestuft, ausdrücklich mit der
Begründung, dass *jedes neue Feld* ein Leck auslösen kann — und dieses Feld
ist die Umkehrung des Pick-Zählers, also genau die Information, die
Invariante 4 während `OPEN` schützt. Die Einstufung von `RoomView` bleibt
deshalb unverändert `HIGH`; die Positivliste in `VerdeckteTippsStufen`
fängt einen Fehlgriff an dieser Stelle ab, ohne dass jemand daran denken
muss.

Der Rest des Features trägt `MEDIUM`, weil die Anzeige der Strafe eine
Aussage über Punkte macht: Wer in `CLOSED` einen Betrag liest, den die
Kappung (8.1-c) oder ein Annullieren (8.6-a) später widerlegt, hält die App
für falsch rechnend. Deshalb Kriterium 7.

## Umgesetzt in

`RoomView`, `Messages` (`State.nonPickers`, `Welcome.params`), `RoomActor`
— dazu `frontend/src/App.jsx`, `frontend/src/useRoom.js` und
`frontend/src/styles.css`.

## Offene Fragen

Ob der ablaufende Rahmen auf älteren Handys flüssig läuft, ist eine Frage
für den Spielabend, nicht für den Schreibtisch — er steht deshalb auf dem
Bogen in `probelauf.md`. Dasselbe gilt für das Selektieren des Einsatzfelds
auf iOS: Das nachlaufende Touch-Ereignis hebt die Selektion dort
gelegentlich wieder auf.
