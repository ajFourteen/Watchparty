# Datenschutzerklärung

**Entwurf, Stand 2026-08-18.** Kein Ersatz für eine rechtliche Prüfung —
vor der Veröffentlichung durch eine sachkundige Person gegenlesen lassen,
insbesondere die Punkte, die unten ausdrücklich als offen markiert sind
(Auftragsverarbeitungsverträge, Drittlandtransfer). Dieser Text ist Teil
von Stufe 8 aus `docs/features/005-tippspiel-liga.md` und noch nicht in
die Anwendung eingebunden.

---

## Verantwortlicher

FOURTEEN IT UG (haftungsbeschränkt), vertreten durch Andreas Jürgensen
Salomon-Petri-Ring 27, 22117 Hamburg
E-Mail: info@fourteen-it.de

Weitere Angaben (Registereintrag, USt-ID) siehe
[Impressum](impressum.md).

## Um welche Anwendung es geht

Diese Erklärung gilt für die gesamte Anwendung. Sie hat zwei getrennte
Spielmodi mit sehr unterschiedlichem Umgang mit personenbezogenen Daten
(`CLAUDE.md`, „Zwei Spielmodi"):

- **Live-Wetten** (ein Spielabend): Beitritt nur mit einem frei gewählten
  Namen, ohne Konto, ohne E-Mail-Adresse. Der Name wird nicht überprüft und
  keiner realen Identität zugeordnet. Der Raumzustand liegt ausschließlich
  im Arbeitsspeicher des Servers, gesichert nur als Abzug für einen
  möglichen Neustart innerhalb desselben Abends und mit eingebauter
  Verfallszeit — danach ist er weg. Ein Wiedererkennungs-Token liegt lokal
  im Browser des jeweiligen Geräts.
- **Tippspiel-Liga** (eine Saison): Verlangt ein Konto mit
  E-Mail-Adresse und Anzeigename. Diese Erklärung betrifft in der Sache vor
  allem diesen Modus — hier entstehen personenbezogene Daten, die über
  einen Abend hinaus gespeichert werden.

## Welche Daten wir verarbeiten, wozu, und auf welcher Grundlage

### Konto und Anmeldung (Tippspiel)

Bei der Anmeldung erheben wir E-Mail-Adresse und einen selbst gewählten
Anzeigenamen. Damit versenden wir einen Anmeldelink („Magic Link"); ein
Kennwort gibt es nicht. Existiert zur Adresse noch kein Konto, entsteht es
beim ersten erfolgreichen Einlösen des Links.

- **Zweck:** Wiedererkennung über eine ganze Saison hinweg (ein Konto pro
  Person, damit Ergebnistipps und Ligazugehörigkeit zugeordnet werden
  können).
- **Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Erfüllung eines
  Vertrags bzw. vorvertragliche Maßnahme auf eigene Anfrage der
  betroffenen Person — die Anmeldung ist eine aktive, freiwillige
  Handlung).
- **Speicherdauer:** bis zur Löschung des Kontos durch die betroffene
  Person selbst (siehe unten).
- Der Anmeldelink ist einmal verwendbar und verfällt nach 15 Minuten. Die
  Antwort auf eine Anmeldeanfrage ist immer dieselbe, unabhängig davon, ob
  zur Adresse schon ein Konto besteht — daraus lässt sich also nicht
  ablesen, wer bereits mitspielt.

### Sitzung

Nach dem Einlösen des Anmeldelinks setzen wir ein Sitzungscookie. Es hält
90 Tage, damit man sich nicht innerhalb einer Saison wiederholt neu
anmelden muss. Das Cookie ist technisch notwendig für die Anmeldefunktion
selbst; dafür ist keine gesonderte Einwilligung nötig (§ 25 Abs. 2 Nr. 2
TDDDG).

### Anmeldeversuche und IP-Adresse (Rate Limit)

Anmeldeanfragen sind je E-Mail-Adresse und je absendender IP-Adresse in
ihrer Häufigkeit begrenzt, damit niemand die Funktion missbrauchen kann
(z. B. um fremde Postfächer mit Mails zu fluten). Die dafür nötigen Daten
(IP-Adresse, Zeitpunkt) liegen ausschließlich im Arbeitsspeicher des
Servers, werden nicht in die Datenbank geschrieben und fallen automatisch
aus einem gleitenden Zeitfenster von 15 Minuten heraus — spätestens bei
einem Neustart des Servers sind sie vollständig weg.

- **Rechtsgrundlage:** Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse
  an einem funktionsfähigen, missbrauchsfreien Dienst).

