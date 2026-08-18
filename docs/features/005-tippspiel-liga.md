# 005 — Tippspiel-Liga über die Saison

## Anlass

Neben den Live-Wetten am Spielabend soll es einen zweiten, davon unabhängigen
Spielmodus geben: eine **Tippspiel-Liga über eine ganze Saison**. Getippt wird
ausschließlich das Endergebnis jedes Spiels eines Spieltags; gewertet wird nach
Tendenz, Abstand und exaktem Ergebnis. Man kann mehreren Ligen angehören,
tippt aber nur einmal — der Tipp gehört dem Tipper und dem Spiel, nicht der
Liga.

Beide Spielmodi laufen parallel: Wer am Sonntagabend im Wohnzimmer live wettet,
hat für dasselbe Spiel womöglich vor dem Anstoß seinen Ligatipp abgegeben. Sie
teilen sich die Anwendung und sonst nichts — insbesondere keine Punkte, keine
Konten und keinen Zustand.

**Das ist der größte Umbau seit dem ersten Release.** Er nimmt drei bewusst
getroffene Entscheidungen zurück (Persistenz über Abende hinweg, kein Account,
kein Datenfeed) und bringt mit Datenbank, Benutzerkonten, E-Mail-Versand und
einer Außenanbindung vier Bausteine ins Projekt, die es heute nicht gibt.

**Der Bau ist beschlossen** (2026-08-17). Geltender Stand bleibt trotzdem
`anforderungen.md`; dieses Dokument ist der Antrag und wird nach der Umsetzung
nicht weiter gepflegt (`teststrategie.md` 9.1). Was am Geltungsbereich schon
nachgezogen ist, steht unter „Nötige ADRs".

## Abgrenzung der beiden Spielmodi

| | Live-Wetten (heute) | Tippspiel (neu) |
|---|---|---|
| Zeithorizont | ein Abend | eine Saison |
| Wer tippt | anonymer Name im Raum | Benutzerkonto |
| Worauf | Ausgang des nächsten Drives u. a. (Katalog, 4) | Endergebnis jedes Spiels |
| Punkte | Einsätze, pari-mutuel, Nullsumme (2, 7) | Wertungspunkte, keine Nullsumme, kein Einsatz |
| Auflösung | Host von Hand (11) | Endergebnis aus dem Feed |
| Zustand | Arbeitsspeicher + Snapshot (ADR-004/023) | Datenbank |
| Leitung | WebSocket, Push (ADR-006) | HTTP, Anfrage/Antwort |
| Ausfall | Abend kaputt | Spieltag nachtragbar |

Die Tabelle ist der eigentliche Kern des Features: **Es gibt fast keine
Gemeinsamkeit außer der Anwendung, die beides ausliefert.** Wer versucht,
`Room`, `Player` oder `Points` für die Liga wiederzuverwenden, bringt die
Invarianten aus CLAUDE.md in Gefahr, ohne einen Zeile Code zu sparen.

## Betroffene Anforderungen

**Zurückgenommen, aber nur für das Tippspiel** (die Live-Wetten bleiben in
jedem Punkt, wie sie sind — das gehört ausdrücklich in die Formulierung, sonst
liest sich jede dieser Streichungen wie eine Aufgabe des bisherigen Prinzips):

| ID | heute | künftig |
|---|---|---|
| 1-c | keine Persistenz über Spielabende hinweg | gilt für die Live-Wetten; das Tippspiel ist per Definition dauerhaft |
| 1-e | Beitritt verlangt nur einen Namen, kein Account | gilt für die Live-Wetten; das Tippspiel verlangt ein Konto |
| 11 (out of scope) | keine Persistenz / keine Saison | fällt für das Tippspiel |
| 11 (out of scope) | keine automatische Ergebnis-Erkennung per Datenfeed | fällt für das Tippspiel; die Begründung (Broadcast-Verzögerung, Synchronität zum Fernsehbild) trägt nur für die Live-Wetten und ist für ein Endergebnis gegenstandslos |
| 11 (out of scope) | kein Remote-Play über mehrere Orte | fällt für das Tippspiel; eine Liga ist ortsunabhängig |

**Unberührt:** Die gesamte Wett-Ökonomie (2, 3, 6, 7, 8), der Wettkatalog (4),
Fenster und Ablauf (5, 9), Rollen (10). Kein Zeichen davon ändert sich.
Weiterhin gilt: kein echtes Geld.

