import { useState } from "react";
import { useLeagueAccount } from "./useLeagueAccount.js";
import { LoginScreen } from "./LoginScreen.jsx";
import { MatchdayScreen } from "./MatchdayScreen.jsx";
import { LeaguesScreen } from "./LeaguesScreen.jsx";
import { LeagueDetailScreen } from "./LeagueDetailScreen.jsx";

/** Das Tippspiel: Anmeldung, Spieltag, Ligen — unabhängig von den Live-Wetten (CLAUDE.md, "Beide Modi teilen sich die Anwendung und sonst nichts"). */
export function League() {
  const { status, account, error, requestLink, logout, deleteAccount } = useLeagueAccount();
  const [tab, setTab] = useState("schedule");
  const [openLeagueId, setOpenLeagueId] = useState(null);

  if (status === "loading") {
    return (
      <main className="shell">
        <p className="hint">Lädt …</p>
      </main>
    );
  }

  if (status === "anonymous") {
    return (
      <main className="shell">
        <LoginScreen onRequestLink={requestLink} error={error} />
      </main>
    );
  }

  return (
    <main className="shell">
      <header className="scorebug">
        <span className="brand">Tippspiel</span>
        <span className="bug-stat">
          <span className="bug-label">Angemeldet als</span>
          <span className="bug-value league-account-name">{account.displayName}</span>
        </span>
        <button className="button ghost" onClick={logout} aria-label="Abmelden">
          ⏻
        </button>
      </header>

      <nav className="league-tabs">
        <button
          className={`league-tab${tab === "schedule" ? " active" : ""}`}
          onClick={() => {
            setTab("schedule");
            setOpenLeagueId(null);
          }}
        >
          Spieltag
        </button>
        <button
          className={`league-tab${tab === "leagues" ? " active" : ""}`}
          onClick={() => {
            setTab("leagues");
            setOpenLeagueId(null);
          }}
        >
          Meine Ligen
        </button>
      </nav>

      {tab === "schedule" && <MatchdayScreen />}

      {tab === "leagues" && openLeagueId === null && <LeaguesScreen onOpen={setOpenLeagueId} />}

      {tab === "leagues" && openLeagueId !== null && (
        <LeagueDetailScreen
          leagueId={openLeagueId}
          onBack={() => setOpenLeagueId(null)}
          onLeft={() => setOpenLeagueId(null)}
        />
      )}

      <details className="danger-zone">
        <summary className="button ghost wide">Konto löschen</summary>
        <p className="hint">
          Löscht E-Mail-Adresse und Anzeigenamen unwiderruflich. Bereits gewertete Tipps
          verschwinden aus allen Ranglisten.
        </p>
        <button
          className="button danger"
          onClick={() => {
            if (window.confirm("Konto wirklich löschen? Das lässt sich nicht rückgängig machen.")) {
              deleteAccount();
            }
          }}
        >
          Konto endgültig löschen
        </button>
      </details>
    </main>
  );
}
