# 010 — Spieltags-Report per Mail (Schnitt 5)

## Anlass

Der Spieltags-Report existiert seit Feature 006–009 als Seite zum Abruf —
ein Tipper muss aber selbst daran denken, sie zu öffnen. Mit diesem
Feature bekommt ein Tipper, der das für sich bestellt hat, seinen Report
automatisch per Mail, sobald der Spieltag vollständig ausgewertet ist.

Das ist der fünfte und letzte Schnitt der Idee „Spieltags-Report"
(`docs/schnitte/spieltags-report.md`). Auslöser, Empfängerkreis und
Abmeldung sind bereits entschieden (ADR-041, 2026-08-21) — dieser Schnitt
setzt sie um, verhandelt sie nicht neu. Behelf (Skill `schneiden`):
Text-Mail statt gestalteter HTML-Mail, Bestellen/Abbestellen als ein
Schalter am Konto statt je Liga, Versand fire-and-forget wie der
bestehende `MailSender` (kein Retry bei Zustellfehlern).

## Betroffene Anforderungen

| ID | Bezug | Anmerkung |
|---|---|---|
| 13.9-n | neu | Opt-in/Opt-out des Mailversands am Konto |
| 13.9-o | neu | Versand ausgelöst durch den Übergang des letzten offenen Spiels eines Spieltags auf FINAL |
| 13.9-p | neu | jede Mail trägt einen Ein-Klick-Abmeldelink, wirksam ohne Anmeldung |
| 13.9-a | bestehend | der Mailinhalt ist dieselbe Bilanz-Berechnung (`matchdayReport`), unverändert wiederverwendet |
| 13.3-f | bestehend | ein abgesagtes Spiel (CANCELLED) zählt für die Vollständigkeit eines Spieltags wie ein gewertetes |
| 13.2-b | bestehend | ein neu entstehendes Konto trägt keinen aktiven Mailversand-Opt-in |

## Akzeptanzkriterien

1. Ein angemeldeter Tipper kann den Mailversand des Spieltags-Reports für
   sich bestellen (Opt-in).
2. Ein angemeldeter Tipper kann den Mailversand wieder abbestellen
   (Opt-out).
3. Ein neu entstehendes Konto hat den Mailversand nicht bestellt.
4. Ohne Opt-in bekommt ein Tipper auch nach einem vollständig
   ausgewerteten Spieltag keine Report-Mail.
5. Wechselt durch einen Feed-Abgleich das letzte noch offene Spiel eines
   Spieltags auf FINAL, bekommt jeder Tipper mit aktivem Opt-in den
   Report dieses Spieltags per Mail.
6. Wechselt durch einen Handeintrag (Notweg, 13.3-g) das letzte noch
   offene Spiel eines Spieltags auf FINAL, gilt Kriterium 5 ebenso.
7. Ein Spieltag mit einem abgesagten Spiel gilt als vollständig
   ausgewertet, sobald alle übrigen Spiele FINAL sind.
8. Ein bereits vollständig ausgewerteter Spieltag löst bei einem
   erneuten Abgleich keinen zweiten Versand aus.
9. Die versendete Mail zeigt denselben Inhalt wie die eigene Bilanz des
   Tippers für diesen Spieltag (Endergebnis, eigener Tipp und Punkte je
   Spiel, Spieltagssumme).
10. Jede Report-Mail enthält einen individuellen Ein-Klick-Abmeldelink.
11. Ein Aufruf des Abmeldelinks bestellt den Mailversand ab, ohne dass
    eine Anmeldung nötig ist.
12. Ein unbekannter oder bereits verwendeter Abmeldelink-Token quittiert
    denselben Erfolg wie ein gültiger — kein unterscheidbares Ergebnis
    nach außen.

## Szenarien

**Ein Tipper bestellt den Mailversand.**
Angenommen Anna ist angemeldet und hat den Mailversand nicht bestellt —
wenn sie ihn bestellt, dann gilt ihr Opt-in fortan als aktiv.

