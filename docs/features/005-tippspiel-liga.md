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
Dann sind beide Antworten nicht unterscheidbar, und nur an die erste Adresse
geht eine Nachricht.

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
  Account            @AggregateRoot, @Identity AccountId — Konto des Tippers
  EmailAddress       @ValueObject — Format, Normalisierung (Kleinschreibung)
  DisplayName        @ValueObject — 1..20 Zeichen (Regel wie PlayerName,
                     eigener Typ: ein Anzeigename ist kein Spielername)
  LoginLink          @Entity — einmalig, mit Verfall (Kriterium 2)
  Season/SeasonId    @ValueObject — die Saison als Ganzes
  Matchday           @ValueObject — Saison + Spieltagsnummer
  Game               @AggregateRoot, @Identity GameId — Spiel mit Anstoß,
                     Mannschaften, Status, optionalem Endergebnis
  Team/TeamId        @ValueObject
  GameScore          @ValueObject — zwei nicht-negative Ganzzahlen; trägt
                     tendency() und margin()
  Tendency           @ValueObject — HEIM / GAST / UNENTSCHIEDEN
  ScoreBucket        @ValueObject — die vier Abstands-Eimer (25)
  Prediction         @AggregateRoot, @Identity — Ergebnistipp eines Kontos
                     zu einem Spiel; Identität ist das Paar (Konto, Spiel)
  LeaguePoints       @ValueObject — Wertungspunkte. Eigener Typ, ausdrücklich
                     nicht Points: eine Liga zahlt keinen Pool aus (ADR-025)
  League             @AggregateRoot, @Identity LeagueId
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
adapter/out/db/      Repository-Umsetzungen, Migrationen
adapter/out/feed/    Feed-Client und Mapping, Nachführ-Job über den
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
| ADR-035 | Datenbank für das Tippspiel — welche, wo, mit welchen Migrationen; Verhältnis zu ADR-004 (der für die Live-Wetten unverändert gilt) |
| ADR-036 | Konten mit Magic Link statt Kennwort; Sitzungsdauer, Einmaligkeit, Rate Limit |
| ADR-037 | Externer Feed als Quelle für Spielplan und Ergebnisse; Wahl der Quelle, Nachführ-Takt, Verhalten bei Ausfall, Handeintrag als Notweg |
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

| # | Stufe | Ergebnis | Umfang |
|---|---|---|---|
| 0 | Entscheidungen | Die offenen Fragen unten sind beantwortet, ADR-034 bis ADR-039 stehen, Kapitel 13 ist in `anforderungen.md` | S |
| 1 | Wertung | `Scoring`, `GameScore`, `ScoreBucket`, `LeaguePoints` samt Szenarien und Property-Tests. **Ohne jede Infrastruktur** — der HIGH-Teil zuerst, solange nichts drumherum ablenkt | S |
| 2 | Persistenz | Datenbank, Migrationen, Repository-Ports und -Adapter, Testaufbau. Die Stufe, die ADR-004 einordnet | M |
| 3 | Konten | Magic Link, Mailversand, Sitzung, Rate Limit, Löschung | M |
| 4 | Spieldaten | Feed-Anbindung, Nachführ-Job, Handeintrag, Umgang mit Verlegung, Absage, Korrektur | M |
| 5 | Tippen | Spieltag abrufen, tippen, Abgabeschluss, Verdeckung bis Anstoß | M |
| 6 | Ligen | Anlegen, Beitreten, Verlassen, Rangliste je Saison und je Spieltag | M |
| 7 | Oberfläche | Moduswechsel, Anmeldung, Spieltagsansicht, Ranglisten | L |
| 8 | Betrieb | Sicherung der Datenbank, Überwachung des Feeds, Datenschutzerklärung, Secrets | M |

Stufe 1 vor Stufe 2 ist Absicht: Der einzige Teil, an dem eine Saison
tatsächlich hängt, lässt sich vollständig prüfen, bevor die erste Tabelle
existiert.

## Was dieses Feature ins Projekt holt, das es heute nicht gibt

Der Vollständigkeit halber, weil jeder dieser Punkte eigene Folgekosten hat und
keiner davon aus dem bisherigen Betrieb bekannt ist:

- **Eine Datenbank** mit Migrationen, Sicherung und Rückspielprobe. Ein
  Fly-Volume ohne Sicherung ist für einen Abend vertretbar (ADR-023), für eine
  Saison nicht.
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

Zu jeder steht eine Empfehlung, keine Festlegung. Die fünf, die den Bau
blockieren, stehen seit dem Beschluss auch in `offene-entscheidungen.md` —
dort sind sie zu beantworten, hier bleiben sie als Teil des Antrags stehen.

**Welche Datenbank?** Empfehlung: verwaltetes Postgres statt SQLite auf dem
vorhandenen Volume. Nicht wegen der Last — die ist lächerlich klein —, sondern
wegen der Sicherung: Ein Volume ist an eine Maschine gebunden, und der Verlust
einer Saison ist etwas anderes als der Verlust eines Abends.

**Welche Feed-Quelle?** Die offen erreichbaren ESPN-Endpunkte sind bequem und
unbeauftragt: keine Zusage, keine Nutzungserlaubnis, jederzeit änderbar. Eine
bezahlte Quelle kostet wenig und ist verlässlich. Empfehlung: mit ESPN
anfangen, aber hinter dem Port `ScheduleFeed`, damit ein Wechsel ein Adapter
ist und kein Umbau.

**Welche Liga wertet ab wann?** Empfehlung: Eine Liga wertet die ganze Saison,
auch für Mitglieder, die später beitreten (Kriterium 17). Die Alternative — erst
ab Beitritt — ist gerechter für Frühstarter und verlangt eine Zeitachse in der
Rangliste. Am ersten Spieltag zu klären, nicht am Schreibtisch.

**Wie viele Saisons gleichzeitig?** Alles oben ist auf eine laufende Saison
geschrieben. Ob eine Liga über Saisons hinweg fortbesteht oder jede Saison eine
neue Liga ist, ändert das Modell (`League` mit oder ohne `SeasonId`).
Empfehlung: Liga je Saison, das ist die einfachere Zeitachse.

**Braucht die Liga Playoffs?** Anstoßzeiten und Spieltage der Playoffs folgen
anderen Regeln als die Regular Season. Empfehlung: erste Saison ohne Playoffs,
danach entscheiden, ob es sich gelohnt hat.

**Wie sichtbar sind die Live-Wetten für Ligamitglieder?** Ob jemand, der nur
tippt, die Watchparty überhaupt angeboten bekommt — eine Frage an die
Oberfläche, die sich am ersten Spieltag schneller beantwortet als vorher.
