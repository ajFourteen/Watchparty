import { useCallback, useEffect, useRef, useState } from "react";

const TOKEN_KEY = "watchparty.token";
const NAME_KEY = "watchparty.name";

function socketUrl() {
  const scheme = window.location.protocol === "https:" ? "wss" : "ws";
  return `${scheme}://${window.location.host}/ws`;
}

/**
 * Hält die Verbindung zum Server und den zuletzt empfangenen Raumzustand.
 *
 * Der Server ist die einzige Quelle der Wahrheit (ADR-003): Dieser Hook
 * rechnet nichts aus, er spiegelt nur, was über die Leitung kommt. Bei einem
 * Reconnect wird der komplette Zustand neu geschickt, es gibt also keinen
 * lokalen Verlauf, den man zusammenführen müsste.
 */
export function useRoom() {
  const [status, setStatus] = useState("connecting");
  const [state, setState] = useState(null);
  const [playerId, setPlayerId] = useState(null);
  const [error, setError] = useState(null);
  const [yourPick, setYourPick] = useState(null);

  /** Der Wettkatalog kommt mit WELCOME und ändert sich über den Abend nicht. */
  const [catalog, setCatalog] = useState([]);

  const socketRef = useRef(null);
  const retryRef = useRef(null);
  const closedByUs = useRef(false);
  const lastRoundIdRef = useRef(null);

  /** serverNow minus Date.now(); einmal pro STATE gebildet, lokal interpoliert (ADR-003). */
  const clockOffsetRef = useRef(0);

  const send = useCallback((message) => {
    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    }
  }, []);

  const rejoin = useCallback(() => {
    const name = window.localStorage.getItem(NAME_KEY);
    const token = window.localStorage.getItem(TOKEN_KEY);
    if (name) {
      send({ type: "JOIN", name, token });
    }
  }, [send]);

  useEffect(() => {
    function connect() {
      const socket = new WebSocket(socketUrl());
      socketRef.current = socket;
      setStatus("connecting");

      socket.onopen = () => {
        setStatus("online");
        rejoin();
      };

      socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        if (message.type === "WELCOME") {
          window.localStorage.setItem(TOKEN_KEY, message.token);
          setPlayerId(message.playerId);
          setCatalog(message.catalog ?? []);
          setError(null);
        } else if (message.type === "STATE") {
          clockOffsetRef.current = message.serverNow - Date.now();
          // Eine neue Runde fängt bei OPEN frisch an -- der eigene Tipp der
          // vorherigen Runde gilt nicht mehr. Beim allerersten STATE (Join)
          // nicht löschen: YOUR_PICK für eine laufende Runde kommt vorher an.
          if (
            message.phase === "OPEN" &&
            lastRoundIdRef.current !== null &&
            message.roundId !== lastRoundIdRef.current
          ) {
            setYourPick(null);
          }
          lastRoundIdRef.current = message.roundId;
          setState(message);
        } else if (message.type === "YOUR_PICK") {
          setYourPick({ outcomeId: message.outcomeId, stake: message.stake });
        } else if (message.type === "ERROR") {
          setError(message.message);
        }
      };

      socket.onclose = () => {
        if (closedByUs.current) return;
        setStatus("offline");
        // Handys schlafen ein und Verbindungen brechen weg: einfach neu aufbauen.
        retryRef.current = window.setTimeout(connect, 1500);
      };
    }

    connect();
    return () => {
      closedByUs.current = true;
      window.clearTimeout(retryRef.current);
      socketRef.current?.close();
    };
  }, [rejoin]);

  const join = useCallback(
    (name) => {
      window.localStorage.setItem(NAME_KEY, name);
      send({ type: "JOIN", name, token: window.localStorage.getItem(TOKEN_KEY) });
    },
    [send]
  );

  const openBet = useCallback((betId) => send({ type: "OPEN_BET", betId }), [send]);
  const closeBet = useCallback(() => send({ type: "CLOSE_BET" }), [send]);
  const placePick = useCallback(
    (outcomeId, stake) => send({ type: "PLACE_PICK", outcomeId, stake }),
    [send]
  );
  const resolve = useCallback((outcomeId) => send({ type: "RESOLVE", outcomeId }), [send]);
  const annul = useCallback(() => send({ type: "ANNUL" }), [send]);

  /** Serverzeit jetzt, aus dem einmal gebildeten Offset interpoliert — nur Anzeige, der Server entscheidet (ADR-011). */
  const serverNow = useCallback(() => Date.now() + clockOffsetRef.current, []);

  return {
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
  };
}
