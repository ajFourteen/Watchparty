---
name: freigabe
description: Wenn der Commit-Typ festgelegt wird, vor jedem Commit: macht sichtbar, was feat/fix/perf auslösen — Release und Deploy auf Fly.io — und ob der Typ zur Änderung passt. Eine Deployment-Entscheidung, keine Beschriftung.
---

# Freigabe

Unmittelbar vor `git commit`. `ci/commit-format-pruefen.sh` prüft, ob der
Typ gültiges Conventional-Commits-Format hat — nicht, ob er zur
tatsächlichen Änderung passt. Diese Lücke bewusst nicht automatisiert
(siehe Prozess-Audit in `docs/prozess-optimierung.md`: zu störanfällig,
zu viele legitime Ausnahmen) — deshalb hier als Skill statt als Gate.

## Was jeder Typ auslöst (ADR-019, Semantic Release)

- **`feat:` / `fix:` / `perf:`** → Semantic Release erstellt einen Release
  **und** `release.yml` deployt automatisch. Die Änderung geht innerhalb
  weniger Minuten live auf der produktiven Instanz.
- **`docs:` / `chore:` / `style:` / `test:` / `refactor:` / `build:` /
  `ci:`** → kein Release, kein Deploy. Die Änderung bleibt auf `main`
  liegen, bis der nächste `feat:`/`fix:`-Commit sie mitzieht.
- **`!` nach dem Typ oder `BREAKING CHANGE:` im Rumpf** → Major-Version-
  Sprung zusätzlich zum Release.

## Vor dem Commit

Den gewählten Typ und seine Wirkung einmal explizit benennen:
„Das hier ist `<typ>:` — das bedeutet <deployt sofort / bleibt auf main
liegen>." Zwei Fehlrichtungen, die das abfängt:

- Eine reine Dokumentationsänderung als `fix:` — unnötiger Release/Deploy
  für nichts, das sich am Verhalten geändert hat.
- Eine Verhaltensänderung als `chore:` oder `refactor:` — bleibt fälschlich
  unveröffentlicht, obwohl sie live sein sollte.

Passt der Typ nicht zur Änderung: Commit aufteilen oder Typ korrigieren,
nicht den falschen Typ stehen lassen, „weil er ja durch die
Formatprüfung kommt".
