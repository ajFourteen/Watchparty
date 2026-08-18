import { useEffect, useState } from "react";
import { leagueApi } from "./api.js";

const REGULAR_SEASON_WEEKS = 18;

/** Eine NFL-Saison heißt nach ihrem Startjahr — bis Februar zählt noch die vorherige. */
function currentSeasonYear() {
  const now = new Date();
  return now.getMonth() >= 2 ? now.getFullYear() : now.getFullYear() - 1;
}

function PredictionForm({ game, onSubmit }) {
  const initialHome = String(game.ownPrediction?.home ?? "");
  const initialAway = String(game.ownPrediction?.away ?? "");
  const [home, setHome] = useState(initialHome);
  const [away, setAway] = useState(initialAway);
  const [saving, setSaving] = useState(false);

  const numeric = (value) => value.replace(/[^0-9]/g, "");
  const edited = home !== initialHome || away !== initialAway;
  const canSubmit = home !== "" && away !== "" && !saving && (!game.ownPrediction || edited);

  return (
    <div className="predict-form">
      <label className="predict-field">
        {game.awayTeamName}
        <input
          className="field score-field"
          type="text"
          inputMode="numeric"
          value={away}
          onChange={(event) => setAway(numeric(event.target.value))}
        />
      </label>
      <label className="predict-field">
        {game.homeTeamName}
        <input
          className="field score-field"
          type="text"
          inputMode="numeric"
          value={home}
          onChange={(event) => setHome(numeric(event.target.value))}
        />
      </label>
      <button
        className="button primary"
        disabled={!canSubmit}
        onClick={async () => {
          setSaving(true);
          try {
            await onSubmit(Number(home), Number(away));
          } finally {
            setSaving(false);
          }
        }}
      >
        {game.ownPrediction ? "Tipp ändern" : "Tipp abgeben"}
      </button>
    </div>
  );
}

function GameCard({ game, onSubmit }) {
  const kickoff = new Date(game.kickoff);
  const kickedOff = kickoff.getTime() <= Date.now();

  return (
    <li className="game-card">
      <div className="game-head">
        <span className="game-teams">
          {game.awayTeamName} @<br />
          {game.homeTeamName}
        </span>
        <span className="game-kickoff">
          {kickoff.toLocaleString("de-DE", { weekday: "short", day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" })}
        </span>
      </div>

      {game.finalScore && (
        <p className="game-final">
          Endstand: {game.homeTeamName} {game.finalScore.home}:{game.finalScore.away} {game.awayTeamName}
        </p>
      )}

      {!kickedOff && <PredictionForm game={game} onSubmit={onSubmit} />}

      {kickedOff && game.ownPrediction && (
        <p className="hint">
          Dein Tipp: {game.ownPrediction.home}:{game.ownPrediction.away}
        </p>
      )}
      {kickedOff && !game.ownPrediction && <p className="hint">Kein Tipp abgegeben.</p>}

      {kickedOff && game.otherPredictions.length > 0 && (
        <ul className="other-predictions">
          {game.otherPredictions.map((entry) => (
            <li key={entry.displayName}>
              <span className="name">{entry.displayName}</span>
              <span className="points">
                {entry.score.home}:{entry.score.away}
              </span>
            </li>
          ))}
        </ul>
      )}
    </li>
  );
}

/** Spieltag abrufen und tippen (Kapitel 13.4). Fremde Tipps liefert der Server erst ab Anstoß — hier ist nichts zu verstecken, es kommt schlicht nicht an. */
export function MatchdayScreen() {
  const [seasonYear] = useState(currentSeasonYear);
  const [week, setWeek] = useState(1);
  const [matchday, setMatchday] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      setMatchday(await leagueApi.schedule(seasonYear, week));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [week]);

  const submitPrediction = async (gameId, home, away) => {
    await leagueApi.submitPrediction(gameId, home, away);
    await load();
  };

  return (
    <div className="league-card matchday">
      <div className="week-switch">
        <button className="button ghost" disabled={week <= 1} onClick={() => setWeek((w) => w - 1)}>
          ‹
        </button>
        <span className="eyebrow">
          Saison {seasonYear} · Spieltag {week}
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

      {!loading && matchday && matchday.games.length === 0 && (
        <p className="hint">Für diesen Spieltag sind noch keine Spiele bekannt.</p>
      )}

      {!loading && matchday && matchday.games.length > 0 && (
        <ul className="games">
          {matchday.games.map((game) => (
            <GameCard
              key={game.gameId}
              game={game}
              onSubmit={(home, away) => submitPrediction(game.gameId, home, away)}
            />
          ))}
        </ul>
      )}
    </div>
  );
}