**Ein Tipper bestellt den Mailversand wieder ab.**
Angenommen Annas Opt-in ist aktiv — wenn sie den Mailversand abbestellt,
dann gilt ihr Opt-in fortan als inaktiv.

**Ohne Opt-in kommt keine Mail.**
Angenommen Ben hat den Mailversand nicht bestellt, sein einziges Spiel des
Spieltags ist getippt — wenn der Feed das Spiel als FINAL meldet und
damit den Spieltag vollständig auswertet, dann bekommt Ben keine
Report-Mail.

**Der Feed-Abgleich löst den Versand aus.**
Angenommen Anna hat den Mailversand bestellt, ihr einziges Spiel des
Spieltags ist getippt und noch offen — wenn der Feed das Spiel als FINAL
meldet, dann bekommt Anna genau eine Report-Mail für diesen Spieltag, mit
demselben Endergebnis, eigenen Tipp, Punkten und derselben Spieltagssumme
wie ihre eigene Bilanz.

**Der Handeintrag löst den Versand ebenso aus.**
Angenommen Anna hat den Mailversand bestellt, ihr einziges Spiel des
Spieltags ist getippt und noch offen — wenn der Betreiber das Ergebnis von
Hand setzt, dann bekommt Anna genau eine Report-Mail für diesen Spieltag.

**Ein abgesagtes Spiel blockiert den Versand nicht.**
Angenommen ein Spieltag mit zwei Spielen, eines davon bereits vom Feed als
abgesagt gemeldet, Anna hat den Mailversand bestellt und für das andere
Spiel getippt — wenn der Feed dieses andere Spiel als FINAL meldet, dann
bekommt Anna eine Report-Mail für den Spieltag.

**Ein bereits ausgewerteter Spieltag versendet kein zweites Mal.**
Angenommen Annas Spieltag ist bereits vollständig FINAL und sie hat dafür
schon eine Report-Mail bekommen — wenn derselbe Spieltag erneut
abgeglichen wird, dann bekommt Anna keine weitere Report-Mail für diesen
Spieltag.

**Der Abmeldelink wirkt ohne Anmeldung.**
Angenommen Anna hat eine Report-Mail mit ihrem individuellen
Abmeldelink-Token bekommen — wenn sie den Abmeldelink aufruft, ohne
angemeldet zu sein, dann gilt ihr Opt-in danach als inaktiv.

**Ein unbekannter Abmeldelink-Token verrät nichts.**
Angenommen ein Abmeldelink-Token, der zu keinem Konto gehört — wenn er
aufgerufen wird, dann quittiert der Server denselben Erfolg wie bei einem
gültigen Token, ohne einen Fehler zu zeigen.

## Kritikalität

**Stufe:** MEDIUM

Ein Fehler versendet bestenfalls eine ungewollte Mail an eine
personenbezogene Adresse (13.8) oder unterschlägt einen bestellten
Versand — beides ärgerlich, aber durch einen einzigen Klick auf den
Abmeldelink bzw. erneutes Bestellen korrigierbar, und kein struktureller
Leak eines fremden Tipps wie bei `PredictionView` (HIGH, Kriterium 19/20).
Die Eintrittswahrscheinlichkeit ist gering: Die einzige neue Entscheidung
im bestehenden Abgleich ist ein einzelner Statuswechsel-Vergleich, der
Mailinhalt selbst ist unverändert wiederverwendete, bereits geprüfte
Logik (13.9-a).

## Umgesetzt in
- `domain/model/league/Account.java`
- `domain/model/league/ReportMailToken.java`
- `application/league/ScheduleSyncService.java`
- `application/league/ReportMailService.java`
- `application/league/port/out/MatchdayCompletionListener.java`
- `application/league/port/in/ReportMailCommands.java`
- `adapter/out/db/AccountRepositoryJdbc.java`
- `adapter/out/mail/SmtpMailSender.java`, `LoggingMailSender.java`
- `adapter/in/http/ReportMailController.java`

## Offene Fragen
Keine. Eine gestaltete HTML-Mail statt der Text-Mail ist bewusster Behelf
(Skill `schneiden`), kein offener fachlicher Punkt.
