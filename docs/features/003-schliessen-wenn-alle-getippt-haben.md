# 003 — Schließen, wenn alle getippt haben

## Anlass

Haben alle Teilnehmer getippt, bringt das Warten auf den Countdown nichts
mehr: Es kommt kein Tipp mehr, und die Aufdeckung — der eigentliche Moment
der Runde — verzögert sich um bis zu 15 Sekunden. Das Fenster soll deshalb
sofort schließen, sobald der beim Öffnen eingefrorene Teilnehmerkreis
vollständig getippt hat. Damit der Sprung nicht als Aussetzer wirkt, macht
die Oberfläche sichtbar, *warum* geschlossen wurde.

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 5-b | bestehend | Fenster schließt nach 15 Sekunden |
| 5-c | bestehend | oder per Host-Klick |
| 5-d | bestehend | je nachdem, was zuerst eintritt |
| 8.1-b | bestehend | Teilnehmerkreis ist beim Öffnen eingefroren |
| 9-a | bestehend | Ablauf beim Schließen |
| 9-b | bestehend | Aufdeckung beim Schließen |
| 10-c | bestehend | was Spieler sehen |
| 5-g | neu | Ein dritter Auslöser fürs Schließen neben Zeitablauf und Host-Klick. Das ist Verhalten der Leitung, nicht der Darstellung. Marke `backend`. |
| 5-h | neu | Die Oberfläche hebt hervor, dass wegen vollständiger Beteiligung geschlossen wurde, statt kommentarlos umzuschalten. Marke `frontend`. |

Der Abschnitt 5 der Anforderungen bekommt einen dritten Auslöser für den
Fensterschluss, Abschnitt 9 Schritt 4 wird entsprechend nachgezogen.

## Akzeptanzkriterien

**Schließen (5-g)**

1. Sobald jeder Teilnehmer des eingefrorenen Kreises einen Tipp abgegeben
   hat, ist das Fenster geschlossen — ohne Zutun des Hosts und ohne den
   Countdown abzuwarten.
2. Das Schließen wirkt in derselben Zustandsmeldung wie der letzte Tipp:
   Die Runde ist nie in einem Zustand sichtbar, in dem alle getippt haben
   und das Fenster noch offen ist.
3. Maßgeblich ist der eingefrorene Teilnehmerkreis (8.1-b). Wer erst nach
   dem Öffnen beigetreten ist, hält die Runde nicht auf; sein Tipp allein
   schließt sie aber auch nicht.
4. Ein pausierter Spieler (8.1-d) gehört nicht zum Kreis und hält die Runde
   ebenfalls nicht auf.
5. Ein getrennter Spieler, der noch nicht pausiert, gehört zum Kreis und
   hält die Runde auf — bis der Countdown abläuft.
6. Der Auto-Close-Timer der Runde läuft ins Leere, wenn er danach noch
   feuert: Er schließt keine bereits geschlossene und keine neue Runde
   (ADR-010, unverändert).
7. Der letzte Tipp wird noch angenommen und ausgezahlt wie jeder andere —
   das Schließen folgt ihm, es verdrängt ihn nicht.

**Anzeige (5-h)**

8. In `CLOSED` benennt die Oberfläche den Grund, wenn kein Teilnehmer ohne
   Tipp geblieben ist: geschlossen, weil alle getippt haben.
9. Diese Meldung wird animiert eingeblendet, einmal pro Runde und Phase —
   nicht bei jedem eingehenden `STATE`, wie schon bei 10-d.
10. Bei `prefers-reduced-motion: reduce` entfällt die Animation; die Aussage
    bleibt als Text vollständig lesbar.

Bewusst **kein** neues Feld im Protokoll: „alle haben getippt" ist in
`CLOSED` bereits eindeutig ablesbar — die Liste der Teilnehmer ohne Tipp
(8.1-f) ist dann leer, und leer kann sie nur sein, weil Kriterium 1 sofort
schließt. Ein zusätzliches Grund-Feld wäre eine zweite Wahrheit über
denselben Sachverhalt und dazu ein weiteres Feld an `Messages.State`, dessen
Leck-Risiko die Positivliste in `VerdeckteTippsStufen` jedes Mal neu prüfen
müsste.

## Szenarien

Als JGiven-Szenarien (Port-to-Port-Ebene, `teststrategie.md` Abschnitt 2.2)
entstehen die Kriterien 1 bis 5 und 7:

