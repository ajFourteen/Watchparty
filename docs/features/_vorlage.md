# NNN — Kurzname

## Anlass
Wozu, in zwei Sätzen. Ein Satz davon nennt, was ein Mensch nach diesem
Feature tun kann, was er vorher nicht konnte.

## Betroffene Anforderungen

Pflichttabelle. Genau eine ID je Zeile, keine Bereiche, keine
Kapitelverweise — `Bezug` ist eines von `bestehend`, `geändert`, `neu`,
`zurückgenommen`. Der Task `featuredoku` liest nur diese beiden Spalten;
`Anmerkung` und der Fließtext darunter sind frei.

| ID | Bezug | Anmerkung |
|---|---|---|
| 6-b | bestehend | wofür sie hier gilt |
| 6-g | neu | warum es sie noch nicht gibt |

Alles, was keine Anhang-A-ID ist — Kapitelverweise wie „11 (out of scope)",
Invarianten aus `CLAUDE.md`, Begründungen — gehört als Prosa unter die
Tabelle, nicht hinein.

## Akzeptanzkriterien
Nummeriert. Jedes eine prüfbare Aussage. **Höchstens zwölf** — mehr heißt,
der Schnitt ist zu breit (Skill `schneiden`).

## Szenarien
Angenommen — Wenn — Dann, in Prosa und in der
Sprache der Anforderungen. Werden eins zu eins zu JGiven-Szenarien.

## Kritikalität

**Stufe:** LOW | MEDIUM | HIGH

Genau eine, als eigene Zeile und maschinenlesbar. Darunter die Begründung:
Eintrittswahrscheinlichkeit × Schadensausmaß. Braucht das Feature mehr als
eine Stufe, ist es mehr als ein Feature — dann teilen, nicht die Tabelle
aufmachen (Skill `schneiden`).

## Umgesetzt in
Klassen, die die Einstufung tragen. Bindet die Metrik an den Code.

## Offene Fragen
Wandern nach offene-entscheidungen.md, wenn sie es bleiben.
