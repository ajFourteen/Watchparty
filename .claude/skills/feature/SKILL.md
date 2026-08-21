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

0. **Schnitt prüfen, bevor irgendetwas entsteht.**
   Ist das hier *ein* Feature — eine Fähigkeit, die nach Fertigstellung
   jemand benutzen kann, mit genau einer Kritikalität und höchstens zwölf
   Akzeptanzkriterien? Wenn nein oder unklar: erst Skill `schneiden`, dann
   hierher zurück, und zwar nur mit dem **ersten** Schnitt. Das ist der
   Schritt, dessen Fehlen aus Feature 005 ein Dokument mit 38 Kriterien
   und neun Baustufen gemacht hat.

1. **Feature-Dokument prüfen/anlegen.**
   Existiert `docs/features/NNN-kurzname.md` für diese Änderung schon?
   Wenn nicht: aus `docs/features/_vorlage.md` anlegen, nächste freie
   dreistellige Nummer (höchste vorhandene `NNN` in `docs/features/`
   ermitteln, nicht raten).

2. **Alle Abschnitte der Vorlage ausfüllen** — bis auf „Umgesetzt in",
   das erst nach Schritt 5 einen Wert bekommt:
   - **Anlass** — zwei Sätze, wozu.
   - **Betroffene Anforderungen** — die Pflichttabelle `| ID | Bezug |
     Anmerkung |`, eine Anhang-A-ID je Zeile, `Bezug` aus `bestehend`,
     `geändert`, `neu`, `zurückgenommen`. Keine Bereiche („13.1-a bis
     13.8-x"), keine Kapitelverweise („11 (out of scope)"), keine
     Invarianten-Nummern — die gehören als Prosa unter die Tabelle.
     Neue IDs werden in `docs/anforderungen.md` ergänzt, sonst bleibt
     `abdeckung` blind.
   - **Akzeptanzkriterien** — nummeriert, jedes eine prüfbare Aussage,
     höchstens zwölf.
   - **Szenarien** — Angenommen–Wenn–Dann, in der Sprache der
     Anforderungen (Wette, Wettfenster, Runde, Tipp, Einsatz, Anteil,
     Pool, Strafe, Auflösen — nicht „Markt", ADR-022).
   - **Kritikalität** — genau eine Stufe, als eigene maschinenlesbare
     Zeile `**Stufe:** LOW|MEDIUM|HIGH`, darunter die Begründung
     (Eintrittswahrscheinlichkeit × Schadensausmaß). Bestimmt später den
     pitest-Schwellwert für die tragenden Klassen. Wer hier eine Tabelle
     mit mehreren Bereichen braucht, hat den Schnitt aus Schritt 0
     übersprungen.
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

Der Task `featuredoku` prüft die Form beim Bau nach: sieben
Pflichtabschnitte, genau eine Stufe, die ID-Tabelle gegen Anhang A,
höchstens zwölf Kriterien, keine Bautabelle. Er fängt den übersprungenen
Schritt 0 ab, ersetzt ihn aber nicht — ein horizontaler Schnitt kann alle
fünf Prüfungen bestehen.

Für gestufte lokale Rückkopplung während Schritt 3–5: Skill `pruefen`. Vor
dem ersten Commit der Änderung: Skill `invarianten-review` und `freigabe`.
