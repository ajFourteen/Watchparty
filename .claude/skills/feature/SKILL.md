---
name: feature
description: Wenn der Schnitt steht und gebaut werden soll: führt Teststrategie 9.1 aus — Feature-Dokument anlegen, Szenarien zuerst als rot laufende JGiven-Stufen, erst danach Produktivcode. Der Kern ist Rot vor Grün; das prüft sonst nichts nach.
---

# Feature

Der erzwungene Ablauf für jedes neue Feature (nicht für den bestehenden,
einmalig nachgerüsteten Funktionsumfang, siehe Teststrategie §9.2). Der
Kern ist der Schritt, den man am leichtesten überspringt: Die Szenarien
laufen rot, bevor eine Zeile Produktivcode existiert — nicht als Ritual,
sondern weil ein Test, der nie rot war, nichts über die Implementierung
beweist, die ihn grün gemacht hat.

## Schritte

**Vorbedingung:** Der Schnitt steht — ein Feature, das nach Fertigstellung
jemand benutzen kann. Wenn nicht oder unklar: erst Skill `schneiden`, dann
hierher zurück.

Ohne im Gespräch genannten Schnitt (typischerweise der Fall, wenn dieser
Skill in einer neuen Sitzung ohne Vorlauf aufgerufen wird): unter
`docs/schnitte/` nach einer Datei suchen, deren Thema zur Anfrage passt,
und darin die oberste Zeile mit Status `offen` nehmen — nicht `blockiert`
oder `fertig`. Gibt es keine solche Zeile mehr oder keinen Schnittplan zu
dem Thema, hier stoppen und nachfragen, statt zu raten, welcher Schnitt
gemeint ist.

1. **Feature-Dokument prüfen/anlegen.**
   Existiert `docs/features/NNN-kurzname.md` für diese Änderung schon?
   Wenn nicht: aus `docs/features/_vorlage.md` anlegen, nächste freie
   dreistellige Nummer (höchste vorhandene `NNN` in `docs/features/`
   ermitteln, nicht raten).

2. **Abschnitte ausfüllen.** Die verbindliche Form steht in
   `docs/features/_vorlage.md` und wird hier nicht wiederholt — eine
   zweite Kopie wäre die zweite Wahrheit, und `featuredoku` prüft die Form
   ohnehin beim Bau. „Umgesetzt in" bleibt bis Schritt 6 leer.

   Zwei Dinge, die weder Vorlage noch Task abnehmen können, weil sie
   Urteil verlangen:
   - **Sprache der Anforderungen** in den Szenarien: Wette, Wettfenster,
     Runde, Tipp, Einsatz, Anteil, Pool, Strafe, Auflösen — nicht „Markt"
     (ADR-022). Ein Fachbegriff ohne eigenen Domänentyp ist ein Anlass
     nachzufragen (ADR-025).
   - **Offene Fragen** gehen nach `docs/offene-entscheidungen.md`
     (Skill `triage`), nicht stillschweigend in eine Annahme.

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
   Kritikalitätseinstufung tragen. Gibt es zu diesem Feature einen
   Schnittplan unter `docs/schnitte/`: dort die Zeile dieses Schnitts auf
   `fertig` setzen und den Pfad des gerade angelegten Feature-Dokuments in
   die letzte Spalte eintragen — das ist der einzige Schreibzugriff, den
   `feature` auf den Schnittplan hat.

7. **Nach der Umsetzung: Feature-Dokument nicht weiterpflegen.** Lebendes
   Dokument ist ab jetzt der JGiven-Report, nicht die Datei unter
   `docs/features/`. Sie bleibt als Beleg der Kritikalitätsbewertung
   stehen.

Der Kern sind die Schritte 3 und 4. Schritt 2 ist heute Form, die
`featuredoku` prüft (sieben Pflichtabschnitte, genau eine Stufe,
ID-Tabelle gegen Anhang A, höchstens zwölf Kriterien, keine Bautabelle);
Schritt 5 hält `ArchitectureTest` nach. **Rot vor Grün hält niemand nach
außer diesem Ablauf** — es gibt keinen Task, der einem grünen Test ansieht,
ob er je rot war. Und `featuredoku` fängt zwar den übersprungenen Schnitt
ab, ersetzt ihn aber nicht: ein horizontaler Schnitt besteht alle fünf
Prüfungen.

Für gestufte lokale Rückkopplung während Schritt 3–5: Skill `pruefen`. Vor
dem ersten Commit der Änderung: Skill `invarianten-review` und `freigabe`.
