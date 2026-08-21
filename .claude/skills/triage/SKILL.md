---
name: triage
description: Wenn eine neue Idee, Beobachtung oder Frage auftaucht und noch unklar ist, WAS sie überhaupt ist: ordnet sie genau einem von vier Orten zu — offene Entscheidung, Beobachtungsbogen, ADR oder Feature — und legt sie dort an. Beantwortet „wohin gehört das", nicht „wie zerlege ich es" (dafür: schneiden).
---

# Triage

Der erste Schritt für jede neue Idee, jede Beobachtung, jede technische
Frage, bevor irgendetwas davon Code wird. Die Abgrenzung ist in den
Dokumenten selbst schon scharf definiert — dieser Skill wendet sie nur an,
statt sie bei jeder Gelegenheit neu abzuwägen.

## Die Entscheidung

Beantworte der Reihe nach:

1. **Steht schon fest, wie es ausgehen soll, oder braucht es eine
   menschliche Entscheidung?**
   Wenn eine echte Alternative besteht und niemand sie schon getroffen hat:
   Eintrag in `docs/offene-entscheidungen.md`, im passenden Abschnitt
   (Fachlich/Technisch), mit Spielmodus-Tag (Live-Wetten/Tippspiel/beide) wie
   die bestehenden Einträge. **Hier nicht selbst entscheiden** — das ist der
   ganze Zweck der Datei. Bei Unsicherheit: `AskUserQuestion` statt Annahme.

2. **Ist es nur eine Beobachtung ohne anstehende Entscheidung** — etwas, das
   sich erst am echten Spielabend zeigt (Parameterwerte, Fensterlänge,
   Verhalten von Handys)?
   → `docs/probelauf.md`, Beobachtungsbogen. Nicht nach
   `offene-entscheidungen.md`: Dort steht nur, was eine anstehende
   Entscheidung braucht.

3. **Ist es eine bereits entschiedene technische Frage** (Architektur,
   Stack, Bibliothek, Datenformat)?
   → Skill `adr` aufrufen.

4. **Ist es eine bereits entschiedene fachliche Änderung mit
   Implementierungsbedarf** (neues Verhalten, neue Wette, neue Regel)?
   → Skill `feature` aufrufen — legt das Feature-Dokument an und startet
   den TDD-Ablauf aus Teststrategie §9.1.

Eine Idee kann mehrere Ziele gleichzeitig berühren (z. B. eine Beobachtung,
die zugleich eine offene Frage beantwortet) — dann in dieser Reihenfolge
mehrfach einordnen, nicht nur an der ersten passenden Stelle.

## Nach der Ablage

Kurz bestätigen, wo die Idee gelandet ist und warum — besonders wenn die
Zuordnung nicht offensichtlich war. Das macht die Triage überprüfbar, statt
sich auf ein stilles Urteil zu verlassen.