**Neu:** ein eigenes Kapitel 13 in `anforderungen.md`. Die bestehende
Nummerierung bleibt unangetastet (dieselbe Zusage wie in Feature 004), Kapitel
12 („Offene Punkte") bleibt stehen. Vorschlag für die Gliederung:

| Abschnitt | Inhalt |
|---|---|
| 13.1 | Zweck, Abgrenzung zu den Live-Wetten, Parallelbetrieb |
| 13.2 | Konto und Anmeldung |
| 13.3 | Spielplan, Anstoßzeiten, Endergebnisse |
| 13.4 | Tippen und Abgabeschluss |
| 13.5 | Wertung (Tendenz, Abstand, exaktes Ergebnis) |
| 13.6 | Ligen, Mitgliedschaft, Rangliste |
| 13.7 | Sonder- und Randfälle (Absage, Verlegung, Korrektur, Feed-Ausfall) |
| 13.8 | Datenschutz und Löschung |

Anhang A wächst entsprechend um die IDs `13.1-a` bis `13.8-x`; die
Akzeptanzkriterien unten sind so geschnitten, dass daraus je eine atomare
Regel wird.

## Bewusste Festlegungen

Diese Punkte sind entschieden (Rückfrage vom 2026-08-17) und stehen hier, damit
sie nicht als offen missverstanden werden.

**Wertung: die höchste erreichte Stufe zählt, nicht die Summe.**
Exaktes Ergebnis 6, sonst richtiger Abstand 5, sonst richtige Tendenz 3, sonst
0. Ein Tipp bringt also nie mehr als 6 Punkte.

**Die Stufen bauen aufeinander auf.** Der Abstand wird nur bei richtiger
Tendenz gewertet. Wer 17:24 tippt und 24:17 bekommt, hat nicht „das 1-Score-
Game erkannt", sondern den Sieger verwechselt — 0 Punkte. Ohne diese Regel wäre
ein falsch getipptes knappes Spiel (5) mehr wert als ein richtig getipptes
deutliches (3), und das wäre nicht zu erklären.

**Abstands-Eimer:** 0 Punkte Differenz = Unentschieden, 1–8 = 1-Score-Game,
9–16 = 2-Score-Game, ab 17 = 3+-Score-Game. Acht ist die größte Differenz, die
ein einzelner Drive noch ausgleicht (Touchdown plus Two-Point) — deshalb diese
Grenzen und keine glatten Zehner. Beim Unentschieden fallen Tendenz und Abstand
zusammen: Wer richtig auf Unentschieden tippt, hat den Abstand 0 damit
zwangsläufig getroffen und bekommt 5.

**Ein Tipp gehört dem Tipper und dem Spiel, nicht der Liga.** Eine Liga ist
eine Menge von Mitgliedern und eine Rangliste darüber — sie besitzt keine
Tipps. Daraus folgt „nur einmal tippen" von selbst, statt über eine
Kopiermechanik zwischen Ligen. Es folgt auch, dass eine Liga rückwirkend
gewertet werden kann: Die Tipps ihrer Mitglieder gibt es schon.

**Abgabeschluss ist der Anstoß des jeweiligen Spiels.** Nicht der Spieltag als
Ganzes. Ein Spieltag ist die Anzeige- und Tippeinheit, kein Termin.

**Spielplan und Endergebnisse kommen aus einem externen Feed.** Kein
Handeintrag als Regelfall — 272 Spiele pro Saison von Hand sind der sichere Weg
in eine ungepflegte Liga. Der Handeintrag bleibt als *Notweg* für den Betreiber
bestehen (13.7), weil ein Feed ausfallen und Unsinn liefern kann.

**Wiedererkennung über ein Benutzerkonto mit E-Mail (Magic Link).** Ein
Gerätetoken wie bei den Live-Wetten (ADR-014) trägt einen Abend, aber keine
Saison: Wer im November den Browser aufräumt, verliert vier Monate Tipps. Kein
Kennwort, weil ein Kennwort einen Weg zurück braucht und der Weg zurück wieder
die E-Mail wäre.

Die folgenden sechs standen bis zum 2026-08-17 unter „Offene Fragen" und sind
seitdem entschieden. Sie gehören in ADR-034 bis ADR-039, sobald die geschrieben
sind.

**Verwaltetes Postgres, nicht SQLite auf dem Volume.** Nicht wegen der Last —
die ist lächerlich klein —, sondern wegen der Sicherung: Ein Fly-Volume hängt
an einer Maschine, und der Verlust einer Saison ist etwas anderes als der
Verlust eines Abends. Für die Live-Wetten ändert das nichts; ADR-004 gilt
unverändert, der Raumzustand bleibt im Arbeitsspeicher.

**ESPN als Feed, hinter dem Port `ScheduleFeed`.** Die Quelle ist
unbeauftragt: keine Zusage, keine Nutzungserlaubnis, jederzeit änderbar. Das
ist als Risiko angenommen und nicht übersehen — abgefedert durch den Port (ein
Wechsel bleibt ein Adapter), durch aufgezeichnete Antworten im Test statt Netz
und durch den Handeintrag als Notweg (14).

**Eine Liga wertet die ganze Saison, auch für Spätbeitreter.** Wer im November
beitritt, bringt seine bis dahin abgegebenen Tipps mit und kann am ersten Tag
vorne stehen. Das ist der Preis dafür, dass die Rangliste aus den Tipps allein
nachrechenbar bleibt: „erst ab Beitritt" verlangt eine Zeitachse, die niemand
am Tisch nachvollzieht, und einen zweiten Weg durch die Wertung — ausgerechnet
den Teil mit `HIGH` und 99 % Mutation Score.

**Eine Liga gehört zu genau einer Saison.** `League` trägt eine `SeasonId`;
nächstes Jahr entsteht eine neue Liga mit neuem Code. Eine fortbestehende Liga
bräuchte einen Saisonwechsel als eigenen Vorgang, den jemand auslöst, und
prompt die Frage nach der ewigen Tabelle. Dass sich die Runde jedes Jahr neu
zusammenfindet, ist bei einer Handvoll Freunden kein Aufwand.

**Erste Saison ohne Playoffs.** Die Wertung endet nach der Regular Season.
Playoff-Runden haben andere Größen (6, 4, 2, 1 Spiele) und hängen am Ergebnis
der Vorrunde; das in `Matchday` vorzusehen, bevor überhaupt jemand getippt hat,
ist Aufwand gegen eine Vermutung. Die Entscheidung fällt im Januar mit echten
Daten neu — bis dahin bleibt `Matchday` auf die Regular Season beschränkt.

**Handeintrag-Notweg über ein fest konfiguriertes Admin-Konto, nicht über ein
eigenes Berechtigungsmodell** (Rückfrage vom 2026-08-18, siehe ADR-036).
Anders als die Live-Wetten kennt das Tippspiel keine Host-Rolle, die sich auf
„wer zuerst da war" abbildet. Statt eines Admin-Flags in der Datenbank oder
eines zweiten Login-Wegs meldet sich der Betreiber wie jeder Tipper per
Magic Link an; der Endpunkt für `ScheduleCommands.setResultManually` prüft
nur, ob die authentifizierte Sitzung zur konfigurierten Adresse
(`watchparty.league.admin.email`) gehört. Die Umsetzung selbst steht noch
aus (Stufe 7, Rest).

**Beide Spielmodi stehen gleichwertig nebeneinander, mit einem sichtbaren
Wechsel.** Kein Auswahlschritt vor dem Beitritt (1-f: ein Link genügt) und
keine getrennten Wege, bei denen das Tippspiel nur findet, wer den Pfad kennt.
Der Umschalter ist die Oberflächenseite der Festlegung aus `anforderungen.md`,
dass keiner der beiden Modi der Normalfall ist.

## Akzeptanzkriterien

### Konto und Anmeldung (13.2)

1. Wer seine E-Mail-Adresse angibt, bekommt eine Nachricht mit einem Link, der
   ihn anmeldet. Existiert noch kein Konto zu dieser Adresse, entsteht es beim
   ersten erfolgreichen Anmelden.
2. Der Link ist **einmal** verwendbar und verfällt nach 15 Minuten. Ein
   verbrauchter oder verfallener Link meldet niemanden an.
3. Die Antwort auf eine Anmeldeanfrage ist immer dieselbe, unabhängig davon, ob
   die Adresse bekannt ist. Sonst ist das Formular eine Auskunft darüber, wer
   mitspielt.
4. Anmeldeanfragen sind je Adresse und je Absender-IP begrenzt; darüber hinaus
   wird nichts versendet.
5. Eine Anmeldung hält 90 Tage; danach ist ein neuer Link nötig. Innerhalb
   einer Saison soll niemand sich wöchentlich neu anmelden.
6. Ein Konto trägt einen Anzeigenamen (1 bis 20 Zeichen, dieselbe Regel wie
   `PlayerName`), der in Ranglisten steht. Die E-Mail-Adresse steht in keiner
   Rangliste und ist für andere Mitglieder nicht sichtbar.
7. Ein Konto kann gelöscht werden. Danach sind Adresse und Anzeigename fort,
   die Tipps verschwinden aus allen Ranglisten (13.8).

### Spielplan und Ergebnisse (13.3)

8. Der Server kennt zu jedem Spiel: Saison, Spieltag, Heim- und Gastmannschaft,
   Anstoßzeit und — nach dem Spiel — das Endergebnis.
9. Spielplan und Ergebnisse werden regelmäßig aus dem Feed nachgeführt, ohne
   dass jemand etwas anstößt.
10. Verschiebt der Feed eine Anstoßzeit (Flex-Scheduling), gilt ab dann die
    neue Zeit. Bereits abgegebene Tipps bleiben gültig, auch wenn die neue
    Anstoßzeit vor dem Abgabezeitpunkt liegt. Ein Tipp wird beim Abgeben
    geprüft, nie rückwirkend.
11. Fällt der Feed aus oder liefert er ein Spiel unvollständig, bleibt der
    letzte bekannte Stand stehen. Kein Spiel verschwindet, kein Ergebnis wird
    auf 0:0 gesetzt.
12. Ein Endergebnis kann sich nachträglich korrigieren (Feed oder Handeintrag).
    Die Wertung wird dann neu gerechnet, und die Rangliste ändert sich
    entsprechend. Es gibt keinen eingefrorenen Punktestand, der von den
    Ergebnissen abweicht.
13. Ein abgesagtes oder nicht gewertetes Spiel fällt aus der Wertung: Es bringt
    niemandem Punkte, auch keine 0-Punkte-Zeile mit Wirkung auf Platzierungen.
14. Der Betreiber kann ein Endergebnis von Hand setzen und überschreibt damit
    den Feed. Das ist der Notweg aus 13.7 und nicht der Regelfall.

### Tippen (13.4)

15. Ein angemeldeter Tipper sieht die Spiele eines Spieltags mit Anstoßzeit und
    kann zu jedem Spiel ein Endergebnis tippen (zwei nicht-negative ganze
    Zahlen).
16. Ein Tipp ist bis zum Anstoß des Spiels **änderbar**. Ab dem Anstoß nicht
    mehr — weder ändern noch nachtragen.
17. Ein Tipp gilt für alle Ligen des Tippers gleichzeitig, auch für Ligen, denen
    er erst später beitritt.
18. Wer ein Spiel nicht tippt, bekommt dafür 0 Punkte. Es gibt **keine Strafe**
    — die Nicht-Tipper-Strafe (8.1) ist eine Regel der Live-Wetten und wandert
    nicht mit.
19. Fremde Tipps zu einem Spiel sind erst ab dessen Anstoß sichtbar. Vorher
    liefert der Server sie nicht aus — dieselbe Zusage wie Invariante 4, je
    Spiel statt je Wettfenster.
20. Der eigene Tipp ist jederzeit sichtbar.

### Wertung (13.5)

21. Ein Tipp mit exakt richtigem Ergebnis bringt 6 Punkte.
22. Ein Tipp mit richtiger Tendenz und richtigem Abstands-Eimer, aber nicht
    exaktem Ergebnis, bringt 5 Punkte.
23. Ein Tipp mit richtiger Tendenz, aber falschem Abstands-Eimer, bringt 3
    Punkte.
24. Ein Tipp mit falscher Tendenz bringt 0 Punkte, unabhängig vom Abstand.
25. Die Abstands-Eimer sind 0 / 1–8 / 9–16 / ab 17.
26. Wertungspunkte sind ganzzahlig und nie negativ.
27. Die Wertung ist eine reine Funktion aus Tipp und Endergebnis. Zweimal
    dieselbe Eingabe ergibt zweimal dasselbe Ergebnis, ohne Zustand dazwischen.

### Ligen und Rangliste (13.6)

28. Ein angemeldeter Tipper kann eine Liga anlegen; er ist ihr Verwalter.
29. Eine Liga hat einen Beitrittscode, der sich weitergeben lässt. Wer ihn hat,
    tritt bei.
30. Ein Tipper kann in beliebig vielen Ligen Mitglied sein, ohne dass sich
    seine Tipps vervielfachen.
31. Die Rangliste einer Liga zeigt ihre Mitglieder mit der Summe ihrer
    Wertungspunkte über die gewerteten Spiele der Saison, absteigend.
32. Bei Punktgleichheit entscheidet zuerst die Zahl der exakten Ergebnisse,
    dann die Zahl der richtigen Tendenzen; bleibt es gleich, teilen sich die
    Tipper den Platz.
33. Es gibt zusätzlich eine Rangliste je Spieltag.
34. Ein Mitglied kann eine Liga verlassen; seine Tipps bleiben bestehen und
    zählen weiterhin in seinen übrigen Ligen.
35. Die Ranglisten zweier Ligen sind vollständig getrennt: Wer nicht Mitglied
    ist, steht nicht darin.

### Parallelbetrieb (13.1)

36. Kein Kommando und kein Zustand der Live-Wetten wirkt im Tippspiel und
    umgekehrt.
    Insbesondere ändert `RESET` (8.7) keinen Ligatipp und keine Rangliste, und
    eine Ligawertung bewegt keinen Punktestand einer Watchparty.
37. Der Ausfall des Feeds, der Datenbank oder des Mailversands hält keine
    laufende Watchparty an. Die Live-Wetten bleiben in ihren Invarianten
    funktionsfähig, auch wenn das Tippspiel steht.
38. Beide Spielmodi sind aus derselben Anwendung erreichbar, mit einem
    sichtbaren Wechsel dazwischen.

## Szenarien

Als JGiven-Szenarien auf der Port-zu-Port-Ebene (`teststrategie.md` 2.2), außer
wo anders vermerkt.

**Höchste Stufe zählt, nicht die Summe.**
Angenommen ein Spiel endete 24:17.
Wenn Anna 24:17 tippt, Ben 27:20, Cem 31:10 und Dana 17:24.
Dann hat Anna 6 Punkte, Ben 5, Cem 3 und Dana 0.

**Falsche Tendenz schlägt jeden Abstand.**
Angenommen ein Spiel endete 24:17.
Wenn Anna 17:24 tippt.
Dann hat sie 0 Punkte, obwohl sie ein 1-Score-Game getippt hat.

**Die Grenzen der Abstands-Eimer.**
Angenommen ein Spiel endete 28:20 (Abstand 8).
Wenn Anna 21:13 tippt (Abstand 8) und Ben 30:21 (Abstand 9).
Dann hat Anna 5 Punkte und Ben 3.
*(Zusammen mit den Grenzen 16/17 auf der Einheitenebene, 2.1, als
Property-Test: die Eimergrenzen sind der Ort für Off-by-one.)*

**Unentschieden ist ein eigener Eimer.**
Angenommen ein Spiel endete 20:20.
Wenn Anna 20:20 tippt, Ben 17:17 und Cem 21:20.
Dann hat Anna 6 Punkte, Ben 5 und Cem 0.

**Ein Tipp zählt in allen Ligen.**
Angenommen Anna ist Mitglied in Liga A und Liga B und hat ein Spiel getippt.
Wenn das Spiel gewertet wird.
Dann stehen ihre Punkte in beiden Ranglisten, und sie hat den Tipp einmal
abgegeben.

**Wer später beitritt, bringt seine Tipps mit.**
Angenommen Anna hat drei Spieltage getippt und tritt danach Liga C bei.
Wenn die Rangliste von Liga C gebildet wird.
Dann stehen ihre Punkte aus allen drei Spieltagen darin.

**Nach dem Anstoß ist Schluss.**
Angenommen ein Spiel hat angestoßen.
Wenn Anna einen Tipp dafür abgibt oder ihren bestehenden ändert.
Dann wird das abgelehnt, und der Stand ihres Tipps ist unverändert.

**Eine Verlegung nach vorn entwertet keinen abgegebenen Tipp.**
Angenommen Anna hat ein Spiel getippt, dessen Anstoß für Sonntag 19 Uhr
gemeldet war.
Wenn der Feed den Anstoß auf Sonntag 15 Uhr vorverlegt.
Dann bleibt Annas Tipp gültig und wird gewertet.

**Fremde Tipps bleiben bis zum Anstoß verdeckt.**
Angenommen Anna und Ben sind in derselben Liga und beide haben ein noch nicht
angestoßenes Spiel getippt.
Wenn Ben den Spieltag abruft.
Dann sieht er seinen eigenen Tipp und von Anna nur, *dass* sie getippt hat —
kein Ergebnis, keine Zahl.
*(Leck-Test nach `teststrategie.md` 3.1, wie `VerdeckteTippsStufen`: geprüft
wird die Antwort auf der Leitung, nicht die Anzeige.)*

**Eine Ergebniskorrektur rechnet die Rangliste neu.**
Angenommen ein Spiel wurde mit 24:17 gewertet, Anna führt die Liga an.
Wenn das Ergebnis auf 24:21 korrigiert wird.
Dann ist die Rangliste anhand des korrigierten Ergebnisses neu gebildet.

**Ein abgesagtes Spiel wertet niemand.**
Angenommen ein Spiel ist abgesagt und einige haben es getippt.
Wenn die Rangliste gebildet wird.
Dann bringt dieses Spiel niemandem Punkte, und die Reihenfolge entspricht der
ohne dieses Spiel.

**Ein verbrauchter Anmeldelink meldet niemanden an.**
Angenommen Anna hat einen Anmeldelink angefordert und benutzt.
Wenn sie denselben Link erneut öffnet.
Dann ist sie darüber nicht angemeldet.

**Die Anmeldeantwort verrät nicht, wer ein Konto hat.**
Angenommen zu `anna@example.org` existiert ein Konto und zu
`niemand@example.org` nicht.
Wenn für beide Adressen ein Link angefordert wird.
Dann sind beide Antworten nicht unterscheidbar, und an beide Adressen geht
eine Nachricht — sonst könnte über den Empfang selbst erschlossen werden,
welche Adresse ein Konto hat, und niemand könnte sich über eine neue Adresse
je zum ersten Mal anmelden (Kriterium 1). *Korrigiert am 2026-08-17: Der
ursprüngliche Text dieses Szenarios sagte "nur an die erste Adresse", im
Widerspruch zu Kriterium 1 — auf Rückfrage entschieden zugunsten von
Kriterium 1 (Selbstregistrierung bleibt der einzige Weg zu einem Konto).*

**Die Spielmodi rühren einander nicht an.**
Angenommen Anna ist Host einer Watchparty mit abgerechneten Runden und
zugleich Mitglied einer Liga mit gewerteten Tipps.
Wenn sie `RESET` auslöst.
Dann ist die Watchparty zurückgesetzt, und ihre Ligapunkte, Tipps und
Mitgliedschaften sind unverändert.

**Die Live-Wetten überleben den Ausfall des Tippspiels.**
Angenommen die Datenbank ist nicht erreichbar.
Wenn eine Watchparty eine Runde öffnet, tippt, schließt und auflöst.
Dann läuft die Runde vollständig durch.
*(Der Beleg dafür, dass der Raum-Thread nie auf die Datenbank wartet —
Invariante 2 unter neuen Vorzeichen.)*

**Ein Konto löschen löscht seine Spur.**
Angenommen Anna ist in zwei Ligen und hat gewertete Tipps.
Wenn sie ihr Konto löscht.
Dann steht sie in keiner Rangliste mehr, und keine ihrer Tipps ist mehr
abrufbar.

Auf der Einheitenebene (2.1) zusätzlich: die Wertungsfunktion als
Property-Test (Ergebnis stets 0/3/5/6; exaktes Ergebnis stets 6; Vertauschen
von Tipp und Ergebnis ergibt dieselbe Punktzahl bei gespiegelter Tendenz), die
Eimergrenzen, `LeagueCode`, `EmailAddress`. Auf der Adapterebene (2.3): das
Feed-Mapping gegen aufgezeichnete Antworten, die Migrationen, das
Repository-Verhalten.

## Kritikalität

Das Feature zerfällt in Bereiche mit deutlich verschiedenem Risiko; eine
Pauschaleinstufung wäre hier irreführend.

| Bereich | Stufe | Begründung |
|---|---|---|
| Wertung (`Scoring`, `GameScore`, Eimer) | **HIGH** | Derselbe Fall wie `Settlement`: Eine falsche Punktzahl fällt niemandem auf, und sie wirkt nicht eine Runde lang, sondern über eine ganze Saison. Off-by-one an einer Eimergrenze ist die wahrscheinlichste Einzelursache. Mutation Score ≥ 99 % nach 6.3. |
| Verdeckte Tipps vor Anstoß | **HIGH** | Ein Leck macht das Tippen sinnlos und ist in der Oberfläche unsichtbar — die Begründung aus 6.4 gilt wörtlich weiter. Zusätzlich hoch, weil hier erstmals ein HTTP-Adapter beteiligt ist, bei dem ein zu großzügiges Antwortobjekt reicht. |
| Konto und Anmeldung | **HIGH** | Wer fremde Links errät oder eine fremde Sitzung bekommt, tippt unter fremdem Namen; und hier liegen erstmals personenbezogene Daten. Schaden nicht am Spielabend messbar, sondern rechtlich. |
| Trennung der Spielmodi | **HIGH** | Ein Fehler zerstört den jeweils anderen Spielmodus (36, 37). Eintrittswahrscheinlichkeit nicht niedrig, weil beide sich Prozess und Anwendung teilen und die Live-Wetten ohne Synchronisierung geschrieben sind — ein Ligazugriff auf `Room` aus einem Request-Thread wäre ein Datenrennen, das kein Test *zufällig* findet. Deshalb als eigene Zusage und mit einer ArchUnit-Regel, nicht nur als Sorgfalt. |
| Ligen und Rangliste | MEDIUM | Fehler sind sichtbar und nachrechenbar; korrigierbar, ohne dass ein Abend verloren geht. |
| Spieldaten und Feed | MEDIUM | Ein Ausfall ist laut und nachtragbar (14). Schaden begrenzt, Eintrittswahrscheinlichkeit dagegen hoch — eine unbeauftragte Fremdquelle ändert ihr Format, wann sie will. |
| Anzeige der Spieltage | LOW | Frontend, außerhalb der Teststrategie (§11). |

## Umgesetzt in

Alles Neue liegt in einem eigenen Zweig des Paketbaums, `…/league/`, parallel
zum bestehenden. Die Ringregeln aus ADR-024 gelten unverändert; ArchUnit
bekommt zusätzlich die Regel, dass `league` und der bestehende Raumcode
einander **nicht** importieren.

```
domain/model/league/
  Account            @AggregateRoot, @Identity EmailAddress — Konto des
                     Tippers. Kein separates AccountId: die E-Mail-Adresse
                     selbst ist die Identitaet, ein Feld und ein Index
                     weniger (so wenig personenbezogene Daten wie moeglich,
                     Rueckfrage vom 2026-08-17)
  EmailAddress       @ValueObject — Format, Normalisierung (Kleinschreibung)
  DisplayName        @ValueObject — 1..20 Zeichen (Regel wie PlayerName,
                     eigener Typ: ein Anzeigename ist kein Spielername)
  LoginLinkToken, LoginLink   @ValueObject bzw. @Entity — Anmeldelink,
                     einmalig, verfällt nach 15 Minuten (Kriterium 2)
  SessionToken, AccountSession   @ValueObject bzw. @Entity — angemeldete
                     Sitzung, hält 90 Tage (Kriterium 5)
  ClientIp           @ValueObject — Absenderadresse fürs Rate Limit je IP
  SeasonId           @ValueObject — das Startjahr einer Saison
  Matchday           @ValueObject — Saison + Spieltagsnummer, auf die
                     Regular Season beschränkt (1..18)
  GameId, GameStatus   @ValueObject — Identität (die ID des Feeds selbst)
                     und Stand (SCHEDULED/FINAL/CANCELLED) eines Spiels
  Game               @AggregateRoot, @Identity GameId — Spiel mit Anstoß,
                     Mannschaften, Status, optionalem Endergebnis;
                     mergeFromFeed/applyManualResult als benannte Übergänge
  Team/TeamId        @ValueObject — am Spiel mitgeführt statt in einer
                     eigenen Tabelle normalisiert
  GameScore          @ValueObject — zwei nicht-negative Ganzzahlen; trägt
                     tendency() und margin()
  Tendency           @ValueObject — HEIM / GAST / UNENTSCHIEDEN
  ScoreBucket        @ValueObject — die vier Abstands-Eimer (25)
  Prediction         @AggregateRoot, @Identity — Ergebnistipp eines Kontos
                     zu einem Spiel; Identität ist das Paar (Konto, Spiel)
  LeaguePoints       @ValueObject — Wertungspunkte. Eigener Typ, ausdrücklich
                     nicht Points: eine Liga zahlt keinen Pool aus (ADR-025)
  League             @AggregateRoot, @Identity LeagueId; traegt eine SeasonId
                     — eine Liga gehoert zu genau einer Saison
  LeagueCode         @ValueObject — vorlesbarer Beitrittscode
  Membership         @Entity — Mitgliedschaft mit Beitrittszeitpunkt
domain/service/league/
  Scoring            @Service — (Prediction, GameScore) -> LeaguePoints.
                     Reine Funktion, HIGH, der Kern des Features
  Standings          @Service — Mitglieder + Tipps + Ergebnisse -> Rangliste
                     samt Gleichstandsregel (32)
application/league/
  port/in/           LoginCommands (Link anfordern, Link einlösen),
                     PredictionCommands (Spieltag abrufen, tippen),
                     LeagueCommands (anlegen, beitreten, verlassen,
                     Rangliste), ScheduleCommands (Feed nachführen,
                     Ergebnis von Hand setzen)
  port/out/          AccountRepository, GameRepository,
                     PredictionRepository, LeagueRepository,
                     MailSender, ScheduleFeed
  view/              Projektionen für die Antworten; hier hängt Kriterium 19
                     (verdeckt bis Anstoß) — dieselbe Rolle wie RoomView
adapter/in/http/     REST-Endpunkte, Sitzungscookie, Rate Limit (4)
adapter/out/db/      Repository-Umsetzungen, Flyway-Migrationen (Postgres)
adapter/out/feed/    ESPN-Client und Mapping, Nachführ-Job über den
                     bestehenden Scheduler-Port
adapter/out/mail/    Versand der Anmeldelinks
config/league/       Beans, Datenquelle, Feed- und Mail-Konfiguration
frontend/src/league/ Anmeldung, Spieltagsansicht, Tippformular, Ligen,
                     Ranglisten; dazu ein Moduswechsel in der Hülle
```

Bestehender Code, der sich ändert: `WatchpartyApplication` (Datenquelle,
Scheduler-Jobs), `ArchitectureTest` (neue Stereotypen, Trennungsregel),
`fly.toml` (Datenbank, Secrets), die Startseite (Moduswechsel). **`Room`,
`Player`, `Round`, `Settlement`, `RoomActor`, `RoomView` und der WebSocket-Weg
bleiben unberührt.**

## Was mit den harten Invarianten passiert

Die sechs Invarianten aus CLAUDE.md sind für die Live-Wetten formuliert. Sie
bleiben wörtlich gültig; das Tippspiel stellt ihnen eigene zur Seite. Diese
Abgrenzung ist kein Formalismus — sie ist der Grund, warum das Feature den
bestehenden Code nicht gefährdet.

1. **Aller Raumzustand auf dem Raum-Thread** — unverändert. Neu: *Kein
   Ligacode fasst `Room` oder `Player` an, und kein Raumcode fasst ein
   Liga-Repository an.* Ein Request-Thread, der in den Raum greift, wäre
   genau das Datenrennen, gegen das Invariante 1 gebaut ist. Deshalb die
   ArchUnit-Trennung statt einer Verabredung.
2. **Der Raum-Thread blockiert nie** — unverändert und jetzt schärfer: Er darf
   erst recht nicht auf eine Datenbank, einen Mailversand oder einen Feed
   warten. Der Ligaweg läuft komplett auf Request-Threads, wie jede
   gewöhnliche Spring-Anwendung.
3. **Server ist die einzige Quelle der Wahrheit** — gilt für beide Spielmodi.
   Insbesondere rechnet kein Client Wertungspunkte aus.
4. **Verdeckte Tipps sind eine Anforderung an die Leitung** — gilt sinngemäß
   für das Tippspiel: bis zum Anstoß geht kein fremder Ergebnistipp über die
   Leitung (19).
5. **Punkte ganzzahlig und nullsumme** — die Nullsumme gilt weiter für die
   Live-Wetten. Das Tippspiel hat keine Nullsumme, weil es keinen Einsatz gibt;
   ganzzahlig sind beide. Der eigene Typ `LeaguePoints` hält die beiden
   Rechenwelten auseinander.
6. **Genau eine Server-Instanz** — unverändert (ADR-005). Mit einer Datenbank
   *wäre* eine zweite Instanz technisch denkbar; sie bleibt trotzdem
   ausgeschlossen, solange die Live-Wetten den Zustand im Arbeitsspeicher halten.

## Nötige ADRs

Feature 004 hat ADR-033 belegt. Neu zu schreiben:

| ADR | Entscheidung |
|---|---|
| ADR-034 | Zwei Spielmodi in einer Anwendung, getrennte Modelle statt Wiederverwendung; Trennung per ArchUnit erzwungen |
| ADR-035 | Verwaltetes Postgres für das Tippspiel, mit Flyway-Migrationen; Verhältnis zu ADR-004 (der für die Live-Wetten unverändert gilt) |
| ADR-036 | Konten mit Magic Link statt Kennwort; Sitzungsdauer, Einmaligkeit, Rate Limit |
| ADR-037 | ESPN als Feed hinter dem Port `ScheduleFeed`; Nachführ-Takt, Verhalten bei Ausfall, Handeintrag als Notweg, Wechsel der Quelle als Adapter |
| ADR-038 | Wertung als reine Funktion mit „höchste Stufe zählt"; Fachbegriffe der Liga (Ergebnistipp, Wertungspunkte, Abstand, Rangliste) analog ADR-022 |
| ADR-039 | HTTP statt WebSocket für das Tippspiel — Anfrage/Antwort reicht, wo nichts in Sekunden geschieht |

Dazu Nachträge: CLAUDE.md (Invarianten je Spielmodus, Aufbau),
`anforderungen.md` (Kapitel 13 samt Anhang A), `teststrategie.md` (die neuen
Bereiche in der Einstufungstabelle 6.4).

Bereits erledigt: der Geltungsbereich in `anforderungen.md` (Präambel „Zwei
Spielmodi", Kapitel 11 und 12, Geltungs-Spalte in Anhang A) und in
`offene-entscheidungen.md` (Geltung je Eintrag). Kapitel 13 selbst kommt
stufenweise mit dem Bau — eine `backend`-Regel ohne grünes Szenario bricht
die Feature-Abdeckung (`teststrategie.md` 7.1).

## Reihenfolge des Baus

Jede Stufe ist für sich abgeschlossen und einzeln einsetzbar; keine setzt
voraus, dass die folgende je gebaut wird.

| # | Stufe | Ergebnis | Umfang | Stand |
|---|---|---|---|---|
| 0 | Entscheidungen | ADR-034 bis ADR-039 stehen, Kapitel 13 ist in `anforderungen.md`. Die fachlichen und technischen Fragen selbst sind seit dem 2026-08-17 beantwortet (siehe „Bewusste Festlegungen") | S | **erledigt** |
| 1 | Wertung | `Scoring`, `GameScore`, `ScoreBucket`, `LeaguePoints` samt Szenarien und Property-Tests. **Ohne jede Infrastruktur** — der HIGH-Teil zuerst, solange nichts drumherum ablenkt | S | **erledigt** (2026-08-17): `domain/model/league`, `domain/service/league`, Kapitel 13.5 in `anforderungen.md`, Anhang A 13.5-a bis 13.5-e, Mutation Score 100 % |
| 2 | Persistenz | Datenbank, Migrationen, Repository-Ports und -Adapter, Testaufbau. Die Stufe, die ADR-004 einordnet | M | **erledigt** (2026-08-17): Postgres-Anbindung (ADR-035) mit Flyway unter `adapter/out/db`, Testaufbau mit Testcontainers, erster Baustein `Account` (nur Datenhaltung — Anmeldefluss folgt in Stufe 3) samt `AccountRepository`/`AccountRepositoryJdbc`, ArchUnit-Trennung jetzt auch auf dem Anwendungsring |
| 3 | Konten | Magic Link, Mailversand, Sitzung, Rate Limit, Löschung | M | **erledigt** (2026-08-17, Mailversand ergänzt 2026-08-18): `LoginLink`/`LoginLinkToken`, `AccountSession`/`SessionToken`, `ClientIp`, `LoginService` (`LoginCommands`), Rate Limit im Arbeitsspeicher, Kapitel 13.2 in `anforderungen.md`, Anhang A 13.2-a bis 13.2-h. Mailversand über `SmtpMailSender` (Strato, Rückfrage vom 2026-08-18) mit `LoggingMailSender` als Rückfallebene ohne Zugangsdaten — die echten Zugangsdaten selbst sind Stufe 8 |
| 4 | Spieldaten | Feed-Anbindung, Nachführ-Job, Handeintrag, Umgang mit Verlegung, Absage, Korrektur | M | **erledigt** (2026-08-17): `Game`/`GameId`/`GameStatus`, `Matchday`, `Team`/`TeamId`, `SeasonId`, `EspnScheduleFeed` gegen aufgezeichnete Antworten, `ScheduleSyncService`/`ScheduleSyncJob` über den geteilten `Scheduler`-Port, Kapitel 13.3 in `anforderungen.md`, Anhang A 13.3-a bis 13.3-g |
| 5 | Tippen | Spieltag abrufen, tippen, Abgabeschluss, Verdeckung bis Anstoß | M | **erledigt** (2026-08-17): `Prediction`/`PredictionId`, `PredictionService` (Kickoff-Prüfung), `PredictionView` als HIGH-kritische Sichtbarkeitsregel (Mutation Score 100 %), Kapitel 13.4 in `anforderungen.md`, Anhang A 13.4-a bis 13.4-e. Kriterium 17 (gilt für alle Ligen) und 18 (0 Punkte ohne Strafe) folgen strukturell schon, prüfbar erst mit Stufe 6 |
| 6 | Ligen | Anlegen, Beitreten, Verlassen, Rangliste je Saison und je Spieltag | M | **erledigt** (2026-08-17): `League`/`LeagueId`/`LeagueCode`/`LeagueName`/`Membership`, `Standings` (Gleichstandsregel, geteilter Platz), `LeagueService`, Kapitel 13.6 in `anforderungen.md`, Anhang A 13.6-a bis 13.6-j. Damit auch die bislang zurückgestellten Kriterien 12 (Ranglisten-Teil), 17 und 18 geprüft |
| 7 | Oberfläche | Moduswechsel, Anmeldung, Spieltagsansicht, Ranglisten | L | **erledigt** (2026-08-18): `adapter/in/http` (Login-/Prediction-/LeagueController, Sitzungscookie über `AccountArgumentResolver`), `frontend/src/league` (Anmeldung, Spieltag, Ligen, Ranglisten), Moduswechsel in `App.jsx`. `LeagueHttpFlowTest` prüft den REST-Weg end-to-end gegen echtes Postgres; das Frontend ist nur bis `npm run build` und manuell per `curl` durch den vollständigen Ablauf verifiziert, nicht in einem echten Browser — dafür fehlt in dieser Umgebung ein Werkzeug (Playwright o. ä.). **Offen:** kein Endpunkt für den Handeintrag (Kriterium 14, `ScheduleCommands.setResultManually`) — hängt an der noch unentschiedenen Frage, wie sich der Betreiber ausweist (`offene-entscheidungen.md`); folgt später |
| 8 | Betrieb | Sicherung der Datenbank, Überwachung des Feeds, Datenschutzerklärung, Secrets | M | offen |

Stufe 1 vor Stufe 2 ist Absicht: Der einzige Teil, an dem eine Saison
tatsächlich hängt, lässt sich vollständig prüfen, bevor die erste Tabelle
existiert.

## Was dieses Feature ins Projekt holt, das es heute nicht gibt

Der Vollständigkeit halber, weil jeder dieser Punkte eigene Folgekosten hat und
keiner davon aus dem bisherigen Betrieb bekannt ist:

- **Eine Datenbank** mit Migrationen, Sicherung und Rückspielprobe. Ein
  Fly-Volume ohne Sicherung ist für einen Abend vertretbar (ADR-023), für eine
  Saison nicht — deshalb verwaltetes Postgres. Die Rückspielprobe gehört
  trotzdem dazu: Eine Sicherung, die nie zurückgespielt wurde, ist eine
  Vermutung.
- **Personenbezogene Daten.** E-Mail-Adressen verlangen Datenschutzerklärung,
  Löschkonzept und einen Vertrag mit dem Mailversender. Das Repository ist
  öffentlich (ADR-028) — Zugangsdaten gehören ausschließlich in Fly-Secrets.
- **Eine Außenabhängigkeit ohne Zusage.** Der Feed kann sich ändern oder
  wegfallen; das ist kein Restrisiko, sondern eine Wartungsaufgabe über die
  Saison.
- **Ein zweiter Zeitbegriff.** Die Live-Wetten kennen 15 Sekunden, das Tippspiel
  kennt
  Anstoßzeiten in Zeitzonen mit Sommerzeitwechsel mitten in der Saison.
- **Längere Testläufe.** Datenbanktests und Feed-Attrappen gegen das
  10-Minuten-Budget aus `teststrategie.md` 10 — das ist beim Aufbau von
  Stufe 2 zu messen, nicht am Ende festzustellen.

## Offene Fragen

**Keine mehr.** Die sechs Fragen dieses Abschnitts sind am 2026-08-17
beantwortet und stehen als Festlegungen oben; sie sind damit aus
`offene-entscheidungen.md` verschwunden, wie es dort vorgesehen ist. Ihre
dauerhafte Heimat sind ADR-034 bis ADR-039 und Kapitel 13 in
`anforderungen.md` — beides entsteht mit Stufe 0 des Baus.

Was offen *bleibt*, ist nicht entscheidbar, sondern zu beobachten: ob eine
Saison über Postgres tatsächlich unauffällig läuft, ob der ESPN-Feed die Saison
durchhält und ob die Rangliste bei Spätbeitretern am Tisch als fair empfunden
wird. Das gehört auf den Bogen in `probelauf.md`, nicht hierher.
