import { useEffect, useState } from "react";
import { useRoom } from "./useRoom.js";
import { useWakeLock } from "./useWakeLock.js";
import { Guide } from "./Guide.jsx";

const STATUS_LABEL = {
  connecting: "Verbinde",
  online: "Live",
  offline: "Getrennt",
};

/** Merkt sich, dass die Anleitung schon einmal von selbst aufging. */
const GUIDE_SEEN_KEY = "watchparty.guideSeen";

/**
 * Ob der Punktestand der anderen aufgeklappt ist. Pro Gerät gemerkt und
 * standardmäßig zu: Aufgeklappt schiebt er die offene Wette nach unten,
 * und die 15 Sekunden sind zu kurz zum Scrollen.
 */
const STANDINGS_OPEN_KEY = "watchparty.standingsOpen";

/**
 * `/join/CODE` füllt das Code-Feld vor (Anforderung 1-l) — eingegeben
 * werden muss dann nur noch der Name. Danach wird die URL bereinigt, damit
 * ein Neuladen der Seite nicht erneut denselben Code vorschlägt.
 */
function codeFromJoinLink() {
  const match = window.location.pathname.match(/^\/join\/([A-Za-z0-9]{4})$/);
  if (!match) return "";
  window.history.replaceState(null, "", "/");
  return match[1].toUpperCase();
}

function JoinScreen({ onJoin, status }) {
  const [name, setName] = useState(
    () => window.localStorage.getItem("watchparty.name") ?? ""
  );
  const [code, setCode] = useState(codeFromJoinLink);
  const trimmed = name.trim();
  const trimmedCode = code.trim();
  const isCreatingRoom = trimmedCode === "";
  // Mitspielen setzt einen echten Code voraus (Anforderung 1-h) -- ein
  // unvollstaendiger Code waere ohnehin ein Fehler beim Server, das soll der
  // Knopf schon vorher zeigen, statt erst nach dem Absenden.
  const codeIsValid = isCreatingRoom || /^[A-Z0-9]{4}$/.test(trimmedCode);
  const canSubmit = trimmed !== "" && codeIsValid;
  const buttonLabel = isCreatingRoom ? "Raum erstellen" : "Mitspielen";

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
          if (event.key === "Enter" && canSubmit) onJoin(trimmed, trimmedCode);
        }}
      />
      <input
        className="field"
        value={code}
        maxLength={4}
        placeholder="Code (optional — leer lässt eine neue Watchparty entstehen)"
        autoComplete="off"
        onChange={(event) => setCode(event.target.value.toUpperCase())}
        onKeyDown={(event) => {
          if (event.key === "Enter" && canSubmit) onJoin(trimmed, trimmedCode);
        }}
      />
      <button
        className="button primary"
        disabled={!canSubmit || status !== "online"}
        onClick={() => onJoin(trimmed, trimmedCode)}
      >
        {buttonLabel}
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

/**
 * Die Punkte der anderen, direkt unter dem eigenen Stand und ausklappbar.
 * Der zugeklappte Kopf trägt schon das Wichtigste — eigener Platz und wie
 * viele dabei sind —, damit man fürs Nachsehen nicht aufklappen muss.
 */
