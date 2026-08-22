/**
 * Frontend-Ebene der Live-Wetten (docs/teststrategie.md, Abschnitt 2.6).
 *
 * Geprueft wird ausschliesslich die Projektion Serverdaten -> sichtbare
 * Ausgabe. Keine fachliche Regel wird hier ein zweites Mal entschieden:
 * Wer wie viele Punkte bekommt, hat Settlement entschieden; ob ein Tipp
 * verdeckt bleibt, entscheidet der Server (Invariante 4) und nicht diese
 * Oberflaeche. Hier steht nur, was ein Mensch am Handy sieht — und das
 * sind genau die Regeln, die in Anhang A die Marke `frontend` tragen und
 * bis 2026-08-21 auf keiner Ebene geprueft waren.
 */
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, vi } from "vitest";

import { anforderung } from "../anforderung.js";
import { PARAMS, WETTE, raum, spieler, zustand } from "../zustand.js";

let aktuellerRaum;
vi.mock("../../src/useRoom.js", () => ({ useRoom: () => aktuellerRaum }));

const { Watchparty } = await import("../../src/Watchparty.jsx");

function zeige(overrides = {}) {
  aktuellerRaum = raum(overrides);
  return render(<Watchparty />);
}

describe("Die Live-Wetten-Oberflaeche", () => {
  beforeEach(() => {
    aktuellerRaum = raum();
  });

  anforderung("1-k", "zeigt den Code der eigenen Watchparty staendig an", () => {
    zeige({ roomCode: "K7QM" });
    expect(screen.getByText("K7QM")).toBeInTheDocument();
  });

  anforderung("1-l", "fuellt das Code-Feld aus einem /join/CODE-Link vor", () => {
    window.history.replaceState(null, "", "/join/k7qm");
    // Vor dem Beitritt: kein playerId, also das Beitrittsformular.
    zeige({ playerId: null, state: null });

    expect(screen.getByPlaceholderText("Code (optional)")).toHaveValue("K7QM");
  });

  anforderung("3-d", "zeigt die Kontostaende aller Mitspieler im Leaderboard", async () => {
    zeige({
      state: zustand({
        players: [
          spieler({ id: "p1", name: "Anna", points: 120 }),
          spieler({ id: "p2", name: "Ben", points: 80 }),
        ],
      }),
    });

    await userEvent.click(screen.getByText("Punktestand"));
    const leaderboard = within(document.querySelector(".standings .roster"));
    expect(leaderboard.getByText("Anna")).toBeInTheDocument();
    expect(leaderboard.getByText("120")).toBeInTheDocument();
    expect(leaderboard.getByText("Ben")).toBeInTheDocument();
    expect(leaderboard.getByText("80")).toBeInTheDocument();
  });

  anforderung("4-f", "zeigt die Anmerkungen zur Wette und zu den Ausgaengen", () => {
    zeige({
      state: zustand({ phase: "OPEN", bet: WETTE, closesAt: null, participantCount: 1 }),
    });

    const runde = within(document.querySelector(".board"));
    expect(runde.getByText(WETTE.note)).toBeInTheDocument();
    expect(runde.getByText("Auch nach Two-Point-Versuch.")).toBeInTheDocument();
  });

  anforderung("6-d", "laesst den Einsatz vor dem Bestaetigen erhoehen", async () => {
    const abgegeben = [];
    zeige({
      state: zustand({ phase: "OPEN", bet: WETTE, participantCount: 1 }),
      placePick: (outcomeId, stake) => abgegeben.push({ outcomeId, stake }),
    });

    const runde = within(document.querySelector(".board"));
    const feld = runde.getByRole("textbox", { name: /Einsatz/ });
    expect(feld).toHaveValue(String(PARAMS.minStake));

    await userEvent.clear(feld);
    await userEvent.type(feld, "40");
    await userEvent.click(runde.getByText("Touchdown"));
    await userEvent.click(runde.getByRole("button", { name: "Tipp abgeben" }));

    expect(abgegeben).toEqual([{ outcomeId: "TD", stake: 40 }]);
  });

  anforderung("5-h", "benennt beim Schliessen, dass alle getippt haben", () => {
    zeige({
      state: zustand({
        phase: "CLOSED",
        bet: WETTE,
        players: [spieler({ id: "p1", name: "Anna" }), spieler({ id: "p2", name: "Ben" })],
        revealedPicks: [
          { playerId: "p1", outcomeId: "TD", stake: 10 },
          { playerId: "p2", outcomeId: "FG", stake: 20 },
        ],
        nonPickers: [],
      }),
    });

    expect(screen.getByText(/Alle haben getippt/)).toBeInTheDocument();
  });

  anforderung(
    "8.1-g",
    "hebt ab dem Schliessen hervor, wer nicht getippt hat, und nennt die Strafe",
    () => {
      zeige({
        state: zustand({
          phase: "CLOSED",
          bet: WETTE,
          players: [
            spieler({ id: "p1", name: "Anna", points: 100 }),
            spieler({ id: "p2", name: "Ben", points: 100 }),
          ],
          revealedPicks: [{ playerId: "p1", outcomeId: "TD", stake: 10 }],
          nonPickers: ["p2"],
        }),
      });

      const aufdeckung = within(document.querySelector(".reveal"));
      const zeileVonBen = aufdeckung.getByText("Ben").closest("li");
      expect(zeileVonBen).toHaveClass("miss");
      expect(zeileVonBen).toHaveTextContent("Kein Tipp");
      expect(zeileVonBen).toHaveTextContent(`Strafe ${PARAMS.penalty}`);
    }
  );

  anforderung(
    "8.1-g",
    "zeigt die auf den Kontostand gekappte Strafe, nicht die volle",
    () => {
      // 8.1-c: Wer weniger hat als die Strafe, zahlt hoechstens sein Konto.
      zeige({
        state: zustand({
          phase: "CLOSED",
          bet: WETTE,
          players: [
            spieler({ id: "p1", name: "Anna" }),
            spieler({ id: "p2", name: "Ben", points: 7 }),
          ],
          revealedPicks: [{ playerId: "p1", outcomeId: "TD", stake: 10 }],
          nonPickers: ["p2"],
        }),
      });

      const aufdeckung = within(document.querySelector(".reveal"));
      expect(aufdeckung.getByText("Ben").closest("li")).toHaveTextContent("Strafe 7");
    }
  );

  anforderung("9-d", "zeigt im Ergebnis je Tipp den Einsatz und hebt die eigene Zeile hervor", () => {
    zeige({
      playerId: "p1",
      state: zustand({
        phase: "RESOLVED",
        bet: WETTE,
        players: [
          spieler({ id: "p1", name: "Anna" }),
          spieler({ id: "p2", name: "Ben" }),
        ],
        revealedPicks: [
          { playerId: "p1", outcomeId: "TD", stake: 30 },
          { playerId: "p2", outcomeId: "FG", stake: 20 },
        ],
        nonPickers: [],
        winningOutcomeId: "TD",
        pool: 50,
        deltas: { p1: 20, p2: -20 },
      }),
    });

    const ergebnis = within(document.querySelector(".result .reveal"));
    const eigeneZeile = ergebnis.getByText("Anna").closest("li");
    expect(eigeneZeile).toHaveClass("self");
    expect(eigeneZeile).toHaveTextContent("Einsatz 30");
    expect(ergebnis.getByText("Ben").closest("li")).toHaveTextContent("Einsatz 20");
    expect(ergebnis.getByText("Ben").closest("li")).not.toHaveClass("self");
  });

  anforderung("10-c", "zeigt Spielern den Countdown und den Tippzaehler", () => {
    const closesAt = Date.parse("2026-09-13T18:00:30Z");
    zeige({
      playerId: "p2",
      state: zustand({
        phase: "OPEN",
        bet: WETTE,
        closesAt,
        players: [spieler({ id: "p1", host: true }), spieler({ id: "p2", name: "Ben" })],
        hostPlayerId: "p1",
        pickCount: 1,
        participantCount: 2,
      }),
    });

    expect(screen.getByText("1 von 2 haben getippt")).toBeInTheDocument();
    expect(screen.getByText("30")).toBeInTheDocument();
  });

  anforderung("10-c", "zeigt Spielern aufgedeckte Tipps und das Ergebnis", () => {
    zeige({
      playerId: "p2",
      state: zustand({
        phase: "RESOLVED",
        bet: WETTE,
        players: [spieler({ id: "p1", name: "Anna" }), spieler({ id: "p2", name: "Ben" })],
        hostPlayerId: "p1",
        revealedPicks: [
          { playerId: "p1", outcomeId: "TD", stake: 30 },
          { playerId: "p2", outcomeId: "TD", stake: 20 },
        ],
        nonPickers: [],
        winningOutcomeId: "TD",
        pool: 50,
        deltas: { p1: 0, p2: 0 },
      }),
    });

    const ergebnis = within(document.querySelector(".result"));
    expect(ergebnis.getByText("Ergebnis")).toBeInTheDocument();
    expect(ergebnis.getByText("Pool: 50 Punkte")).toBeInTheDocument();
    expect(ergebnis.getByText("Touchdown")).toBeInTheDocument();
  });

  anforderung("10.1-e", "kennzeichnet den Host mit einem eigenen Chip neben dem Namen", async () => {
    zeige({
      playerId: "p1",
      state: zustand({
        players: [
          spieler({ id: "p1", name: "Anna", host: true }),
          spieler({ id: "p2", name: "Ben" }),
        ],
        hostPlayerId: "p1",
      }),
    });

    await userEvent.click(screen.getByText("Punktestand"));
    const leaderboard = within(document.querySelector(".standings .roster"));
    const zeileVonAnna = leaderboard.getByText("Anna").closest("li");
    expect(zeileVonAnna.querySelector(".tag")).toHaveTextContent("Host");
    expect(leaderboard.getByText("Ben").closest("li").querySelector(".tag")).toBeNull();
  });
});
