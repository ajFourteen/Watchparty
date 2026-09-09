---
name: feature
description: Wenn eine neue Feature-Idee ankommt, bevor geschnitten wird: weitet zuerst den Problemraum (Discover/Define statt sofort Lösungsraum) und klärt dabei auftauchende Fragen größtenteils selbst; nur was wirklich offen bleibt, geht nach ADR, offene-entscheidungen.md oder Beobachtungsbogen. Läuft vor schneiden.
---

# Feature

Jede Idee, die hier ankommt, ist bereits eine Feature-Idee — Fragen werden
direkt geklärt, offene Entscheidungen, Beobachtungsbogen und ADRs sind
Ergebnisse dieser Arbeit, kein eigener Eingangskanal dafür. Dieser Skill
ist der erste Schritt für jede solche Idee, bevor `schneiden` sie in
Scheiben legt: Er weitet zunächst den Problemraum (Discover), bevor er ihn
wieder schließt (Define) — nicht sofort in Schnitte oder Code denken.

## Discover: den Problemraum weiten

Bevor irgendetwas geschnitten wird:

- Wer ist betroffen, welches Bedürfnis steckt hinter der genannten Lösung?
- Welche anderen Rahmungen der Idee gäbe es? Berührt sie eine bestehende
  Anforderung (`docs/anforderungen.md`), eine bestehende Entscheidung
  (`docs/adrs.md`) oder ein verwandtes, bereits gebautes Feature?
- Gibt es dazu schon einen Eintrag in `docs/offene-entscheidungen.md` oder
  `docs/probelauf.md`, den diese Idee berührt oder beantwortet?

## Define: Fragen klären, größtenteils selbst

Beim Weiten des Problemraums tauchen Fragen auf. Beantworte sie zuerst
selbst, aus dem, was schon da ist — `AskUserQuestion` ist die Ausnahme für
die echte Sackgasse, nicht der erste Reflex. Jede Frage, die dabei offen
bleibt, geht in genau eine der folgenden Ablagen (eine Frage kann mehrere
gleichzeitig berühren, dann mehrfach einordnen):

1. **Zieht die Antwort eine Architekturentscheidung nach sich** (Stack,
   Bibliothek, Datenformat, Struktur)? → Skill `adr` aufrufen.

2. **Bleibt die Frage echt offen** — eine Alternative besteht, niemand hat
   sie schon getroffen, und du kommst selbst nicht weiter? → Eintrag in
   `docs/offene-entscheidungen.md`, im passenden Abschnitt
   (Fachlich/Technisch), mit Spielmodus-Tag (Live-Wetten/Tippspiel/beide)
   wie die bestehenden Einträge. **Hier nicht selbst entscheiden** — das
   ist der ganze Zweck der Datei.

3. **Lässt sie sich erst am echten Spielabend beantworten** (Parameterwerte,
   Fensterlänge, Verhalten von Handys)? → `docs/probelauf.md`,
   Beobachtungsbogen.

## Danach

Steht die Idee als verstandenes Problem da — Rahmung geklärt, die meisten
Fragen selbst beantwortet, der Rest an seiner Ablage —, übernimmt Skill
`schneiden`: er zerlegt sie in vertikale Scheiben. Dieser Skill erzeugt
selbst keinen Schnittplan und kein Feature-Dokument; das Bauen selbst
übernimmt danach Skill `implementieren`.

## Nach der Klärung

Kurz bestätigen, welche Fragen wie geklärt wurden und was offen an welche
Ablage ging — besonders wenn die Einordnung nicht offensichtlich war. Das
macht die Klärung überprüfbar, statt sich auf ein stilles Urteil zu
verlassen.
