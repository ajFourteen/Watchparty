import { useState } from "react";
import { useLeagueAccount } from "./useLeagueAccount.js";
import { LoginScreen } from "./LoginScreen.jsx";
import { MatchdayScreen } from "./MatchdayScreen.jsx";
import { LeaguesScreen } from "./LeaguesScreen.jsx";
import { LeagueDetailScreen } from "./LeagueDetailScreen.jsx";

/** Das Tippspiel: Anmeldung, Spieltag, Ligen — unabhängig von den Live-Wetten (CLAUDE.md, "Beide Modi teilen sich die Anwendung und sonst nichts"). */
export function League() {
  const { status, account, error, requestLink, logout, deleteAccount, confirmLogin } =
    useLeagueAccount();
  const [tab, setTab] = useState("schedule");
  const [openLeagueId, setOpenLeagueId] = useState(null);

  if (status === "loading") {
    return (
      <main className="shell shell--league">
        <p className="hint">Lädt …</p>
      </main>
    );
  }

  if (status === "pendingLogin") {
    return (
      <main className="shell shell--league">
        <div className="league-card">
          <p className="eyebrow">Tippspiel</p>
          <h1 className="display">Anmeldelink geöffnet</h1>
          <p className="hint">
            Falls das hier eine Vorschau deines Mail-Programms ist: Der Link bleibt
            gültig, bis du wirklich auf "Jetzt anmelden" klickst — kopiere ihn also
            ruhig erst in deinen Browser, bevor du klickst.
          </p>
          <button className="button primary wide" onClick={confirmLogin}>
            Jetzt anmelden
          </button>
        </div>
      </main>
    );
  }

  if (status === "anonymous") {
    return (
      <main className="shell shell--league">
        <LoginScreen onRequestLink={requestLink} error={error} />
      </main>
    );
  }

  return (
    <main className="shell shell--league">
      <header className="scorebug">
        <span className="bug-stat">
          <span className="bug-label">Angemeldet als</span>
          <span className="bug-value league-account-name">{account.displayName}</span>
        </span>
        <span className="bug-stat">
          <span className="bug-label">Punktestand</span>
          <span className="bug-value">{account.totalPoints}</span>
        </span>
        <details className="account-menu">
          <summary className="button ghost" aria-label="Konto-Menü">
            ⋮
          </summary>
          <div className="account-menu-panel">
            <button className="button ghost wide" onClick={logout}>
              Abmelden
            </button>
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
          </div>
        </details>
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
    </main>
  );
}
