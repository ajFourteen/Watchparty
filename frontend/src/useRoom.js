import { useCallback, useEffect, useRef, useState } from "react";

const TOKEN_KEY = "watchparty.token";
const NAME_KEY = "watchparty.name";

function socketUrl() {
  const scheme = window.location.protocol === "https:" ? "wss" : "ws";
  return `${scheme}://${window.location.host}/ws`;
}

/**
 * Haelt die Verbindung zum Server und den zuletzt empfangenen Raumzustand.
 *
 * Der Server ist die einzige Quelle der Wahrheit (ADR-003): Dieser Hook
 * rechnet nichts aus, er spiegelt nur, was ueber die Leitung kommt. Bei einem
 * Reconnect wird der komplette Zustand neu geschickt, es gibt also keinen
 * lokalen Verlauf, den man zusammenfuehren muesste.
 */
export function useRoom() {
  const [status, setStatus] = useState("connecting");
  const [state, setState] = useState(null);
  const [playerId, setPlayerId] = useState(null);
  const [error, setError] = useState(null);

  const socketRef = useRef(null);
  const retryRef = useRef(null);
  const closedByUs = useRef(false);

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
          setError(null);
        } else if (message.type === "STATE") {
          setState(message);
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

  const hostAction = useCallback(() => send({ type: "HOST_ACTION" }), [send]);

  return { status, state, playerId, error, join, hostAction };
}