### Ergebnistipps und Ligen

Ergebnistipps und Ligamitgliedschaften sind an das Konto gebunden und
werden für die Dauer der Saison bzw. bis zur Löschung des Kontos
gespeichert. In Ranglisten ist ausschließlich der Anzeigename sichtbar,
niemals die E-Mail-Adresse — auch nicht gegenüber anderen Mitgliedern
derselben Liga oder dem Verwalter einer Liga.

- **Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO, wie bei der Anmeldung —
  das Tippen ist der eigentliche Zweck der Anwendung.

## Weitergabe an Dritte

Wir setzen folgende Auftragsverarbeiter ein:

- **Versand der Anmeldelinks und Betriebs-Alarme:** IONOS (SMTP).
- **Hosting:** Fly.io, Region Frankfurt (`fra`).

**Offen:** Für beide sind Auftragsverarbeitungsverträge (Art. 28 DSGVO)
zu schließen bzw. zu prüfen, ob sie bereits vorliegen. Bei Fly.io als
US-amerikanischem Unternehmen ist zusätzlich zu prüfen, ob trotz der
EU-Serverregion eine Übermittlung in ein Drittland stattfindet (z. B. über
Support-Zugriff oder Metadaten) und welche Garantie dafür greift (etwa das
EU-US Data Privacy Framework) — das lässt sich ohne Blick in den
aktuellen Fly.io-Auftragsverarbeitungsvertrag nicht abschließend
beantworten.

Eine automatische Ergebnis-Erkennung ruft Daten von ESPN ab (öffentliche
Spielpläne und Ergebnisse, keine personenbezogenen Daten der Nutzer werden
dorthin übertragen).

## Löschung

Ein Konto kann von der betroffenen Person jederzeit selbst gelöscht
werden. Dabei werden E-Mail-Adresse und Anzeigename entfernt, und alle
Ergebnistipps verschwinden aus sämtlichen Ranglisten — es bleibt kein
abrufbarer Rest.

## Rechte der betroffenen Person

Jede betroffene Person hat das Recht auf:

- Auskunft über die zu ihr gespeicherten Daten (Art. 15 DSGVO),
- Berichtigung unrichtiger Daten (Art. 16 DSGVO),
- Löschung (Art. 17 DSGVO) — in dieser Anwendung direkt über die
  Selbstlöschfunktion des Kontos möglich, alternativ per Anfrage an die
  oben genannte Kontaktadresse,
- Einschränkung der Verarbeitung (Art. 18 DSGVO),
- Datenübertragbarkeit (Art. 20 DSGVO),
- Widerspruch gegen eine Verarbeitung auf Grundlage berechtigten
  Interesses (Art. 21 DSGVO),
- Beschwerde bei einer Datenschutzaufsichtsbehörde (Art. 77 DSGVO).

## Automatisierte Entscheidungsfindung

Findet nicht statt. Wertungspunkte und Ranglisten werden nach einer
festen, offengelegten Regel berechnet (Kapitel 13.5 der Anforderungen),
nicht durch ein lernendes oder profilbildendes System.

## Änderungen dieser Erklärung

Diese Erklärung kann angepasst werden, wenn sich die Verarbeitung ändert
(z. B. neue Auftragsverarbeiter). Es gilt jeweils die zuletzt
veröffentlichte Fassung.
