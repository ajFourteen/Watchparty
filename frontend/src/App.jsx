import { useState } from "react";
import { useRoom } from "./useRoom.js";

const STATUS_LABEL = {
  connecting: "Verbinde",
  online: "Verbunden",
  offline: "Getrennt",
};

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

function Roster({ players, playerId }) {
  return (
    <ol className="roster">
      {players.map((player) => (
        <li
          key={player.id}
          className={`row${player.connected ? "" : " away"}${
            player.id === playerId ? " self" : ""
          }`}
        >
          <span className="name">
            {player.name}
            {player.host && <span className="tag">Host</span>}
          </span>
          <span className="points">{player.points}</span>
        </li>
      ))}
    </ol>
  );
}

export default function App() {
  const { status, state, playerId, error, join, hostAction } = useRoom();
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

  return (
    <main className="shell">
      <header className="header">
        <p className="eyebrow">Im Raum</p>
        <h1 className="display">{state.players.length} dabei</h1>
      </header>

      <Roster players={state.players} playerId={playerId} />

      {isHost ? (
        <section className="host">
          <p className="hint">
            Du bist Host. Hier kommen spaeter Markt oeffnen, jetzt schliessen und
            aufloesen hin.
          </p>
          <button className="button primary" onClick={hostAction}>
            Host-Aktion senden
          </button>
        </section>
      ) : (
        <p className="hint">Der Host steuert die Runden.</p>
      )}

      <p className="counter">
        Host-Aktionen <strong>{state.hostActionCount}</strong>
      </p>

      {error && <p className="error">{error}</p>}
      <footer className={`status ${status}`}>{STATUS_LABEL[status]}</footer>
    </main>
  );
}
