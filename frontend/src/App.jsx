import { useEffect, useState } from "react";
import { useRoom } from "./useRoom.js";

const STATUS_LABEL = {
  connecting: "Verbinde",
  online: "Verbunden",
  offline: "Getrennt",
};

const MIN_STAKE = 25;

function JoinScreen({ onJoin, status }) {
  const [name, setName] = useState(
    () => window.localStorage.getItem("watchparty.name") ?? ""
  );
  const trimmed = name.trim();

  return (
    <div className="join">
      <p className="eyebrow">Watchparty</p>
      <h1 className="display">Wer bist du?</h1>
      <input
        className="field"
        value={name}
        maxLength={20}
        placeholder="Dein Name"
        autoComplete="off"
        onChange={(event) => setName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter" && trimmed) onJoin(trimmed);
        }}
      />
      <button
        className="button primary"
        disabled={!trimmed || status !== "online"}
        onClick={() => onJoin(trimmed)}
      >
        Mitspielen
      </button>
    </div>
  );
}

function Leaderboard({ players, playerId }) {
  const sorted = [...players].sort((a, b) => b.points - a.points);
  return (
    <ol className="roster">
      {sorted.map((player) => (
        <li
          key={player.id}
          className={`row${player.connected ? "" : " away"}${
            player.id === playerId ? " self" : ""
          }`}
        >
          <span className="name">
            {player.name}
            {player.host && <span className="tag">Host</span>}
            {player.paused && <span className="tag pause">Pausiert</span>}
            {!player.connected && !player.paused && (
              <span className="tag away-tag">Getrennt</span>
            )}
          </span>
          <span className="points">{player.points}</span>
        </li>
      ))}
    </ol>
  );
}

/** Countdown aus closesAt und dem einmal gebildeten Uhren-Offset (Etappe 4/5). */
function Countdown({ closesAt, serverNow }) {
  const [, tick] = useState(0);
  useEffect(() => {
    const id = window.setInterval(() => tick((n) => n + 1), 250);
    return () => window.clearInterval(id);
  }, []);
  const remainingMs = Math.max(0, closesAt - serverNow());
  return <p className="countdown">{Math.ceil(remainingMs / 1000)}s</p>;
}

function outcomeLabel(market, outcomeId) {
  return market?.outcomes.find((outcome) => outcome.id === outcomeId)?.label ?? outcomeId;
}

function BettingForm({ market, ownPoints, onPlaceBet }) {
  const [outcomeId, setOutcomeId] = useState(null);
  const [stake, setStake] = useState(Math.min(MIN_STAKE, ownPoints));

  return (
    <div className="market">
      <h2 className="display">{market.question}</h2>
      <ul className="options">
        {market.outcomes.map((outcome) => (
          <li key={outcome.id}>
            <button
              className={`button option${outcomeId === outcome.id ? " selected" : ""}`}
              onClick={() => setOutcomeId(outcome.id)}
            >
              {outcome.label}
              {outcome.note && <span className="note">{outcome.note}</span>}
            </button>
          </li>
        ))}
      </ul>

      {ownPoints < MIN_STAKE ? (
        <p className="hint">
          Du hast weniger als den Mindesteinsatz ({MIN_STAKE}) -- ein Tipp geht automatisch
          All-in mit deinen {ownPoints} Punkten.
        </p>
      ) : (
        <label className="stake">
          Einsatz
          <input
            className="field"
            type="number"
            min={MIN_STAKE}
            max={ownPoints}
            step={5}
            value={stake}
            onChange={(event) => setStake(Number(event.target.value))}
          />
        </label>
      )}

      <button
        className="button primary"
        disabled={!outcomeId}
        onClick={() => onPlaceBet(outcomeId, stake)}
      >
        Tipp abgeben
      </button>
    </div>
  );
}

function RevealedBets({ bets, players, market }) {
  const nameOf = (id) => players.find((player) => player.id === id)?.name ?? "?";
  return (
    <ul className="reveal">
      {bets.length === 0 && <li className="hint">Niemand hat getippt.</li>}
      {bets.map((bet) => (
        <li key={bet.playerId}>
          <span className="name">{nameOf(bet.playerId)}</span>
          <span>{outcomeLabel(market, bet.outcomeId)}</span>
          <span className="points">{bet.stake}</span>
        </li>
      ))}
    </ul>
  );
}

