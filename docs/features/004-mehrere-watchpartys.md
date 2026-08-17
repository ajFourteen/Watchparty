# 004 — Mehrere Watchpartys gleichzeitig

## Anlass

Bisher ist der Server *der* Raum: eine Instanz, ein Spielraum, ein Snapshot.
Damit kann an einem Spieltag nur ein Wohnzimmer die App benutzen. Künftig
sollen mehrere, voneinander getrennte Watchpartys gleichzeitig laufen — andere
Wohnzimmer, andere Menschen, eigene Punktekonten und eigene Runden. *Innerhalb*
einer Watchparty bleibt alles wie es ist, insbesondere läuft weiterhin immer
nur eine Wettrunde zur Zeit.

Damit wird eine bewusst getroffene Entscheidung zurückgenommen: „Keine
mehreren parallelen Räume" stand in `offene-entscheidungen.md` unter *bewusst
ausgeschlossen* und in `anforderungen.md` unter *out of scope*. Beide Stellen
fallen weg, die Begründung dafür steht in ADR-033.

Was **nicht** zurückgenommen wird: ADR-005 (genau eine Server-Instanz) und
ADR-004 (Zustand im Arbeitsspeicher) bleiben unverändert gültig. Eine Instanz
hält künftig viele Räume — das ist kein Widerspruch, sondern der Grund, warum
der Umbau ohne Sharding und ohne externe Zustandshaltung auskommt. Zu
korrigieren ist nur die *Formulierung* des Arguments, die an mehreren Stellen
lautet „zwei Instanzen wären zwei getrennte Räume": Künftig wären zwei
Instanzen zwei getrennte *Mengen* von Räumen (CLAUDE.md Invariante 6,
`fly.toml`, ADR-005, ADR-018).

## Betroffene Anforderungen

