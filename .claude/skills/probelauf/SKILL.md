---
name: probelauf
description: Nach einem echten Spielabend: arbeitet den Beobachtungsbogen aus docs/probelauf.md ab und ordnet jede Beobachtung ihrem Ziel zu — offene Entscheidung, Anforderung oder Feature.
---

# Probelauf

Nach einem echten Spielabend, wenn `docs/probelauf.md` neue Beobachtungen
trägt. Im Kern ein wiederholter Aufruf von Skill `triage` über eine ganze
Liste statt über eine einzelne Idee — mit der Besonderheit, dass die Quelle
danach aufgeräumt werden muss, nicht nur die Ziele befüllt.

## Schritte

1. `docs/probelauf.md` lesen, jede notierte Beobachtung einzeln
   durchgehen.

2. Für jede Beobachtung:
   - **Beantwortet sie eine bereits offene Frage** aus
     `docs/offene-entscheidungen.md` (z. B. die Kalibrierung der drei
     Parameter aus 3.1, die Fensterlänge)? → dort eintragen, dann Skill
     `entscheidung`, falls sich die Frage damit tatsächlich klären lässt.
   - **Deckt sie eine neue technische Notwendigkeit auf**, die vorher
     niemand entschieden hat? → Skill `adr`.
   - **Verlangt sie eine Verhaltensänderung**? → Skill `feature`.
   - **Ist sie eine Randnotiz ohne Konsequenz** (z. B. "lief unauffällig")?
     → im Beobachtungsbogen als erledigt markieren, mit kurzer Begründung
     — nicht kommentarlos löschen. Nachvollziehbarkeit für den nächsten
     Probelauf ist der Zweck des Bogens.

3. Am Ende `docs/probelauf.md` so aktualisieren, dass nur noch offene,
   unbeantwortete Beobachtungen stehen bleiben — alles andere ist entweder
   erledigt-markiert oder an eine der vier Zieldateien weitergereicht.

## Was dieser Skill nicht tut

Er entscheidet nichts selbst. Wo eine Beobachtung eine echte Alternative
aufwirft (Parameterwert X oder Y?), landet sie in
`docs/offene-entscheidungen.md` und wartet dort auf eine menschliche
Antwort — genau wie bei `triage`.
