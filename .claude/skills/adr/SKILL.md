---
name: adr
description: Legt einen neuen Architecture Decision Record mit der nächsten freien Nummer an, in Kontext-Entscheidung-Konsequenzen-Form, und zieht den Rückverweis nach.
---

# ADR

Für jede technische Entscheidung, die über die aktuelle Codeänderung hinaus
Bestand haben soll — nicht für jede Implementierungsdetail-Wahl, sondern für
die, die jemand in sechs Monaten nachvollziehen will, ohne den Commit zu
lesen.

## Schritte

1. **Nächste Nummer ermitteln:** höchste vorhandene `ADR-0NN` in der Tabelle
   am Anfang von `docs/adrs.md` suchen (nicht hart annehmen — die Zahl
   ändert sich mit jedem neuen ADR). Nummer lückenlos fortsetzen.

2. **Tabellenzeile ergänzen**, oben in der Übersichtstabelle:
   `| ADR-0NN | <Entscheidung, ein Satz> | Akzeptiert |`
   (oder `Vorgeschlagen`, wenn die Entscheidung empfohlen, aber noch nicht
   bestätigt ist — siehe Formatzeile am Dateianfang).

3. **Abschnitt schreiben** in der Form, die die Datei selbst vorgibt:
   Kontext → Entscheidung → Konsequenzen. Kein neues Format erfinden.

4. **Rückverweis nachziehen, wo die Entscheidung im Bau sichtbar wird** —
   in `CLAUDE.md`, wenn sie eine neue Datei, einen neuen Ordner oder eine
   neue Konvention nach sich zieht. `aufbaudoku` prüft nur, dass genannte
   Dateien existieren und Domänentypen erwähnt sind — nicht, dass die
   ADR-Nummer selbst irgendwo zitiert ist. Das bleibt hier Handarbeit.

## Bevor der Status „Akzeptiert" ist

Kommt die Anfrage nicht über einen bereits geklärten Eintrag aus
`docs/offene-entscheidungen.md` (Skill `entscheidung`), sondern direkt als
technische Frage: kurz rückfragen, ob die Entscheidung wirklich schon
getroffen ist, statt sie beim Schreiben des ADRs stillschweigend zur
getroffenen Entscheidung zu machen. Im Zweifel `Vorgeschlagen` statt
`Akzeptiert`, oder zuerst Skill `triage`.