function ResolveForm({ market, onResolve }) {
  return (
    <div className="market">
      <p className="hint">Welcher Ausgang war es wirklich?</p>
      <ul className="options">
        {market.outcomes.map((outcome) => (
          <li key={outcome.id}>
            <button className="button option" onClick={() => onResolve(outcome.id)}>
              {outcome.label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

function ResultView({ round, players }) {
  const nameOf = (id) => players.find((player) => player.id === id)?.name ?? "?";
  if (round.annulled) {
    return <p className="hint">Niemand hat getippt -- die Runde wurde annulliert.</p>;
  }
  const deltas = Object.entries(round.deltas ?? {});
  return (
    <div className="result">
      <p className="display">{outcomeLabel(round.market, round.winningOutcomeId)}</p>
      <p className="hint">Pool: {round.pool} Punkte</p>
      <ul className="reveal">
        {deltas.map(([id, delta]) => (
          <li key={id}>
            <span className="name">{nameOf(id)}</span>
            <span className={delta >= 0 ? "positive" : "negative"}>
              {delta >= 0 ? `+${delta}` : delta}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function HostControls({ phase, onOpenMarket, onCloseMarket }) {
  if (phase === "IDLE" || phase === "RESOLVED") {
    return (
      <button className="button primary" onClick={onOpenMarket}>
        Markt oeffnen
      </button>
    );
  }
  if (phase === "OPEN") {
    return (
      <button className="button" onClick={onCloseMarket}>
        Jetzt schliessen
      </button>
    );
  }
  return null;
}

export default function App() {
  const {
    status,
    state,
    playerId,
    error,
    yourBet,
    join,
    openMarket,
    closeMarket,
    placeBet,
    resolve,
    serverNow,
  } = useRoom();
  const joined = Boolean(playerId) && Boolean(state);

  if (!joined) {
    return (
      <main className="shell">
        <JoinScreen onJoin={join} status={status} />
        {error && <p className="error">{error}</p>}
        <footer className={`status ${status}`}>{STATUS_LABEL[status]}</footer>
      </main>
    );
  }

  const isHost = state.hostPlayerId === playerId;
  const ownPoints = state.players.find((player) => player.id === playerId)?.points ?? 0;

  return (
    <main className="shell">
      <header className="header">
        <p className="eyebrow">Im Raum</p>
        <h1 className="display">{state.players.length} dabei</h1>
      </header>

      {state.phase === "IDLE" && (
        <p className="hint">Der Host kann den naechsten Markt oeffnen.</p>
      )}

      {state.phase === "OPEN" && state.market && (
        <>
          <Countdown closesAt={state.closesAt} serverNow={serverNow} />
          <p className="counter">
            {state.betCount} von {state.participantCount} haben getippt
          </p>
          {yourBet ? (
            <p className="hint">
              Du hast auf <strong>{outcomeLabel(state.market, yourBet.outcomeId)}</strong> mit{" "}
              {yourBet.stake} Punkten getippt.
            </p>
          ) : (
            <BettingForm market={state.market} ownPoints={ownPoints} onPlaceBet={placeBet} />
          )}
        </>
      )}

      {state.phase === "CLOSED" && state.market && (
        <>
          <h2 className="display">{state.market.question}</h2>
          <RevealedBets bets={state.revealedBets ?? []} players={state.players} market={state.market} />
          {isHost && <ResolveForm market={state.market} onResolve={resolve} />}
        </>
      )}

      {state.phase === "RESOLVED" && state.market && (
        <ResultView
          round={{
            market: state.market,
            winningOutcomeId: state.winningOutcomeId,
            pool: state.pool,
            annulled: state.annulled,
            deltas: state.deltas,
          }}
          players={state.players}
        />
      )}

      {isHost ? (
        <section className="host">
          <HostControls phase={state.phase} onOpenMarket={openMarket} onCloseMarket={closeMarket} />
        </section>
      ) : (
        state.phase === "IDLE" && <p className="hint">Der Host steuert die Runden.</p>
      )}

      <Leaderboard players={state.players} playerId={playerId} />

      {error && <p className="error">{error}</p>}
      <footer className={`status ${status}`}>{STATUS_LABEL[status]}</footer>
    </main>
  );
}
