import { useEffect, useState } from "react";
import { leagueApi } from "./api.js";

function StandingsTable({ entries }) {
  if (entries.length === 0) {
    return <p className="hint">Noch keine gewerteten Spiele.</p>;
  }
  return (
    <ol className="roster">
      {entries.map((entry) => (
        <li key={entry.displayName} className={`row${entry.isSelf ? " self" : ""}`}>
          <span className="rank">{entry.rank}</span>
          <span className="name">{entry.displayName}</span>
          <span className="points">{entry.totalPoints}</span>
        </li>
      ))}
    </ol>
  );
}

/** Liga-Detail: Code zum Weitergeben, Mitglieder, Saison- und Spieltagsrangliste (Kriterium 31/33/35). */
export function LeagueDetailScreen({ leagueId, onLeft, onBack }) {
  const [league, setLeague] = useState(null);
  const [seasonStandings, setSeasonStandings] = useState(null);
  const [matchdayWeek, setMatchdayWeek] = useState(1);
  const [matchdayStandings, setMatchdayStandings] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    leagueApi
      .leagueDetail(leagueId)
      .then(setLeague)
      .catch((e) => setError(e.message));
    leagueApi
      .seasonStandings(leagueId)
      .then(setSeasonStandings)
      .catch((e) => setError(e.message));
  }, [leagueId]);

  useEffect(() => {
    leagueApi
      .matchdayStandings(leagueId, matchdayWeek)
      .then(setMatchdayStandings)
      .catch((e) => setError(e.message));
  }, [leagueId, matchdayWeek]);

  const leave = async () => {
    if (!window.confirm(`${league?.name ?? "Diese Liga"} wirklich verlassen?`)) return;
    await leagueApi.leaveLeague(leagueId);
    onLeft();
  };

  if (error) {
    return (
      <div className="league-card">
        <p className="error">{error}</p>
        <button className="button ghost wide" onClick={onBack}>
          Zurück
        </button>
      </div>
    );
  }

  if (!league) {
    return (
      <div className="league-card">
        <p className="hint">Lädt …</p>
      </div>
    );
  }

  return (
    <div className="league-card">
      <button className="button ghost" onClick={onBack}>
        ‹ Meine Ligen
      </button>
      <h1 className="display">{league.name}</h1>
      <p className="hint">
        Beitrittscode <span className="tag room-code">{league.code}</span> — weitergeben, wer mitspielen soll.
      </p>

      <p className="eyebrow">Mitglieder ({league.memberNames.length})</p>
      <p className="hint">{league.memberNames.join(", ")}</p>

      <p className="eyebrow">Rangliste — ganze Saison</p>
      {seasonStandings && <StandingsTable entries={seasonStandings} />}

      <div className="week-switch">
        <button className="button ghost" disabled={matchdayWeek <= 1} onClick={() => setMatchdayWeek((w) => w - 1)}>
          ‹
        </button>
        <span className="eyebrow">Rangliste — Spieltag {matchdayWeek}</span>
        <button className="button ghost" disabled={matchdayWeek >= 18} onClick={() => setMatchdayWeek((w) => w + 1)}>
          ›
        </button>
      </div>
      {matchdayStandings && <StandingsTable entries={matchdayStandings} />}

      <button className="button ghost wide" onClick={leave}>
        Liga verlassen
      </button>
    </div>
  );
}
