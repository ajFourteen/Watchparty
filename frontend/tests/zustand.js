/**
 * Serverdaten in der Form, in der sie tatsaechlich ueber die Leitung
 * kommen — die Eingangsseite der Frontend-Ebene.
 *
 * Die Feldnamen hier sind keine freie Erfindung: Der Gradle-Task
 * `protokollvertrag` gleicht jeden Feldnamen der Nachrichtentypen mit
 * diesem Verzeichnis ab und bricht den Bau, sobald eine Seite umbenennt.
 * Ein Tippfehler faellt damit im Bau auf und nicht erst am Spielabend.
 */

export function spieler(overrides = {}) {
  return {
    id: "p1",
    name: "Anna",
    points: 100,
    connected: true,
    host: false,
    paused: false,
    ...overrides,
  };
}

export const WETTE = {
  id: "drive-ausgang",
  question: "Wie endet der nächste Drive?",
  note: "Gezählt wird bis zum Ende des Drives.",
  outcomes: [
    { id: "TD", label: "Touchdown", note: "Auch nach Two-Point-Versuch." },
    { id: "FG", label: "Field Goal", note: null },
    { id: "PUNT", label: "Punt", note: null },
  ],
};

export const PARAMS = { startingPoints: 100, minStake: 10, penalty: 25 };

/** Ein STATE-Frame, wie ihn RoomView baut. */
export function zustand(overrides = {}) {
  return {
    phase: "IDLE",
    roundId: "r1",
    players: [spieler()],
    hostPlayerId: "p1",
    bet: null,
    closesAt: null,
    pickCount: 0,
    participantCount: 0,
    revealedPicks: null,
    nonPickers: null,
    winningOutcomeId: null,
    pool: null,
    deltas: null,
    annulled: false,
    annulReason: null,
    ...overrides,
  };
}

/** Alles, was useRoom der Oberflaeche liefert. */
export function raum(overrides = {}) {
  return {
    status: "online",
    state: zustand(),
    playerId: "p1",
    roomCode: "K7QM",
    error: null,
    yourPick: null,
    catalog: [WETTE],
    params: PARAMS,
    serverNow: () => Date.parse("2026-09-13T18:00:00Z"),
    join: () => {},
    openBet: () => {},
    closeBet: () => {},
    placePick: () => {},
    resolve: () => {},
    annul: () => {},
    reset: () => {},
    ...overrides,
  };
}
