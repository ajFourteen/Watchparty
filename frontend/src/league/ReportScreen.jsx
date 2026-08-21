import { useEffect, useState } from "react";
import { leagueApi } from "./api.js";

const REGULAR_SEASON_WEEKS = 18;

/** Eine NFL-Saison heißt nach ihrem Startjahr — bis Februar zählt noch die vorherige. */
function currentSeasonYear() {
  const now = new Date();
  return now.getMonth() >= 2 ? now.getFullYear() : now.getFullYear() - 1;
}

function ReportEntry({ entry }) {
  return (
    <li className="game-card">
      <div className="game-head">
        <span className="game-teams">
          {entry.awayTeamName} @<br />
          {entry.homeTeamName}
        </span>
        <span className="report-points">{entry.points} P.</span>
      </div>

      <p className="game-final">
        Endstand: {entry.homeTeamName} {entry.finalScore.home}:{entry.finalScore.away} {entry.awayTeamName}
      </p>

      {entry.ownPrediction ? (
        <p className="hint">
          Dein Tipp: {entry.ownPrediction.home}:{entry.ownPrediction.away}
        </p>
      ) : (
        <p className="hint">Kein Tipp abgegeben.</p>
      )}
    </li>
  );
}

/** Die eigene Bilanz eines Spieltags (Kapitel 13.9, Feature 006 Schnitt 1) — nur gewertete Spiele, nur das eigene Konto. */
export function ReportScreen() {
  const [seasonYear] = useState(currentSeasonYear);
  const [week, setWeek] = useState(1);
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    leagueApi
      .matchdayReport(seasonYear, week)
      .then((result) => {
        if (!cancelled) setReport(result);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [seasonYear, week]);

  return (
    <div className="league-card matchday">
      <div className="week-switch">
        <button className="button ghost" disabled={week <= 1} onClick={() => setWeek((w) => w - 1)}>
          ‹
        </button>
        <span className="eyebrow">
          Bilanz · Saison {seasonYear} · Spieltag {week}
        </span>
        <button
          className="button ghost"
          disabled={week >= REGULAR_SEASON_WEEKS}
          onClick={() => setWeek((w) => w + 1)}
        >
          ›
        </button>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && <p className="hint">Lädt …</p>}

      {!loading && report && (
        <p className="report-summary">
          Spieltagssumme: <strong>{report.totalPoints}</strong> Wertungspunkte
        </p>
      )}

      {!loading && report && report.games.length === 0 && (
        <p className="hint">Für diesen Spieltag ist noch kein Spiel gewertet.</p>
      )}

      {!loading && report && report.games.length > 0 && (
        <ul className="games">
          {report.games.map((entry) => (
            <ReportEntry key={entry.gameId} entry={entry} />
          ))}
        </ul>
      )}
    </div>
  );
}
