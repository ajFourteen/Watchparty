---
name: domaenentyp
description: Checkliste für einen neuen Typ in domain/model — was ArchitectureTest und NullAway bereits erzwingen, und was Urteilssache bleibt.
---

# Domänentyp

Für jeden neuen Typ unter `src/main/java/.../domain/model` (oder
`domain/model/league`). Ursprünglich als vier gleichrangige Punkte gedacht
— zwei davon sind inzwischen harte Checks, kein Skill-Inhalt mehr. Dieser
Skill ist deshalb bewusst schmaler geworden, und das ist der Punkt: Was
sich prüfen lässt, gehört in `check`, nicht in ein Merkblatt.

## Bereits hart geprüft — hier nichts zu tun, nur zu wissen

- **jMolecules-Stereotyp** (`@AggregateRoot`, `@Entity`, `@ValueObject`,
  `@Identity`, `@Service`) — `ArchitectureTest`, Abschnitt „DDD-Bausteine"
  (ADR-025/ADR-027), prüft das bei jedem `archTest`-Lauf. Fehlt er, ist das
  ein roter Test, kein Erinnern.
- **Nullness** (`@Nullable` nur explizit, sonst non-null) — NullAway prüft
  das bei `compileJava`. Ein `Objects.requireNonNull(...)` mit
  Begründungskommentar dort, wo NullAway eine Bedingung nicht selbst
  herleiten kann.

## Bleibt Urteilssache — hier zu prüfen

- **JGiven-Szenario sofort, nicht nachträglich.** Kein neuer Typ ohne
  mindestens ein Szenario, das ihn im selben Commit verwendet. Das ist eine
  Aussage über die *Reihenfolge* der Arbeit, kein Zustand, der sich am
  fertigen Code noch ablesen ließe — deshalb bleibt es Skill-Inhalt, nicht
  Gradle-Task (siehe `docs/prozess-optimierung.md`, Ordnungsprinzip).
- **Gegenprobe gegen `docs/anforderungen.md`.** Ist der Name wirklich ein
  Begriff aus den Anforderungen, oder eine technische Abkürzung, die dort
  eigentlich nachgefragt werden sollte (ADR-025)? Im Zweifel fragen, nicht
  stillschweigend benennen.
- **Passt wirklich keiner der Stereotypen** (wie bei `Bets`, dem
  Wettkatalog)? Dann in `ArchitectureTest` explizit als Ausnahme eintragen
  — sonst schlägt `archTest` ohnehin fehl, aber eine stillschweigend
  wachsende Ausnahmeliste ist trotzdem schlechter als eine begründete.
