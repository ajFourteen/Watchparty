import { useCallback, useEffect, useRef, useState } from "react";

const NAME_KEY = "watchparty.name";

/** Der Code der zuletzt betretenen Watchparty -- wird beim automatischen Reconnect wieder gebraucht. */
const CURRENT_ROOM_KEY = "watchparty.currentRoom";

/**
 * Ein Token je Watchparty (ADR-033): Wer zwei Watchpartys im selben Browser
 * besucht, soll dabei nicht das Token der einen mit dem der anderen
 * überschreiben -- sonst risse der Reconnect in der zuerst besuchten ab.
 */
function tokenKey(roomCode) {
  return `watchparty.token.${roomCode}`;
}

function tokenFor(roomCode) {
  return roomCode ? window.localStorage.getItem(tokenKey(roomCode)) : null;
}

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
  const [roomCode, setRoomCode] = useState(null);
  const [error, setError] = useState(null);
  const [yourPick, setYourPick] = useState(null);

  /** Der Wettkatalog kommt mit WELCOME und ändert sich über den Abend nicht. */
  const [catalog, setCatalog] = useState([]);

  /**
   * Startguthaben, Mindesteinsatz und Strafe (Anforderung 3.1-c) — aus
   * demselben Grund am WELCOME wie der Katalog. Bewusst ohne Vorbelegung:
   * Der Client soll die Werte nicht kennen, sondern gesagt bekommen. Bis
   * das WELCOME da ist, gilt niemand als beigetreten.
   */
  const [params, setParams] = useState(null);

  const socketRef = useRef(null);
  const retryRef = useRef(null);
  const closedByUs = useRef(false);
  const lastRoundIdRef = useRef(null);

  /** Spiegelt playerId, damit der onmessage-Handler nicht auf einem alten Wert haengt. */
  const playerIdRef = useRef(null);

  /**
   * Ob der gerade laufende JOIN-Versuch der stille, automatische beim
   * Verbindungsaufbau ist (rejoin()) statt ein Klick im Beitrittsformular.
   * Ein alter Watchparty-Code in localStorage kann inzwischen ungueltig
   * geworden sein (Aufraeum-Sweep, Server-Neustart) -- dessen Fehlermeldung
   * darf dann nicht wie ein fehlgeschlagener eigener Beitrittsversuch
   * aussehen, obwohl noch gar keiner stattgefunden hat.
   */
  const autoRejoinRef = useRef(false);

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
    const currentRoom = window.localStorage.getItem(CURRENT_ROOM_KEY);
    if (name && currentRoom) {
      autoRejoinRef.current = true;
      send({ type: "JOIN", name, token: tokenFor(currentRoom), roomCode: currentRoom });
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
          autoRejoinRef.current = false;
          window.localStorage.setItem(tokenKey(message.roomCode), message.token);
          window.localStorage.setItem(CURRENT_ROOM_KEY, message.roomCode);
          setRoomCode(message.roomCode);
          playerIdRef.current = message.playerId;
          setPlayerId(message.playerId);
          setCatalog(message.catalog ?? []);
          setParams(message.params ?? null);
          setError(null);
        } else if (message.type === "STATE") {
          // RESET (Host-Kommando, ADR-023) raeumt auch die Spieler weg. Die
          // Verbindung bleibt offen, deshalb muss der Client selbst merken,
          // dass er nicht mehr dabei ist -- daran, dass die eigene playerId
          // nicht mehr in der Liste steht -- und zurueck zur Beitrittsansicht.
          // Bewusst kein automatisches Wiederbeitreten, sonst waere RESET nur
          // Anzeige (Invariante 3: der Client rechnet nichts aus).
          if (
            playerIdRef.current &&
            !message.players.some((player) => player.id === playerIdRef.current)
          ) {
            const currentRoom = window.localStorage.getItem(CURRENT_ROOM_KEY);
            if (currentRoom) {
              window.localStorage.removeItem(tokenKey(currentRoom));
            }
            window.localStorage.removeItem(CURRENT_ROOM_KEY);
            window.localStorage.removeItem(NAME_KEY);
            playerIdRef.current = null;
            setPlayerId(null);
            setRoomCode(null);
            setState(null);
            setYourPick(null);
            lastRoundIdRef.current = null;
            return;
          }
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
          if (autoRejoinRef.current) {
            // Der stille Rejoin-Versuch ist an einem inzwischen ungueltigen
            // Code aus localStorage gescheitert (Aufraeum-Sweep,
            // Server-Neustart) -- kein eigener Beitrittsversuch, also auch
            // keine Fehlermeldung. Der veraltete Code wird verworfen, damit
            // der naechste Verbindungsversuch es nicht wieder probiert.
            autoRejoinRef.current = false;
            const staleRoom = window.localStorage.getItem(CURRENT_ROOM_KEY);
            if (staleRoom) {
              window.localStorage.removeItem(tokenKey(staleRoom));
            }
            window.localStorage.removeItem(CURRENT_ROOM_KEY);
            return;
          }
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

  /**
   * Erzeugen und Beitreten sind getrennte Kommandos (ADR-040): Ohne Code
   * entsteht eine neue Watchparty (`CREATE_ROOM`), mit einem Code tritt man
   * einer bestehenden bei (`JOIN`) -- dieselbe Unterscheidung, die die
   * Beschriftung des Knopfs im Beitrittsformular schon trifft. Der Client
   * normalisiert den Code hier nur so weit, dass er ein eigenes, frueher
   * gespeichertes Token fuer genau diesen Code wiederfindet; die
   * massgebliche Faltung verwechselbarer Zeichen (O/I/L) macht der Server.
   */
  const join = useCallback(
    (name, code) => {
      autoRejoinRef.current = false;
      window.localStorage.setItem(NAME_KEY, name);
      const normalizedCode = code ? code.trim().toUpperCase() : "";
      if (normalizedCode) {
        send({ type: "JOIN", name, token: tokenFor(normalizedCode), roomCode: normalizedCode });
      } else {
        send({ type: "CREATE_ROOM", name });
      }
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
  const reset = useCallback(() => send({ type: "RESET" }), [send]);

  /** Serverzeit jetzt, aus dem einmal gebildeten Offset interpoliert — nur Anzeige, der Server entscheidet (ADR-011). */
  const serverNow = useCallback(() => Date.now() + clockOffsetRef.current, []);

  return {
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
  };
}
