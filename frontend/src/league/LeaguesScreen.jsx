import { useEffect, useState } from "react";
import { leagueApi } from "./api.js";

function CreateLeagueForm({ onCreated }) {
  const [name, setName] = useState("");
  const [seasonYear, setSeasonYear] = useState(String(new Date().getFullYear()));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const canSubmit = name.trim() !== "" && seasonYear !== "" && !busy;

  const submit = async () => {
    if (!canSubmit) return;
    setBusy(true);
    setError(null);
    try {
      await leagueApi.createLeague(name.trim(), Number(seasonYear));
      setName("");
      onCreated();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="league-form">
      <p className="eyebrow">Liga anlegen</p>
      <input
        className="field"
        value={name}
        maxLength={40}
        placeholder="Name der Liga"
        onChange={(event) => setName(event.target.value)}
      />
      <input
        className="field"
        type="text"
        inputMode="numeric"
        value={seasonYear}
        placeholder="Saison"
        onChange={(event) => setSeasonYear(event.target.value.replace(/[^0-9]/g, ""))}
      />
      <button className="button primary" disabled={!canSubmit} onClick={submit}>
        Anlegen
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

function JoinLeagueForm({ onJoined }) {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const canSubmit = code.trim() !== "" && !busy;

  const submit = async () => {
    if (!canSubmit) return;
    setBusy(true);
    setError(null);
    try {
      await leagueApi.joinLeague(code.trim());
      setCode("");
      onJoined();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="league-form">
      <p className="eyebrow">Liga beitreten</p>
      <input
        className="field"
        value={code}
        maxLength={6}
        placeholder="Beitrittscode"
        autoComplete="off"
        onChange={(event) => setCode(event.target.value.toUpperCase())}
        onKeyDown={(event) => {
          if (event.key === "Enter" && canSubmit) submit();
        }}
      />
      <button className="button primary" disabled={!canSubmit} onClick={submit}>
        Beitreten
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

/** Meine Ligen, anlegen, beitreten (Kriterium 28/29/30). */
export function LeaguesScreen({ onOpen }) {
  const [leagues, setLeagues] = useState(null);
  const [error, setError] = useState(null);

  const load = async () => {
    try {
      setLeagues(await leagueApi.myLeagues());
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="league-card">
      <p className="eyebrow">Meine Ligen</p>
      {error && <p className="error">{error}</p>}
      {leagues && leagues.length === 0 && <p className="hint">Noch in keiner Liga.</p>}
      {leagues && leagues.length > 0 && (
        <ul className="roster">
          {leagues.map((league) => (
            <li key={league.id} className="row">
              <button className="league-row-button" onClick={() => onOpen(league.id)}>
                <span className="name">
                  {league.name}
                  {league.isManager && <span className="tag">Verwalter</span>}
                </span>
                <span className="points">{league.seasonYear}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      <CreateLeagueForm onCreated={load} />
      <JoinLeagueForm onJoined={load} />
    </div>
  );
}