function Standings({ players, playerId }) {
  const [open, setOpen] = useState(
    () => window.localStorage.getItem(STANDINGS_OPEN_KEY) === "1"
  );
  const sorted = [...players].sort((a, b) => b.points - a.points);
  const rank = sorted.findIndex((player) => player.id === playerId) + 1;

  return (
    <details
      className="standings"
      open={open}
      onToggle={(event) => {
        const nowOpen = event.currentTarget.open;
        setOpen(nowOpen);
        window.localStorage.setItem(STANDINGS_OPEN_KEY, nowOpen ? "1" : "0");
      }}
    >
      <summary className="standings-head">
        <span className="eyebrow">Punktestand</span>
        <span className="standings-note">
          {rank > 0 && `Platz ${rank} von ${players.length}`}
          {rank <= 0 && `${players.length} dabei`}
        </span>
        <span className="chevron" aria-hidden="true">
          ▾
        </span>
      </summary>
      <Leaderboard players={players} playerId={playerId} />
    </details>
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

/**
 * Derselbe Countdown als ablaufender Rahmen um die Anwendung — sichtbar
 * auch für den, der gerade auf einen Ausgang zielt statt auf die Zahl.
 *
 * Die Dauer wird einmal beim Aufsetzen aus closesAt und dem Uhren-Offset
 * gebildet (ADR-003) und danach nicht mehr angefasst. Das ist wichtig:
 * STATE kommt bei *jedem* abgegebenen Tipp neu, eine an den Renderzyklus
 * gehängte Dauer würde die Animation dann jedes Mal verstellen. Der Rahmen
 * läuft dadurch immer genau bei closesAt aus, auch wenn die Seite mitten
 * im offenen Fenster neu geladen wurde.
 *
 * Vier Balken statt eines echten Rahmens, weil sie sich per transform
 * einziehen lassen — das bleibt auf dem Compositor und damit auch auf
 * älteren Handys flüssig.
 */
function CountdownFrame({ closesAt, serverNow }) {
  const [duration] = useState(() => Math.max(0, closesAt - serverNow()));
  if (duration <= 0) return null;
  const style = { animationDuration: `${duration}ms` };
  return (
    <div className="frame time-frame" aria-hidden="true">
      <span className="edge top" style={style} />
      <span className="edge right" style={style} />
      <span className="edge bottom" style={style} />
      <span className="edge left" style={style} />
    </div>
  );
}

function outcomeLabel(bet, outcomeId) {
  return bet?.outcomes.find((outcome) => outcome.id === outcomeId)?.label ?? outcomeId;
}

function PickForm({ bet, ownPoints, params, onPlacePick }) {
  const [outcomeId, setOutcomeId] = useState(null);
  // Als Text gehalten, damit sich das Feld auch mal ganz leeren lässt; der
  // Server klemmt den Einsatz ohnehin auf Mindesteinsatz und Kontostand.
  const [stake, setStake] = useState(() => String(Math.min(params.minStake, ownPoints)));
  const stakeNumber = Number(stake);

  /**
   * Beim Antippen steht der Mindesteinsatz schon im Feld — er soll sich
   * überschreiben lassen, ohne dass jemand erst löschen muss. Das
   * setTimeout ist für iOS: Dort hebt das nachlaufende Touch-Ereignis die
   * Selektion sonst gleich wieder auf.
   */
  const selectAll = (event) => {
    const field = event.target;
    field.select();
    window.setTimeout(() => field.select(), 0);
  };

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

      {ownPoints < params.minStake ? (
        <p className="hint">
          Du hast weniger als den Mindesteinsatz ({params.minStake}) — ein Tipp geht
          automatisch All-in mit deinen {ownPoints} Punkten.
        </p>
      ) : (
        <label className="stake">
          Einsatz
          <input
            className="field stake-field"
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            autoComplete="off"
            value={stake}
            onFocus={selectAll}
            onChange={(event) => setStake(event.target.value.replace(/[^0-9]/g, ""))}
          />
        </label>
      )}

      <button
        className="button primary"
        disabled={!outcomeId}
        onClick={() => onPlacePick(outcomeId, stakeNumber || params.minStake)}
      >
        Tipp abgeben
      </button>
    </div>
  );
}

/**
 * Die Aufdeckung ab CLOSED: erst die abgegebenen Tipps, dann abgesetzt die
 * Teilnehmer ohne Tipp (Anforderung 8.1-f/8.1-g).
 *
 * Die Strafe steht hier bewusst ohne Vorzeichen und mit dem Wort davor:
 * Sie ist noch nicht gebucht, verrechnet wird erst beim Auflösen (9-c) und
 * entfällt ganz, wenn der Host die Runde abbricht (8.6-a). Ein „−25" würde
 * das Gegenteil behaupten — die Zahl mit dem Minus steht erst im Ergebnis,
 * wo sie tatsächlich gilt.
 *
 * Die Kappung auf den Kontostand (8.1-c) rechnen wir hier schon vor statt
 * sie nur in Prosa anzukündigen: Der aktuelle Punktestand steht im STATE
 * (players[].points) und kann sich vor dem Auflösen dieser Runde nicht mehr
 * ändern — Punkte wandern ausschließlich beim Auflösen einer Runde, und es
 * läuft nie mehr als eine gleichzeitig (Invariante 1). Die angezeigte Zahl
 * ist also schon die, die beim Auflösen abgezogen wird.
 */
function RevealedPicks({ picks, nonPickers, players, bet, playerId, penalty }) {
  const playerById = (id) => players.find((player) => player.id === id);
  const nameOf = (id) => playerById(id)?.name ?? "?";
  if (picks.length === 0 && nonPickers.length === 0) {
    return <p className="hint">Niemand hat getippt.</p>;
  }
  return (
    <>
      <ul className="reveal">
        {picks.map((pick) => (
          <li key={pick.playerId} className={pick.playerId === playerId ? "self" : ""}>
            <span className="who">
              <span className="name">{nameOf(pick.playerId)}</span>
              <span className="sub">{outcomeLabel(bet, pick.outcomeId)}</span>
            </span>
            <span className="points">{pick.stake}</span>
          </li>
        ))}
        {nonPickers.map((id) => (
          <li key={id} className={`miss${id === playerId ? " self" : ""}`}>
            <span className="who">
              <span className="name">{nameOf(id)}</span>
              <span className="sub">Kein Tipp</span>
            </span>
            <span className="points negative">
              Strafe {Math.min(penalty, playerById(id)?.points ?? penalty)}
            </span>
          </li>
        ))}
      </ul>
      {nonPickers.length > 0 && (
        <p className="hint">
          Abgezogen wird die Strafe erst beim Auflösen. Bricht der Host die
          Runde ab, entfällt sie ganz.
        </p>
      )}
    </>
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

/**
 * Das Ergebnis. Jede Zeile nennt den Einsatz (9-d) — das Delta ist netto,
 * enthält ihn also schon; deshalb steht er untergeordnet neben dem Tipp und
 * nicht als zweite Zahl daneben.
 *
 * Die Zeilen entstehen aus Tipps *und* Nicht-Tippern, nicht aus den Deltas:
 * Wer bei 0 Punkten steht, zahlt nichts mehr (8.1-c) und taucht deshalb
 * nicht in den Deltas auf — er soll aber trotzdem in der Liste stehen.
 */
function ResultView({
  bet,
  winningOutcomeId,
  pool,
  annulled,
  annulReason,
  deltas,
  players,
  picks,
  nonPickers,
  playerId,
}) {
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
  const deltaOf = (id) => deltas?.[id] ?? 0;
  const rows = [
    ...picks.map((pick) => ({
      id: pick.playerId,
      sub: `${outcomeLabel(bet, pick.outcomeId)} · Einsatz ${pick.stake}`,
    })),
    ...nonPickers.map((id) => ({ id, sub: "Kein Tipp", missed: true })),
  ];
  return (
    <div className="result">
      <p className="eyebrow">Ergebnis</p>
      <p className="display">{outcomeLabel(bet, winningOutcomeId)}</p>
      <p className="hint">Pool: {pool} Punkte</p>
      <ul className="reveal">
        {rows.map((row) => {
          const delta = deltaOf(row.id);
          return (
            <li
              key={row.id}
              className={`${row.missed ? "miss" : ""}${row.id === playerId ? " self" : ""}`}
            >
              <span className="who">
                <span className="name">{nameOf(row.id)}</span>
                <span className="sub">{row.sub}</span>
              </span>
              <span className={delta >= 0 ? "positive" : "negative"}>
                {delta >= 0 ? `+${delta}` : delta}
              </span>
            </li>
          );
        })}
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

/**
 * Setzt den ganzen Raum zurueck: alle Spieler, Punktestaende und die
 * laufende Runde (Abschnitt 12 des Snapshot-Plans). Anders als
 * `Runde annullieren` keine Ausnahme fuer eine kaputte Runde, sondern das
 * Ende des Abends — deshalb eigene Rueckfrage und sichtbar abgesetzt von
 * den uebrigen Host-Knoepfen.
 */
function DangerZone({ onReset }) {
  return (
    <div className="danger-zone">
      <button
        className="button ghost wide"
        onClick={() => {
          if (
            window.confirm(
              "Den ganzen Raum zurücksetzen? Alle Spieler und Punktestände gehen verloren."
            )
          ) {
            onReset();
          }
        }}
      >
        Raum zurücksetzen
      </button>
    </div>
  );
}

export default function App() {
  const {
    status,
    state,
    playerId,
    roomCode,
    error,
    yourPick,
    catalog,
    params,
    join,
    openBet,
    closeBet,
    placePick,
    resolve,
    annul,
    reset,
    serverNow,
  } = useRoom();

  const [guideOpen, setGuideOpen] = useState(false);
  // params gehört dazu: Ohne die Werte aus dem WELCOME (3.1-c) kann die
  // Oberfläche weder Mindesteinsatz noch Strafe benennen.
  const joined = Boolean(playerId) && Boolean(state) && Boolean(params);

  // Screen Wake Lock, solange beigetreten (ADR-032) — beugt dem unbemerkten
  // Wandern der Host-Rolle vor (ADR-021), best effort ohne Fehlermeldung.
  useWakeLock(joined);

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
  const revealedPicks = state.revealedPicks ?? [];
  const nonPickers = state.nonPickers ?? [];

  // Gewonnen oder verloren entscheidet über die Animation beim Aufdecken
  // des Ergebnisses — aus dem eigenen Delta, nicht aus dem Ausgang: Wer
  // beim Push seinen Einsatz zurückbekommt, hat weder das eine noch das
  // andere getan.
  const ownDelta = state.deltas?.[playerId] ?? 0;
  const tone = state.annulled ? "" : ownDelta > 0 ? " win" : ownDelta < 0 ? " loss" : "";

  // 5-h: Der Server schickt keinen Grund fürs Schließen mit, weil es keinen
  // zweiten braucht — er schließt von selbst genau dann, wenn kein
  // Teilnehmer mehr ohne Tipp ist (5-g). Ist diese Liste in CLOSED leer,
  // war das der Auslöser; bei Zeitablauf oder Host-Klick steht immer
  // mindestens einer darin. Ohne einen einzigen Tipp ist es dagegen 8.4,
  // nicht vollzählige Beteiligung.
  const closedBecauseAllPicked =
    state.phase === "CLOSED" && nonPickers.length === 0 && revealedPicks.length > 0;

  return (
    <main className="shell">
      {state.phase === "OPEN" && state.closesAt && (
        <CountdownFrame
          key={state.roundId}
          closesAt={state.closesAt}
          serverNow={serverNow}
        />
      )}

      <header className="scorebug">
        <span className="brand">Watchparty</span>
        {/* Ständig sichtbar (Anforderung 1-k), damit er sich am Tisch schnell vorlesen lässt. */}
        {roomCode && <span className="tag room-code">{roomCode}</span>}
        {isHost && <span className="tag">Host</span>}
        <span className="bug-stat">
          <span className="bug-label">Punkte</span>
          <span className="bug-value">{ownPoints}</span>
        </span>
        <button className="button ghost" onClick={() => setGuideOpen(true)} aria-label="Anleitung">
          ?
        </button>
      </header>

      <Standings players={state.players} playerId={playerId} />

      {/* Alles ab hier ist die laufende Runde. Der key sorgt dafür, dass die
          Übergangsanimation genau einmal je Runde und Phase läuft — STATE
          kommt bei jedem abgegebenen Tipp neu. */}
      <div className="board" key={`${state.roundId}-${state.phase}`}>
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
              <PickForm
                bet={state.bet}
                ownPoints={ownPoints}
                params={params}
                onPlacePick={placePick}
              />
            )}
          </section>
        )}

        {state.phase === "CLOSED" && state.bet && (
          <section className={`stage${closedBecauseAllPicked ? " complete" : ""}`}>
            {closedBecauseAllPicked ? (
              <p className="all-picked">
                <span className="check" aria-hidden="true">
                  ✓
                </span>
                Alle haben getippt — Fenster zu
              </p>
            ) : (
              <p className="eyebrow">Geschlossen</p>
            )}
            <h2 className="display">{state.bet.question}</h2>
            <RevealedPicks
              picks={revealedPicks}
              nonPickers={nonPickers}
              players={state.players}
              bet={state.bet}
              playerId={playerId}
              penalty={params.penalty}
            />
            {isHost && <ResolveForm bet={state.bet} onResolve={resolve} />}
          </section>
        )}

        {state.phase === "RESOLVED" && state.bet && (
          <section className={`stage${tone}`}>
            <ResultView
              bet={state.bet}
              winningOutcomeId={state.winningOutcomeId}
              pool={state.pool}
              annulled={state.annulled}
              annulReason={state.annulReason}
              deltas={state.deltas}
              players={state.players}
              picks={revealedPicks}
              nonPickers={nonPickers}
              playerId={playerId}
            />
          </section>
        )}
      </div>

      {isHost && (
        <section className="host">
          <HostControls
            phase={state.phase}
            catalog={catalog}
            onOpenBet={openBet}
            onCloseBet={closeBet}
            onAnnul={annul}
          />
          <DangerZone onReset={reset} />
        </section>
      )}

      {guideOpen && (
        <Guide catalog={catalog} params={params} onClose={() => setGuideOpen(false)} />
      )}

      {error && <p className="error">{error}</p>}
      <footer className={`status ${status}`}>{STATUS_LABEL[status]}</footer>
    </main>
  );
}
