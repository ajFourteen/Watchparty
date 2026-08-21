# 011 — Kurzanleitung im Tippspiel

## Anlass

Bei den Live-Wetten kann sich jeder am Tisch jederzeit über einen Knopf
selbst erklären lassen, wie Wettfenster, Pool und Punkte funktionieren
(`Guide.jsx`), ohne dass der Host etwas vorträgt. Im Tippspiel fehlt dieses
Gegenstück: Wer sich zum ersten Mal anmeldet, sieht Spielplan, Tippformular
und Ligen ohne eine einzige erklärende Zeile dazu, wie Wertung,
Sichtbarkeit fremder Tipps oder die Gleichstandsregel der Rangliste
funktionieren.

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 13.10-a | neu | Zugriffspunkt und Inhalt der Kurzanleitung |
| 13.10-b | neu | Erstbesuch öffnet sie automatisch, danach nur auf Wunsch |

Der Inhalt der Kurzanleitung fasst ausschließlich bereits geltende Regeln in
Prosa zusammen — 13.2 (Anmeldung), 13.4 (Tippen/Abgabeschluss), 13.5
(Wertung), 13.6 (Ligen/Rangliste) und 13.9 (Spieltags-Report). Keine dieser
Regeln ändert sich durch dieses Feature; sie stehen deshalb nicht als
eigene Zeile in der Tabelle (nur neue, geänderte oder zurückgenommene
Anhang-A-IDs gehören dorthin), sondern nur als Referenz hier im Fließtext.

## Akzeptanzkriterien

1. Ein angemeldeter Tipper kann sich die Kurzanleitung über einen Knopf im
   Kopfbereich des Tippspiels anzeigen lassen, unabhängig davon, welchen
   Tab (Spieltag/Ligen) er gerade offen hat.
2. Er kann die Kurzanleitung wieder schließen und landet danach wieder dort,
   wo er war.
3. Bei der ersten Anmeldung eines Geräts geht die Kurzanleitung von selbst
   auf; bei jeder weiteren Anmeldung auf demselben Gerät bleibt sie zu, bis
   er sie selbst öffnet.
4. Die Kurzanleitung erklärt die Anmeldung per Magic Link (E-Mail-Adresse
   und Anzeigename statt Kennwort, 13.2).
5. Sie erklärt, dass ein Ergebnistipp bis zum Anstoß des jeweiligen Spiels
   änderbar ist und danach weder änderbar noch nachtragbar (13.4).
6. Sie erklärt, dass fremde Ergebnistipps zu einem Spiel erst ab dessen
   Anstoß sichtbar werden, der eigene dagegen immer (13.4).
7. Sie erklärt die Wertung nach der höchsten erreichten Stufe — exaktes
   Ergebnis, richtige Tendenz und richtiger Abstands-Eimer, nur richtige
   Tendenz — und dass eine falsche Tendenz immer 0 Wertungspunkte bringt,
   unabhängig vom Abstand (13.5).
8. Sie erklärt, dass ein Tipper eine Liga anlegen und eine bestehende Liga
   über einen weitergegebenen Beitrittscode betreten kann (13.6).
9. Sie erklärt die Gleichstandsregel der Rangliste: erst Zahl der exakten
   Ergebnisse, dann Zahl der richtigen Tendenzen, bei weiterhin gleichem
   Stand ein geteilter Platz (13.6).
10. Sie erklärt, dass sich der Spieltags-Report per Mail im Konto-Menü
    bestellen und wieder abbestellen lässt (13.9).

## Szenarien

Reine Oberflächen-Komponente ohne Serverzustand — kein neues Feld an
`AuthenticatedAccount`, `PredictionView` oder einem anderen Backend-Typ, kein
neuer Endpunkt. Nach `teststrategie.md` §11 liegt das vollständig außerhalb
der JGiven-Teststrategie (analog zu Kriterium 6–16 in Feature 002); alle
zehn Kriterien werden von Hand nachvollzogen, durch Durchspielen im
Browser:

**Erstbesuch.**
Angenommen ein Gerät hat sich noch nie im Tippspiel angemeldet.
Wenn die Anmeldung per Magic Link gelingt.
Dann öffnet sich die Kurzanleitung von selbst, ohne dass jemand den Knopf
dafür antippt.

**Wiederkehrender Besuch.**
Angenommen dasselbe Gerät hat die Kurzanleitung schon einmal gesehen.
Wenn sich der Tipper erneut anmeldet oder die Seite neu lädt.
Dann bleibt die Kurzanleitung zu, bis er selbst auf den Knopf tippt.

**Zugriff von jedem Tab aus.**
Angenommen ein Tipper ist angemeldet und hat den Ligen-Tab offen.
Wenn er auf den Anleitungs-Knopf tippt.
Dann öffnet sich die Kurzanleitung, und nach dem Schließen steht wieder der
Ligen-Tab da, nicht der Spieltag.

**Inhalt deckt alle fünf Themenbereiche.**
Angenommen die Kurzanleitung ist offen.
Dann stehen darin, in Prosa und ohne dass etwas aus dem Ablauf
weggelassen wird: Anmeldung per Magic Link, Abgabeschluss je Spiel,
verdeckte fremde Tipps vor Anstoß, die Wertung nach höchster Stufe, das
Anlegen einer Liga und der Beitritt per Code, die Gleichstandsregel und der
Mail-Opt-in des Spieltags-Reports.

## Kritikalität

**Stufe:** LOW

Reine Darstellung bereits geltender Regeln: kein Einfluss auf
Wertungspunkte, Kontostand, Rangliste oder Mailversand — die Komponente
liest nur bereits vorhandene, unveränderte Serverdaten (Konto,
angemeldeter Zustand) und zeigt ansonsten fest hinterlegten Text. Ein
Fehler (eine veraltete oder falsche Erklärung) fällt spätestens beim
nächsten Tippen oder bei der nächsten Rangliste sofort auf und wirkt sich
auf keinen gespeicherten Zustand aus. Dieselbe Einstufung wie der
Wettkatalog `Bets` bei den Live-Wetten (`teststrategie.md`, Abschnitt 6.4)
und der überwiegende Teil von Feature 002.

## Umgesetzt in

`frontend/src/league/LeagueGuide.jsx` (neu), `frontend/src/league/League.jsx`
(Zugriffspunkt im Kopfbereich, Erstbesuch-Automatik über
`watchparty.league.guideSeen` in `localStorage`, analog zu `GUIDE_SEEN_KEY`
in `Watchparty.jsx`).

## Offene Fragen

Keine — der Zugriffspunkt (Knopf im Kopfbereich, Erstbesuch-Automatik über
`localStorage`) folgt unmittelbar dem Vorbild `Guide.jsx`, ohne echte
Alternative, die eine Entscheidung nach `offene-entscheidungen.md`
bräuchte.