**Haben alle getippt, schließt das Fenster von selbst.**
Angenommen ein Host und Anna sind im Raum und der Host hat eine Wette
geöffnet.
Wenn Anna tippt und danach der Host tippt.
Dann ist das Fenster geschlossen, ohne dass jemand geschlossen hat und ohne
dass die 15 Sekunden abgelaufen sind, und beide Tipps liegen offen.

**Solange einer fehlt, bleibt das Fenster offen.**
Angenommen ein Host und Anna sind im Raum und der Host hat eine Wette
geöffnet.
Wenn nur Anna tippt.
Dann ist das Fenster weiterhin offen und es fehlt noch ein Tipp.

**Wer zu spät kommt, hält die Runde nicht auf.**
Angenommen ein Host ist im Raum und hat eine Wette geöffnet, und Anna tritt
erst danach bei.
Wenn der Host tippt.
Dann ist das Fenster geschlossen — Anna gehört nicht zum eingefrorenen
Kreis, obwohl sie noch nicht getippt hat.

**Ein pausierter Spieler hält die Runde nicht auf.**
Angenommen ein Host und Anna sind im Raum, Anna ist getrennt und pausiert
nach drei verpassten Runden.
Wenn der Host eine Wette öffnet und tippt.
Dann ist das Fenster geschlossen.

**Ein getrennter Spieler ohne Pause hält die Runde auf.**
Angenommen ein Host und Anna sind im Raum, Anna ist getrennt, hat aber noch
keine Runde verpasst.
Wenn der Host eine Wette öffnet und tippt.
Dann ist das Fenster weiterhin offen, bis die 15 Sekunden ablaufen.

**Der letzte Tipp zählt ganz normal.**
Angenommen ein Host und Anna sind im Raum und der Host hat eine Wette
geöffnet.
Wenn Anna auf Touchdown tippt, der Host auf Punt tippt und der Host
zugunsten von Touchdown auflöst.
Dann hat Anna den ganzen Pool gewonnen — der Tipp, der das Fenster
geschlossen hat, ist ein Tipp wie jeder andere.

Kriterium 6 ist bereits durch `RundenwacheScenarioTest` belegt (5-d) und
wird dort um den neuen Auslöser ergänzt, nicht neu erfunden. Die Kriterien 8
bis 10 sind Oberfläche und liegen nach `teststrategie.md` §11 außerhalb der
Teststrategie; sie werden von Hand nachvollzogen: 8 und 9 durch Durchspielen
einer Runde mit zwei Browsern (der zweite Tipp schließt), 10 über die
DevTools-Emulation von `prefers-reduced-motion`.

## Kritikalität

**Stufe:** MEDIUM

Ein Fehler bei der Frage „haben wirklich alle getippt?" schließt das Fenster
zu früh und sperrt einen Spieler aus, der noch tippen wollte — der zahlt dann
die Strafe nach 8.1 für etwas, das er nicht zu verantworten hat. Das ist ein
Punkte-Effekt, kein reines Anzeigeproblem, und die naheliegende Umsetzung
(Zähler gegen Teilnehmerzahl) hat genau diesen Fehler eingebaut: Ein nach
dem Öffnen beigetretener Spieler zählt beim Tippen mit, gehört aber nicht
zum eingefrorenen Kreis. Deshalb entscheidet die Runde selbst über die
Vollständigkeit ihres Kreises, nicht der Anwendungsring über zwei Zahlen —
und deshalb prüfen Kriterium 3 und 4 genau diesen Fall.

Nicht HIGH, weil die Wirkung auf eine Runde begrenzt ist, am Tisch sofort
auffällt und der Host sie mit `ANNUL` (8.6) zurückdrehen kann.

## Umgesetzt in

`Round` (die Abfrage über den eigenen Teilnehmerkreis), `RoomActor` (der
Auslöser, an derselben Stelle wie der Timer aus ADR-010) — dazu
`frontend/src/App.jsx` und `frontend/src/styles.css`.

## Offene Fragen

Ob der sofortige Schluss am Tisch zu abrupt wirkt — der letzte Tipper sieht
seinen eigenen Tipp praktisch nicht mehr, weil die Aufdeckung ihn
überholt —, lässt sich am Schreibtisch nicht beantworten. Steht als
Beobachtungspunkt in `probelauf.md`; eine kurze Verzögerung vor dem
Schließen wäre die Antwort, falls es stört.
