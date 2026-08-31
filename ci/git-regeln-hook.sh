#!/usr/bin/env bash
# PreToolUse-Hook fuer Bash: zurrt die beiden Arbeitsregeln dieses Projekts fest.
#
#   1. Es wird ausschliesslich auf main gearbeitet -- keine Feature-Branches.
#   2. Vor jedem Commit wird gepullt.
#
# Beides sind Vorgaben des Projektinhabers (2026-08-20). Sie stehen hier als
# ausfuehrbare Regel und nicht nur als Merksatz, weil eine Regel, an die sich
# jemand erinnern muss, keine Regel ist, sondern eine Absicht.
#
# Regel 2 wird nicht als Ritual geprueft ("lief vorher ein git pull?"), sondern
# an ihrer Wirkung: Ist der lokale Branch hinter seinem Upstream, wird der
# Commit abgelehnt. Das ist faelschungssicher, ueberlebt einen Sitzungswechsel
# und trifft genau den Schaden, den die Regel verhindern soll -- ein Commit auf
# einem veralteten main, der zugleich der Release-Branch ist (ADR-019).
#
# Liest das Hook-JSON auf stdin, antwortet mit einer PreToolUse-Entscheidung.

set -uo pipefail

eingabe="$(cat)"
befehl="$(printf '%s' "$eingabe" | jq -r '.tool_input.command // empty')"

[ -z "$befehl" ] && exit 0

ablehnen() {
    jq -n --arg grund "$1" '{
        hookSpecificOutput: {
            hookEventName: "PreToolUse",
            permissionDecision: "deny",
            permissionDecisionReason: $grund
        }
    }'
    exit 0
}

# --- Regel 1: keine Branches anlegen --------------------------------------
#
# Erfasst werden nur die *erzeugenden* Formen. "git branch" ohne Argument
# listet auf, "git branch -d alt" loescht, "git checkout <branch>" wechselt
# nur -- all das bleibt erlaubt. Angelegt wird, wenn auf "git branch" ein
# Wort folgt, das nicht mit "-" beginnt.
#
# Eine Ausnahme davon (2026-08-30): "git checkout -b lokal origin/xyz" legt
# zwar formal einen lokalen Branch an, erzeugt aber nichts Neues -- der
# Branch existiert bereits auf dem Remote, hier entsteht nur die lokale
# Sicht darauf. Genau das braucht die Dependabot-Routine, um einen fremden
# PR-Branch anzufassen; "gh pr checkout" tut ohnehin dasselbe und faellt gar
# nicht erst unter diese Regel. Die Regel schuetzt vor *eigenen*
# Feature-Branches neben main, nicht davor, Bestehendes anzusehen.
#
# Der Startpunkt muss dafuer wirklich ein Remote sein, nicht bloss ein Wort
# mit Schraegstrich: geprueft wird gegen "git remote". Sonst waere
# "git checkout -b feature/neu irgendwas/altes" ein Schlupfloch.
ist_bestehender_remote_branch() {
    local start remote
    start="$(printf '%s' "$1" | sed -nE 's/.*git +(checkout|switch) +-[bBcC] +[^ ]+ +([^ ;&|]+).*/\2/p')"
    [ -z "$start" ] && return 1
    case "$start" in */*) ;; *) return 1 ;; esac
    remote="${start%%/*}"
    # Mit -C statt cd: Das Arbeitsverzeichnis des Hooks ist nicht zugesichert
    # (Regel 2 unten wechselt es deshalb auch erst selbst).
    git -C "${CLAUDE_PROJECT_DIR:-$PWD}" remote 2>/dev/null | grep -qx "$remote"
}

if echo "$befehl" | grep -Eq '(^|[;&|] *)git +(checkout +-[bB]|switch +-[cC])' \
   && ist_bestehender_remote_branch "$befehl"; then
    exit 0
fi

if echo "$befehl" | grep -Eq '(^|[;&|] *)git +(checkout +-[bB]|switch +-[cC]|worktree +add)'; then
    ablehnen "Regel dieses Projekts: Es wird ausschliesslich auf main gearbeitet, keine Feature-Branches und keine Worktrees. Aenderungen gehen direkt auf main (release.yml und Semantic Release haengen daran). Bitte den Befehl ohne Branch-Anlage wiederholen. Einen bereits BESTEHENDEN Branch auszuchecken ist erlaubt: 'git checkout <branch>' direkt, 'git checkout -b <lokal> origin/<branch>' fuer einen, den es nur auf dem Remote gibt, oder 'gh pr checkout <nr>'."
fi

if echo "$befehl" | grep -Eq '(^|[;&|] *)git +branch +[^-]'; then
    ablehnen "Regel dieses Projekts: Es wird ausschliesslich auf main gearbeitet, keine Feature-Branches. (Auflisten und Loeschen von Branches ist erlaubt -- nur das Anlegen nicht.)"
fi

# --- Regel 2: vor jedem Commit pullen -------------------------------------
if ! echo "$befehl" | grep -Eq '(^|[;&|] *)git +(-[^ ]+ +)*commit'; then
    exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-$PWD}" 2>/dev/null || exit 0
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>/dev/null)"
if [ -z "$upstream" ]; then
    # Kein Upstream (frisches Repo, loser Branch): nichts zu vergleichen.
    exit 0
fi

if ! git fetch --quiet 2>/dev/null; then
    # Offline oder Remote nicht erreichbar. Hier zu blockieren waere
    # obstruktiv -- die Regel schuetzt vor veralteter Historie, nicht vor
    # fehlendem Netz. Deshalb durchlassen, aber sichtbar machen.
    jq -n '{systemMessage: "Hinweis: git fetch fehlgeschlagen (offline?). Der Commit laeuft ungeprueft durch -- Regel \"vor jedem Commit pullen\" konnte nicht verifiziert werden."}'
    exit 0
fi

hinterher="$(git rev-list --count "HEAD..$upstream" 2>/dev/null || echo 0)"
if [ "$hinterher" -gt 0 ]; then
    ablehnen "Regel dieses Projekts: Vor jedem Commit wird gepullt. HEAD ist ${hinterher} Commit(s) hinter ${upstream}. Bitte zuerst 'git pull' ausfuehren und den Commit danach wiederholen."
fi

exit 0
