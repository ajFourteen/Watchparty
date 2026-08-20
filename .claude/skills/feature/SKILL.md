---
name: feature
description: Führt Teststrategie-Abschnitt 9.1 aus — Feature-Dokument anlegen, Szenarien zuerst als rot laufende JGiven-Stufen, erst danach Produktivcode.
---

# Feature

Der erzwungene Ablauf für jedes neue Feature (nicht für den bestehenden,
einmalig nachgerüsteten Funktionsumfang, siehe Teststrategie §9.2). Der
Kern ist der Schritt, den man am leichtesten überspringt: Die Szenarien
laufen rot, bevor eine Zeile Produktivcode existiert — nicht als Ritual,
sondern weil ein Test, der nie rot war, nichts über die Implementierung
beweist, die ihn grün gemacht hat.

## Schritte

1. **Feature-Dokument prüfen/anlegen.**
   Existiert `docs/features/NNN-kurzname.md` für diese Änderung schon?
   Wenn nicht: aus `docs/features/_vorlage.md` anlegen, nächste freie
   dreistellige Nummer (höchste vorhandene `NNN` in `docs/features/`
   ermitteln, nicht raten).

2. **Alle Abschnitte der Vorlage ausfüllen** — bis auf „Umgesetzt in",
   das erst nach Schritt 5 einen Wert bekommt:
   - **Anlass** — zwei Sätze, wozu.
   - **Betroffene Anforderungen** — Nummern aus `docs/anforderungen.md`,
     oder dort als neu ergänzen.
   - **Akzeptanzkriterien** — nummeriert, jedes eine prüfbare Aussage.
   - **Szenarien** — Angenommen–Wenn–Dann, in der Sprache der
     Anforderungen (Wette, Wettfenster, Runde, Tipp, Einsatz, Anteil,
     Pool, Strafe, Auflösen — nicht „Markt", ADR-022).
   - **Kritikalität** — Eintrittswahrscheinlichkeit × Schadensausmaß,
     begründet, Ergebnis LOW/MEDIUM/HIGH. Bestimmt später den
     pitest-Schwellwert für die tragenden Klassen.
   - **Offene Fragen** — was hier noch offen bleibt, geht nach
     `docs/offene-entscheidungen.md` (Skill `triage`), nicht
     stillschweigend in eine Annahme.

3. **Szenarien 1:1 in JGiven-Stufenklassen übersetzen**, bevor
   Produktivcode entsteht. Neue Domänentypen dabei sofort mit Stereotyp
   anlegen (Skill `domaenentyp`).

4. **Laufen lassen und prüfen, dass sie rot sind.** Eine fehlende Klasse
   oder Methode ist ein gültiges Rot — kein Kompilierfehler, den man vorab
   wegräumt, sondern der Beleg, dass der Test tatsächlich etwas prüft.

5. **Produktivcode schreiben**, bis die Szenarien grün laufen. Onion-Ringe
   einhalten (`domain` → `application` → `adapter`/`config`, Abhängigkeiten
   nur nach innen, ADR-024).

6. **„Umgesetzt in" nachtragen** — die Klassen, die die
   Kritikalitätseinstufung tragen.

7. **Nach der Umsetzung: Feature-Dokument nicht weiterpflegen.** Lebendes
   Dokument ist ab jetzt der JGiven-Report, nicht die Datei unter
   `docs/features/`. Sie bleibt als Beleg der Kritikalitätsbewertung
   stehen.

Für gestufte lokale Rückkopplung während Schritt 3–5: Skill `pruefen`. Vor
dem ersten Commit der Änderung: Skill `invarianten-review` und `freigabe`.
