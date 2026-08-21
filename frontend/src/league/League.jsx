import { useEffect, useState } from "react";
import { useLeagueAccount } from "./useLeagueAccount.js";
import { LoginScreen } from "./LoginScreen.jsx";
import { MatchdayScreen } from "./MatchdayScreen.jsx";
import { ReportScreen } from "./ReportScreen.jsx";
import { LeaguesScreen } from "./LeaguesScreen.jsx";
import { LeagueDetailScreen } from "./LeagueDetailScreen.jsx";
import { LeagueGuide } from "./LeagueGuide.jsx";

const GUIDE_SEEN_KEY = "watchparty.league.guideSeen";

/** Das Tippspiel: Anmeldung, Spieltag, Ligen — unabhängig von den Live-Wetten (CLAUDE.md, "Beide Modi teilen sich die Anwendung und sonst nichts"). */
export function League() {
  const {
    status,
    account,
    error,
    requestLink,
    logout,
    deleteAccount,
    confirmLogin,
    continueAfterUnsubscribe,
    optInReportMail,
    optOutReportMail,
  } = useLeagueAccount();
  const [tab, setTab] = useState("schedule");
  const [openLeagueId, setOpenLeagueId] = useState(null);
  const [guideOpen, setGuideOpen] = useState(false);

  // Bei der ersten Anmeldung auf diesem Gerät geht die Kurzanleitung von
  // selbst auf (13.10-b); danach nur noch auf Wunsch, dieselbe Bauweise wie
  // bei den Live-Wetten (GUIDE_SEEN_KEY in Watchparty.jsx).
  useEffect(() => {
    if (status === "authenticated" && !window.localStorage.getItem(GUIDE_SEEN_KEY)) {
      window.localStorage.setItem(GUIDE_SEEN_KEY, "1");
      setGuideOpen(true);
    }
  }, [status]);

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

  if (status === "unsubscribed") {
    return (
      <main className="shell shell--league">
        <div className="league-card">
          <p className="eyebrow">Tippspiel</p>
          <h1 className="display">Abgemeldet</h1>
          <p className="hint">
            Du bekommst den Spieltags-Report nicht mehr per Mail. Bestellen kannst du ihn
            jederzeit im Konto-Menü wieder.
          </p>
          <button className="button primary wide" onClick={continueAfterUnsubscribe}>
            Weiter
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
      <header className="scorebug scorebug--league">
        <span className="bug-inline">
          Angemeldet als <strong className="league-account-name">{account.displayName}</strong>
        </span>
        <span className="bug-inline">
          Punktestand <strong>{account.totalPoints}</strong>
        </span>
        <button className="button ghost" onClick={() => setGuideOpen(true)} aria-label="Anleitung">
          ?
        </button>
        <details className="account-menu">
          <summary className="button ghost" aria-label="Konto-Menü">
            ⋮
          </summary>
          <div className="account-menu-panel">
            <label className="account-menu-toggle">
              <input
                type="checkbox"
                checked={account.reportMailOptIn}
                onChange={(e) => (e.target.checked ? optInReportMail() : optOutReportMail())}
              />
              Spieltags-Report per Mail
            </label>
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
          className={`league-tab${tab === "report" ? " active" : ""}`}
          onClick={() => {
            setTab("report");
            setOpenLeagueId(null);
          }}
        >
          Bilanz
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

      {tab === "report" && <ReportScreen />}

      {tab === "leagues" && openLeagueId === null && <LeaguesScreen onOpen={setOpenLeagueId} />}

      {tab === "leagues" && openLeagueId !== null && (
        <LeagueDetailScreen
          leagueId={openLeagueId}
          onBack={() => setOpenLeagueId(null)}
          onLeft={() => setOpenLeagueId(null)}
        />
      )}

      {guideOpen && <LeagueGuide onClose={() => setGuideOpen(false)} />}
    </main>
  );
}
