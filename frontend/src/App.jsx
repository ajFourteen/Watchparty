import { useEffect, useState } from "react";
import { useRoom } from "./useRoom.js";
import { Guide } from "./Guide.jsx";

const STATUS_LABEL = {
  connecting: "Verbinde",
  online: "Live",
  offline: "Getrennt",
};

const MIN_STAKE = 25;

/** Merkt sich, dass die Anleitung schon einmal von selbst aufging. */
const GUIDE_SEEN_KEY = "watchparty.guideSeen";

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
      {sorted.map((player, index) => (
        <li
          key={player.id}
          className={`row${player.connected ? "" : " away"}${
            player.id === playerId ? " self" : ""
          }`}
        >
          <span className="rank">{index + 1}</span>
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

/** Countdown aus closesAt und dem einmal gebildeten Uhren-Offset (ADR-003). */
function Countdown({ closesAt, serverNow }) {
  const [, tick] = useState(0);
  useEffect(() => {
    const id = window.setInterval(() => tick((n) => n + 1), 250);
    return () => window.clearInterval(id);
  }, []);
  const remainingMs = Math.max(0, closesAt - serverNow());
  const seconds = Math.ceil(remainingMs / 1000);
  return (
    <p className={`countdown${seconds <= 5 ? " urgent" : ""}`}>
      {String(seconds).padStart(2, "0")}
    </p>
  );
}

function outcomeLabel(bet, outcomeId) {
  return bet?.outcomes.find((outcome) => outcome.id === outcomeId)?.label ?? outcomeId;
}

function PickForm({ bet, ownPoints, onPlacePick }) {
  const [outcomeId, setOutcomeId] = useState(null);
  const [stake, setStake] = useState(Math.min(MIN_STAKE, ownPoints));

  return (
    <div className="bet">
      <h2 className="display">{bet.question}</h2>
      {bet.note && <p className="rule">{bet.note}</p>}
      <ul className="options">
        {bet.outcomes.map((outcome) => (
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
          Du hast weniger als den Mindesteinsatz ({MIN_STAKE}) — ein Tipp geht automatisch
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
        onClick={() => onPlacePick(outcomeId, stake)}
      >
        Tipp abgeben
      </button>
    </div>
  );
}

function RevealedPicks({ picks, players, bet }) {
  const nameOf = (id) => players.find((player) => player.id === id)?.name ?? "?";
  return (
    <ul className="reveal">
      {picks.length === 0 && <li className="hint">Niemand hat getippt.</li>}
      {picks.map((pick) => (
        <li key={pick.playerId}>
          <span className="name">{nameOf(pick.playerId)}</span>
          <span>{outcomeLabel(bet, pick.outcomeId)}</span>
          <span className="points">{pick.stake}</span>
        </li>
      ))}
    </ul>
  );
}

function ResolveForm({ bet, onResolve }) {
  return (
    <div className="bet">
      <p className="eyebrow">Auflösen</p>
      <p className="hint">Welcher Ausgang war es wirklich?</p>
      <ul className="options">
        {bet.outcomes.map((outcome) => (
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

function ResultView({ bet, winningOutcomeId, pool, annulled, annulReason, deltas, players }) {
  const nameOf = (id) => players.find((player) => player.id === id)?.name ?? "?";
  if (annulled) {
    return (
      <p className="hint">
        {annulReason === "HOST"
          ? "Der Host hat die Runde abgebrochen — keine Punkte, keine Strafen."
          : "Niemand hat getippt — die Runde wurde annulliert."}
      </p>
    );
  }
  const entries = Object.entries(deltas ?? {});
  return (
    <div className="result">
      <p className="eyebrow">Ergebnis</p>
      <p className="display">{outcomeLabel(bet, winningOutcomeId)}</p>
      <p className="hint">Pool: {pool} Punkte</p>
      <ul className="reveal">
        {entries.map(([id, delta]) => (
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

/**
 * Die Wett-Auswahl steht nur in IDLE und RESOLVED offen; welche Wette passt,
 * weiß nur der Host vor dem Fernseher (Anforderung 5).
 */
function HostControls({ phase, catalog, onOpenBet, onCloseBet, onAnnul }) {
  if (phase === "OPEN" || phase === "CLOSED") {
    return (
      <div className="chooser">
        {phase === "OPEN" && (
          <button className="button danger" onClick={onCloseBet}>
            Jetzt schließen
          </button>
        )}
        {/* Der Notausgang, wenn die offene Wette nicht mehr zum Spiel passt
            (Anforderung 8.6). Bewusst unscheinbar: Er ist die Ausnahme, und
            ein Fehlgriff kostet allen die Runde. */}
        <button className="button ghost wide" onClick={onAnnul}>
          Runde annullieren
        </button>
      </div>
    );
  }
  if (phase === "IDLE" || phase === "RESOLVED") {
    return (
      <div className="chooser">
        <p className="eyebrow">Nächste Wette öffnen</p>
        <ul className="options">
          {catalog.map((bet) => (
            <li key={bet.id}>
              <button className="button option" onClick={() => onOpenBet(bet.id)}>
                {bet.question}
                {bet.note && <span className="note">{bet.note}</span>}
              </button>
            </li>
          ))}
        </ul>
      </div>
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
    yourPick,
    catalog,
    join,
    openBet,
    closeBet,
    placePick,
    resolve,
    annul,
    serverNow,
  } = useRoom();

  const [guideOpen, setGuideOpen] = useState(false);
  const joined = Boolean(playerId) && Boolean(state);

  // Beim ersten Abend geht die Anleitung von selbst auf; danach nur noch auf
  // Wunsch, damit sie niemandem jede Runde im Weg steht.
  useEffect(() => {
    if (joined && !window.localStorage.getItem(GUIDE_SEEN_KEY)) {
      window.localStorage.setItem(GUIDE_SEEN_KEY, "1");
      setGuideOpen(true);
    }
  }, [joined]);

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
      <header className="scorebug">
        <span className="brand">Watchparty</span>
        <span className="bug-stat">
          <span className="bug-label">Punkte</span>
          <span className="bug-value">{ownPoints}</span>
        </span>
        <span className="bug-stat">
          <span className="bug-label">Dabei</span>
          <span className="bug-value">{state.players.length}</span>
        </span>
        <button className="button ghost" onClick={() => setGuideOpen(true)} aria-label="Anleitung">
          ?
        </button>
      </header>

      {state.phase === "IDLE" && !isHost && (
        <p className="hint">Der Host öffnet die nächste Wette.</p>
      )}

      {state.phase === "OPEN" && state.bet && (
        <section className="stage">
          <Countdown closesAt={state.closesAt} serverNow={serverNow} />
          <p className="counter">
            {state.pickCount} von {state.participantCount} haben getippt
          </p>
          {yourPick ? (
            <p className="locked">
              Dein Tipp: <strong>{outcomeLabel(state.bet, yourPick.outcomeId)}</strong> mit{" "}
              {yourPick.stake} Punkten.
            </p>
          ) : (
            <PickForm bet={state.bet} ownPoints={ownPoints} onPlacePick={placePick} />
          )}
        </section>
      )}

      {state.phase === "CLOSED" && state.bet && (
        <section className="stage">
          <p className="eyebrow">Geschlossen</p>
          <h2 className="display">{state.bet.question}</h2>
          <RevealedPicks
            picks={state.revealedPicks ?? []}
            players={state.players}
            bet={state.bet}
          />
          {isHost && <ResolveForm bet={state.bet} onResolve={resolve} />}
        </section>
      )}

      {state.phase === "RESOLVED" && state.bet && (
        <section className="stage">
          <ResultView
            bet={state.bet}
            winningOutcomeId={state.winningOutcomeId}
            pool={state.pool}
            annulled={state.annulled}
            annulReason={state.annulReason}
            deltas={state.deltas}
            players={state.players}
          />
        </section>
      )}

      {isHost && (
        <section className="host">
          <HostControls
            phase={state.phase}
            catalog={catalog}
            onOpenBet={openBet}
            onCloseBet={closeBet}
            onAnnul={annul}
          />
        </section>
      )}

      <Leaderboard players={state.players} playerId={playerId} />

      {guideOpen && <Guide catalog={catalog} onClose={() => setGuideOpen(false)} />}

      {error && <p className="error">{error}</p>}
      <footer className={`status ${status}`}>{STATUS_LABEL[status]}</footer>
    </main>
  );
}
