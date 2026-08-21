---
name: entscheidung
description: Wenn eine bislang offene Frage aus offene-entscheidungen.md beantwortet ist: zieht die Vier-Dokumente-Kette nach — Eintrag streichen, ADR schreiben, Anforderungen nachziehen, Anhang A ergänzen. Ohne diesen Schritt bleibt die Antwort nur im Gespräch.
---

# Entscheidung

Wird aufgerufen, sobald eine Frage aus `docs/offene-entscheidungen.md`
tatsächlich beantwortet ist — durch den Nutzer, durch den Probelauf, durch
ein Gespräch. Der Sinn dieses Skills ist, dass keine der vier Stellen
vergessen wird: Eine Entscheidung, die nur in einer davon steht, ist eine
zweite Wahrheit in Wartestellung.

## Schritte

1. **Eintrag aus `docs/offene-entscheidungen.md` entfernen.**
   Wird die Frage dauerhaft ausgeschlossen statt beantwortet, wandert sie
   stattdessen nach unten in „Nicht offen — bewusst ausgeschlossen" statt
   ersatzlos zu verschwinden.

2. **ADR schreiben.**
   Für rein technische Entscheidungen: Skill `adr` aufrufen. Für fachliche
   Entscheidungen mit technischer Konsequenz reicht oft ein kurzer ADR-
   Eintrag zusätzlich zu Schritt 3.

3. **`docs/anforderungen.md` nachziehen**, falls sich fachliches Verhalten
   ändert. Das Feature-Dokument (falls eines entsteht, siehe Skill
   `feature`) ist der Antrag, `anforderungen.md` bleibt der geltende Stand
   — beide dürfen nicht auseinanderlaufen.

4. **Atomare Regel in Anhang A ergänzen**, mit Spielmodus-Kennzeichnung und
   der Kategorie (backend/frontend/organisatorisch/beobachtung) wie die
   bestehenden Zeilen. Das ist die Zeile, an der der `abdeckung`-Task
   später eine grün gelaufene Testmethode einfordert.

## Der Punkt, der leicht übersehen wird

Nach Schritt 4 läuft `gradle abdeckung` (Teil von `check`) für diese Regel
**absichtlich rot** — die Regel ist beschlossen und noch nicht im Code
belegt. Das ist kein Fehler, den man vorschnell wegräumt, sondern die
Metrik, die genau das anzeigen soll: entschieden, aber noch nicht gebaut.
Wird die Regel sofort mitimplementiert, gilt stattdessen Skill `feature` ab
Schritt 2 (Feature-Dokument, JGiven-Szenario zuerst).
