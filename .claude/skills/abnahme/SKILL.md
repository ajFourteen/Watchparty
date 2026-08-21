---
name: abnahme
description: Wenn ein Feature-Dokument existiert und die Implementierung fertig aussieht, vor `freigabe` und vor dem Commit — also bevor ein Feature als fertig gilt: prüft jedes Akzeptanzkriterium einzeln gegen das tatsächlich beobachtete Verhalten, nicht gegen das eigene JGiven-Szenario, das dieselbe Interpretation nur wiederholt.
---

# Abnahme

Zwischen `invarianten-review` und `freigabe`. Ein grünes JGiven-Szenario
beweist, dass Implementierung und Szenario zueinander passen — beide sind
im selben Zug vom selben Bearbeiter entstanden (Skill `feature`, Schritt 3
und 5). Es beweist nicht, dass diese gemeinsame Interpretation mit dem
Akzeptanzkriterium übereinstimmt, das noch vor der ersten Zeile Testcode
in Prosa formuliert wurde. Diese Lücke schließt kein Gate: `check` fragt
„sind die Tests grün", nicht „haben die Tests das Richtige geprüft".

## Vorbedingung

Ein Feature-Dokument mit ausgefülltem Abschnitt „Akzeptanzkriterien"
liegt vor. Ohne das gehört hierher nichts — zurück zu `feature`.

## Vorgehen

Für jedes Kriterium einzeln, in der nummerierten Reihenfolge des
Dokuments:

1. **Kriterium wörtlich lesen**, losgelöst vom eigenen Szenario-Text
   darunter. Die Szenarien sind eine Übersetzung — bei der Abnahme zählt
   das Original.
2. **Erwartetes Verhalten unabhängig neu formulieren**: Was müsste ein
   Mensch beobachten, der nur das Kriterium kennt, nicht die
   Implementierung? Eine Verneinung im Kriterium („keine Fehlermeldung",
   „bleibt unverändert") dabei nicht überspringen — das sind die Fälle,
   die ein Szenario am leichtesten stillschweigend ausspart, weil dafür
   kein positiver Assert naheliegt.
3. **Tatsächlich beobachten, nicht aus dem Code schließen.** Wo möglich
   ausführen: Backend über den laufenden Server (`curl`,
   WebSocket-Client) statt nur den JGiven-Assert zu lesen; Frontend über
   `npm run dev` im Browser, wie CLAUDE.md für UI-Änderungen ohnehin
   verlangt — hier jetzt gezielt am Kriterium entlang, nicht nur am
   Gesamteindruck „sieht richtig aus". Lässt sich in dieser Umgebung
   nichts ausführen (kein Browser, kein Endgerät): das offen benennen,
   nicht stillschweigend als geprüft verbuchen — wie es die
   Feature-Dokumente selbst schon tun (z. B. 005: „nicht in einem echten
   Browser").
4. **Abgleichen und mit Ja/Nein/Unsicher markieren.** Bei „Nein" oder
   „Unsicher": nicht zum nächsten Kriterium weitergehen, sondern zurück zu
   Schritt 5 in `feature` (Produktivcode nachbessern) — oder, falls das
   Kriterium selbst unklar oder falsch war, zu `triage`.

Erst wenn alle Kriterien mit „Ja" markiert sind, gilt das Feature als
fertig implementiert und `freigabe` ist an der Reihe. Das Ergebnis kurz im
Gespräch festhalten (Kriterium → Ja/Nein/Unsicher mit einem Satz
Begründung), nicht in einer eigenen Datei — das Feature-Dokument selbst
bleibt nach `feature` Schritt 7 unverändert stehen.

## Abgrenzung

- **Nicht** die sieben harten Invarianten aus CLAUDE.md — dafür
  `invarianten-review`, eine andere Quelle und ein anderer Blickwinkel
  (Systemregeln statt Feature-Kriterien).
- **Nicht** automatisierbar als Gate: Ein Akzeptanzkriterium ist Prosa,
  kein Regex-Ziel — derselbe Grund, aus dem „Commit-Typ passt zum Diff" in
  `docs/prozess-optimierung.md` bewusst kein Gate wurde, sondern der Skill
  `freigabe`.
- **Nicht** noch einmal `pruefen`: der prüft, ob der Stand technisch trägt
  (kompiliert, testet grün, Architektur eingehalten), nicht ob er das
  Richtige tut.