Bestehend und **geändert**: 1-b (aus „genau ein Spielraum" wird „mehrere
getrennte Watchpartys; je Watchparty immer nur eine Runde gleichzeitig"), 1-e
(der Beitritt verlangt einen Namen und, wer einer bestehenden Watchparty
beitritt, deren Code).

Bestehend und **unberührt**: die gesamte Wett-Ökonomie (2, 3, 6, 7, 8), der
Wettkatalog (4), Fenster und Ablauf (5, 9), die Anzeige (10) — sie gelten ab
jetzt je Raum, ihr Inhalt ändert sich nicht. Ebenso 1-c (keine Persistenz über
Abende hinweg) und 1-d (Snapshot mit sechs Stunden Verfallszeit), die jetzt je
Raum gelten. 1-f (Link öffnen genügt) bleibt gültig und wird durch den
Beitrittslink sogar wörtlicher erfüllt.

Anhang A ergänzt um:

| ID | Marke | Warum neu |
|---|---|---|
| 1-g | backend | Ein eigenes Kommando erzeugt einen Raum; wer ihn erzeugt, ist sein Host. Ohne diese Regel gäbe es keinen Weg zum ersten Raum. |
| 1-h | backend | Der Code ist vierstellig alphanumerisch und wird unabhängig von Groß-/Kleinschreibung angenommen. Er wird vorgelesen — die Toleranz ist Fachlichkeit, keine Bequemlichkeit. |
| 1-i | backend | Räume sind vollständig getrennt: Keine Nachricht und kein Kommando eines Raums wirkt auf einen anderen. Die zentrale neue Zusicherung. |
| 1-j | backend | Ein Raum ohne Aktivität wird nach sechs Stunden verworfen, samt seinem Snapshot. Vorher erledigte das der Neustart. |
| 1-k | frontend | Der Code der eigenen Watchparty ist ständig sichtbar, um ihn vorlesen zu können. |
| 1-l | frontend | `/join/CODE` füllt das Code-Feld vor, sodass ein geteilter Link nur noch den Namen verlangt. |

## Akzeptanzkriterien

**Beitritt und Erzeugung (1-g, 1-h)**

1. Ein `CREATE_ROOM` erzeugt einen neuen Raum, ordnet die Sitzung ihm zu und
   macht den Beitretenden zu seinem Host (ADR-016, unverändert — er ist der
   erste Teilnehmer). Erzeugen und Beitreten sind seit ADR-040 getrennte
   Kommandos, keine Ausprägungen desselben `JOIN` mehr — Erzeugen bringt den
   Raum erst in die Welt, Beitreten setzt ihn voraus.
2. Ein `JOIN` mit dem Code eines bestehenden Raums tritt diesem Raum bei; der
   Beitretende ist ein gewöhnlicher Teilnehmer.
3. Ein Code, zu dem kein Raum existiert — oder gar keiner —, führt zu einer
   Fehlermeldung und erzeugt **keinen** Raum. Ein Tippfehler beim Vorlesen
   darf niemanden in ein leeres eigenes Wohnzimmer setzen, wo er auf
   Mitspieler wartet, die längst in einem anderen Raum sitzen.
4. Der Code ist vierstellig aus Ziffern und Buchstaben. Die Eingabe wird
   unabhängig von Groß-/Kleinschreibung angenommen; angezeigt wird er in
   einer festen Schreibweise (Großbuchstaben).
5. Beim Erzeugen wird ein Code vergeben, der noch von keinem lebenden Raum
   belegt ist.
6. Die Antwort auf einen erfolgreichen Beitritt nennt den Code des Raums
   (`WELCOME`), damit der Client ihn anzeigen und für den Reconnect merken
   kann.

**Trennung der Räume (1-i)**

7. Eine Sitzung gehört zu höchstens einem Raum. Solange sie zu keinem gehört,
   erhält sie nur Antworten auf ihre eigenen Nachrichten und keine
   Zustandsmeldung.
8. Eine Zustandsmeldung geht ausschließlich an die Sitzungen des Raums, um
   dessen Zustand es geht.
9. Ein Host-Kommando (`OPEN_BET`, `CLOSE_BET`, `RESOLVE`, `ANNUL`, `RESET`)
   wirkt ausschließlich in dem Raum, zu dem die absendende Sitzung gehört.
   Wer in einem Raum Host ist, ist in keinem anderen dadurch Host.
10. Punktekonten, Rundenzähler, Wettfenster und Verlauf sind je Raum
    getrennt. Zwei Räume, die dieselbe Wette öffnen, haben eigene Pools.
11. Ein Verbindungsabbruch, ein Reconnect oder ein Verpassen von Runden
    (8.1) wirkt nur auf den Raum des betroffenen Spielers — insbesondere
    lässt der Verpasste-Runden-Zähler eines Raums die Spieler eines anderen
    unberührt.
12. Ein Token (ADR-014) erkennt einen Spieler nur in dem Raum wieder, in dem
    er ausgegeben wurde. Dieselbe Person kann in zwei Räumen zwei getrennte
    Spieler sein.
13. Verdeckte Tipps (Invariante 4) gelten unverändert je Raum: Solange ein
    Fenster offen ist, geht kein einzelner Tipp über die Leitung — auch nicht
    an eine Sitzung eines anderen Raums, für die die Verdeckung eines fremden
    Fensters nichts bedeuten würde.

**Lebensdauer (1-j)**

14. Ein Raum, in dem sechs Stunden lang keine Aktivität stattgefunden hat,
    wird verworfen. „Aktivität" ist jedes Kommando, das seinen Zustand
    verändert — dieselbe Marke, die auch den Snapshot fortschreibt.
15. Mit dem Raum verschwindet sein Snapshot von der Platte. Sonst wächst das
    Volume über Abende hinweg mit Dateien, die ohnehin nicht mehr geladen
    würden (1-d).
16. Ein Raum ohne Teilnehmer bleibt bestehen, bis diese sechs Stunden
    ablaufen. Das ist Absicht: Sind in der Halbzeitpause alle Handys
    eingeschlafen, ist der Raum kurzzeitig leer, ohne dass der Abend vorbei
    wäre.
17. Beim Start werden alle Snapshots geladen, die nicht verfallen sind, und
    ihre Räume wiederhergestellt (ADR-023, jetzt für n Räume statt für
    einen).

**Zugang und Anzeige (1-k, 1-l)**

18. Das Beitrittsformular hat neben dem Namen ein optionales Code-Feld. Ist
    es leer, heißt die Schaltfläche „Raum erstellen"; ist es gefüllt, heißt
    sie „Mitspielen".
19. Nach Beitritt oder Erzeugung ist der Code des Raums ständig sichtbar, in
    einer Form, die sich am Tisch vorlesen lässt.
20. `watchparty.fourteen-it.de/join/abc1` liefert die Anwendung aus und füllt
    das Code-Feld mit `abc1` vor; einzugeben bleibt nur der Name.
21. Ein Reconnect landet im selben Raum wie zuvor, ohne dass der Code erneut
    eingegeben werden muss.

## Szenarien

Als JGiven-Szenarien auf der Port-zu-Port-Ebene (`teststrategie.md` 2.2)
entstehen die Kriterien 1 bis 3, 7 bis 14 und 17. Sie brauchen eine
Erweiterung der Stufen, die heute stillschweigend von einem Raum ausgehen.

**Wer eine Watchparty erzeugt, ist ihr Host.**
Angenommen niemand ist verbunden.
Wenn Anna eine Watchparty erzeugt.
Dann existiert eine Watchparty mit einem vierstelligen Code, Anna ist ihr Host
und der Code steht in ihrer Begrüßung.

**Wer den Code kennt, kommt in dieselbe Watchparty.**
Angenommen Anna hat eine Watchparty erzeugt.
Wenn Ben mit deren Code beitritt.
Dann sind Anna und Ben in derselben Watchparty, Anna ist Host und Ben nicht.

**Ein unbekannter Code ist ein Fehler, kein neuer Raum.**
Angenommen Anna hat eine Watchparty erzeugt.
Wenn Ben mit einem Code beitritt, den es nicht gibt.
Dann bekommt Ben eine Fehlermeldung, ist in keiner Watchparty, und es existiert
weiterhin genau eine.

**Groß- und Kleinschreibung des Codes ist gleichgültig.**
Angenommen Anna hat eine Watchparty erzeugt.
Wenn Ben mit demselben Code in anderer Schreibweise beitritt.
Dann ist Ben in Annas Watchparty.

**Zwei Watchpartys rechnen getrennt ab.**
Angenommen Anna ist Host in Watchparty A, Ben ist Host in Watchparty B, und in
beiden ist je ein weiterer Spieler.
Wenn in A eine Runde geöffnet, getippt und aufgelöst wird.
Dann haben sich in A die Punktekonten geändert und in B kein einziges, und B
ist weiterhin in der Phase IDLE.

**Kein Zustand verlässt seine Watchparty.**
Angenommen Anna ist in Watchparty A, Ben in Watchparty B.
Wenn in A eine Runde geöffnet und getippt wird.
Dann hat Ben keine einzige Nachricht über A erhalten — insbesondere keinen
Zustand, keinen Tipp und keinen Teilnehmer von A.

**Host ist man in einem Raum, nicht überhaupt.**
Angenommen Anna ist Host in Watchparty A und Ben ist Host in Watchparty B.
Wenn Ben eine Wette öffnet.
Dann ist in B ein Fenster offen und in A nicht.

**RESET räumt nur die eigene Watchparty.**
Angenommen Anna ist Host in Watchparty A mit einem Mitspieler, Ben ist Host in
Watchparty B mit einem Mitspieler, und in beiden ist bereits eine Runde
abgerechnet.
Wenn Anna RESET auslöst.
Dann ist A leer und zurückgesetzt, und B hat unverändert seine Spieler,
Punktestände und seinen Rundenzähler.

**Ein Token gilt nur in seiner Watchparty.**
Angenommen Anna ist in Watchparty A beigetreten und hat ein Token erhalten.
Wenn sie mit diesem Token in Watchparty B beitritt.
Dann ist sie dort eine neue Spielerin mit Startguthaben, und ihr Konto in A
bleibt davon unberührt.

**Verpasste Runden zählen nur im eigenen Raum.**
Angenommen Anna ist in A und Ben in B, beide getrennt.
Wenn in A drei Runden ohne Tipp ablaufen.
Dann ist Anna pausiert (8.1-d) und Ben hat keine Runde verpasst.

**Ein Raum ohne Aktivität verschwindet nach sechs Stunden.**
Angenommen es gibt eine Watchparty, in der zuletzt vor sechs Stunden etwas
passiert ist, und eine, in der vor einer Stunde etwas passierte.
Wenn aufgeräumt wird.
Dann existiert nur noch die jüngere, und der Snapshot der älteren ist von der
Platte verschwunden.

**Ein Neustart bringt alle Watchpartys zurück.**
Angenommen es gibt drei Watchpartys mit unterschiedlichen Punktekonten, eine
davon mit einer offenen Wette, und der Server startet neu.
Wenn alle Spieler sich wieder verbinden.
Dann findet jeder seine eigene Watchparty mit ihrem Code, ihren Konten und
ihrem Rundenstand wieder.

Kriterium 4 (Codeformat) und 5 (Kollisionsfreiheit) gehören zum Value Object
und werden auf der Einheitenebene geprüft (`teststrategie.md` 2.1), Kriterium
13 als Erweiterung der bestehenden Positivliste in `VerdeckteTippsStufen` um
den Fall „fremde Sitzung". Die Kriterien 18 bis 21 sind Oberfläche und liegen
nach `teststrategie.md` §11 außerhalb der Teststrategie; sie werden von Hand
mit zwei Browsern nachvollzogen, davon einer über den `/join/CODE`-Link.

## Kritikalität

**HIGH.**

Die Wirkung eines Fehlers reicht hier über eine Runde und über einen Abend
hinaus, weil sie *fremde* Abende betrifft. Fehlt die Zuordnung an einer
einzigen Stelle, geht eine Zustandsmeldung an alle Verbundenen — dann sehen
Fremde die Tipps, Namen und Punkte einer anderen Gruppe, und je nach Phase
bricht das zusätzlich Invariante 4, weil die Verdeckung eines fremden Fensters
für den falschen Empfänger nichts bedeutet. Ein `RESET` oder ein `RESOLVE`, das
im falschen Raum landet, zerstört den Abend von Leuten, die davon nichts
mitbekommen und es auch nicht zurückdrehen können — anders als bei 003 gibt es
hier kein Korrektiv am Tisch, weil niemand am Tisch die andere Gruppe kennt.

Die Eintrittswahrscheinlichkeit ist ausdrücklich nicht niedrig: Der heutige
Code geht an vielen Stellen von der Einzahl aus, sichtbar etwa daran, dass der
Empfängerkreis einer Zustandsmeldung schlicht alle bekannten Sitzungen ist.
Jede dieser Stellen ist ein möglicher Durchlässer, und keine davon fällt beim
Entwickeln mit einem einzigen Browser auf. Deshalb ist die Trennung nicht als
Nebeneffekt der Umbauten formuliert, sondern als eigene Zusicherung 1-i mit
eigenen Szenarien.

## Umgesetzt in

Domäne: `RoomCode` (neues Value Object, `@ValueObject` nach ADR-027— der
Name folgt der Fachsprache aus diesem Dokument und ADR-033, nicht dem
`*Id`-Muster der übrigen Identitäten, weil er anders als sie für Menschen
lesbar und vorlesbar sein muss), `Room` (bekommt das `@Identity`-Feld —
vorher trug es ausdrücklich keines, mit Begründung im Javadoc, und genau
diese Begründung fällt weg), `RoomSnapshot` (Code im Dateiformat,
`SCHEMA_VERSION` 2).

Anwendung: `RoomActor` — ein gemeinsamer Loop für alle Räume, statt eines
Actors je Raum. Damit bleibt Invariante 1 wörtlich wahr (ein Thread, aller
Zustand) und die Threadzahl wächst nicht mit der Zahl der Watchpartys; dass
ein Raum die anderen ausbremst, ist durch Invariante 2 ausgeschlossen, denn
der Loop wartet ohnehin nie. Aus dem einzelnen `Room`-Feld wird eine
Zuordnung Code → Raum, aus der Sitzungszuordnung eine, die Raum *und* Spieler
kennt — eine Sitzung landet darin erst, wenn `CREATE_ROOM` oder `JOIN`
erfolgreich war, nicht schon beim Verbindungsaufbau, und der Empfängerkreis
einer Zustandsmeldung wird entsprechend auf die Sitzungen des betroffenen
Raums eingeschränkt. Dazu ein wiederkehrender Aufräum-Sweep nach 1-j über
den bestehenden `Scheduler`-Port (Intervall eine Stunde, siehe „Bewusste
Festlegungen"). `RoomView` bleibt unverändert — sie projiziert einen Raum,
und das tut sie weiter. Der Eingangs-Port `RoomCommands` bekommt seit
ADR-040 ein eigenes `createRoom(sessionId, name)` neben `join`, statt eines
optionalen vierten Arguments auf `join` allein — Erzeugen und Beitreten
sind fachlich verschiedene Vorgänge, keine Ausprägungen desselben.

Ports: `SnapshotRepository` (`save` mit Code im Snapshot, `loadAll` beim
Start, `delete` beim Verfall — beide nicht blockierend wie `save`, siehe
Invariante 2), `ClientGateway` unverändert — er spricht schon heute
Sitzungs-IDs und muss von Räumen nichts wissen.

Adapter und Konfiguration: **Anders als hier ursprünglich vorgesehen hängt
der Raumbezug nicht an der WebSocket-Verbindung, sondern reist auf dem
Kommando selbst** — genau wie Name und Token es heute schon tun. Das
erfüllt jedes Kriterium unverändert (sie sind verhaltens-, nicht
mechanismusbezogen), erspart aber das Auslesen von Query-Parametern aus dem
WebSocket-Handshake und passt genau zum Beitrittsformular, das Name und
Code gemeinsam abschickt. `GameWebSocketHandler` übersetzt seit ADR-040
zwei Frame-Typen (`CREATE_ROOM`, `JOIN`) statt eines; `WebSocketConfig`
bleibt unverändert. Eine noch nicht beigetretene Sitzung steht in keiner
Empfängerliste, was Kriterium 7 weiterhin ohne Sonderfall erfüllt. Dazu
`SnapshotStore` und `SnapshotConfig` (aus einem Dateipfad wird ein
Verzeichnis, eine Datei je Raum; der Schreib-Thread bleibt einer, jetzt mit
eigener Warteschlange für Löschaufträge), `RoomConfig` unverändert. Neu:
`config/WebConfig` leitet `/join/{code}` auf `index.html` weiter — ohne sie
liefe der Beitrittslink in einen 404, weil Spring Boots eingebaute
Startseite nur `/` selbst bedient. Sie lebt bewusst in `config`, nicht in
einem eigenen Adapter-Unterpaket: `config` ist von den Ring-Regeln
ausdrücklich ausgenommen, eine reine Weiterleitungsregel rechtfertigt kein
neues Paket samt eigenem Ring-Eintrag. `WATCHPARTY_SNAPSHOT_PATH` in
`fly.toml` zeigt künftig auf ein Verzeichnis unter `/data`.

Frontend: `useRoom.js` (`CREATE_ROOM` ohne Code, `JOIN` mit Code — dieselbe
Unterscheidung, die die Schaltfläche schon trifft, ein Token je Raum in
`localStorage` unter dem jeweiligen Code, der zuletzt betretene Code separat
gemerkt fürs automatische Wiederverbinden), das Beitrittsformular in
`App.jsx` mit optionalem Code-Feld und wechselnder Schaltfläche, die
ständige Anzeige des Codes im Kopfbereich, `/join/CODE` als reines
Vorbefüllen des Code-Felds beim ersten Rendern (kein Routing-Paket nötig).

Tests: `WatchpartyTrennungStufen`/`WatchpartyTrennungScenarioTest` als neues
Szenario-Bündel für die Trennung (der Kern dieses Features, Kritikalität
HIGH), eine Erweiterung von `RaumStufen` (der gemeinsamen Basis aller
Port-to-Port-Stufen), die bei jedem ersten Beitritt eines Szenarios den vom
Server vergebenen Code merkt und ihn für alle weiteren Beitritte desselben
Szenarios mitschickt — dreizehn bestehende Stufen-Klassen bleiben dadurch
unverändert lauffähig. `WiederanlaufStufen`/`WiederanlaufScenarioTest` um
mehrere Watchpartys und den Aufräum-Sweep, `VerdeckteTippsStufen` um die
fremde Sitzung, ein neuer `RoomCodeTest` auf Einheitenebene für Form und
Faltung. Seit ADR-040 rufen `RaumStufen`/`WiederanlaufStufen` intern
`RoomActor.createRoom` statt `join` mit leerem Code auf, unsichtbar für ihre
Unterklassen; auf der Adapter- und API-Ebene bekommen
`GameWebSocketHandlerTest`, `RundenablaufStufen` und `WireProtocolSmokeTest`
denselben Wechsel sichtbar, weil sie das rohe Frame selbst verschicken.
Faltung. `ArchitectureTest` brauchte keine Änderung — `RoomCode` erfüllt die
bestehende Stereotyp-Regel automatisch, ohne neue Ausnahme.

## Bewusste Festlegungen

**Ein Token je Raum im Browser.** Heute steht das Token unter einem festen
Schlüssel in `localStorage`. Wer eine zweite Watchparty besucht, würde damit
sein Token aus der ersten überschreiben und dort beim nächsten Reconnect als
neuer Spieler mit Startguthaben landen — der Punktestand des Abends wäre
verloren. Der Schlüssel trägt deshalb den Code des Raums. Der Name bleibt
global, er ist über Räume hinweg derselbe.

**Codes, die sich vorlesen lassen.** Vier Stellen aus Ziffern und Buchstaben
sind rund 1,2 Millionen Möglichkeiten (33⁴), was gegen versehentliche Kollisionen
reicht. Das eigentliche Problem beim Vorlesen sind verwechselbare Zeichen.
Deshalb: Erzeugt werden nur Codes ohne `O`, `I` und `L`; angenommen wird die
Eingabe trotzdem mit diesen Zeichen und `O` zu `0`, `I` und `L` zu `1`
gefaltet. Wer „oh" hört und `O` tippt, kommt an. Angezeigt wird immer in
Großbuchstaben, weil ein Code in gemischter Schreibweise am Telefon nicht
vorlesbar ist.

**Keine Obergrenze für gleichzeitige Watchpartys.** Die Kapazitätsgrenzen in
`fly.toml` bleiben unangetastet. Sie wirken damit weiter als Grenze, nur nicht
mehr je Raum: 200 Verbindungen hart, geteilt über alle Räume, sind bei etwa
zehn Handys pro Wohnzimmer rund zwanzig Watchpartys, und 512 MB Speicher
teilen sich alle. Wird die Grenze erreicht, scheitert der Verbindungsaufbau
für alle gleichermaßen. Das ist für den erwarteten Gebrauch — Freunde, die den
Link weitergeben — vertretbar und als Beobachtungspunkt in `probelauf.md`
besser aufgehoben als als geratenes Limit im Code.

**Der Code ist die einzige Zugangskontrolle.** Wer ihn kennt oder errät, ist
drin. Kein Kennwort, keine Sperre gegen Durchprobieren. Bei rund 1,2 Millionen
Codes und einer Handvoll lebender Räume trifft ein Rateversuch praktisch nie;
systematisches Durchprobieren über die Leitung wäre möglich, und der Gewinn
daraus wäre, in einem fremden Wohnzimmer mitzuspielen. Host wird ein
Eindringling dabei nicht, denn Host ist, wer den Raum erzeugt hat (ADR-016) —
die schädlichen Kommandos bleiben ihm also verwehrt. Das ist als
Restrisiko angenommen, nicht übersehen.

**Ein stündlicher Sweep statt eines Timers je Watchparty.** Statt für jede
Watchparty einen eigenen Sechs-Stunden-Timer zu führen, prüft ein einzelner,
sich selbst neu einplanender Task stündlich alle Watchpartys gegen ihre
letzte Aktivität. „Nach sechs Stunden" muss dafür nicht sekundengenau sein
— eine abgelaufene Watchparty verschwindet damit spätestens eine Stunde
nach ihrem eigentlichen Ablauf, was für einen unbeobachteten Aufräumvorgang
weit genug reicht. Das hält die Zahl der geplanten Tasks unabhängig von der
Zahl der Watchpartys bei eins statt bei n.

## Offene Fragen

Ob sechs Stunden die richtige Frist zum Aufräumen sind, hängt daran, ob ein
Abend länger dauert als erwartet — dieselbe Unsicherheit, die die Frist in
ADR-023 schon trägt, und dieselbe Antwort: am Spielabend beobachten
(`probelauf.md`).

Ob der Code an der Stelle sichtbar ist, an der man ihn braucht — nämlich
wenn jemand später dazukommt und danach fragt —, ist eine Frage an die
Oberfläche, die sich am Tisch schneller beantwortet als am Schreibtisch.
