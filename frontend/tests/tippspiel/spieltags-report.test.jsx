/**
 * Frontend-Ebene des Spieltags-Reports (docs/teststrategie.md,
 * Abschnitt 2.6). Kapitel 13.9-f bis 13.9-m sind in Anhang A `frontend`
 * markiert, weil der Server die Zahlen liefert und die Oberflaeche daraus
 * die Hoehepunkte bildet. Genau diese Bildung wird hier geprueft — es ist
 * die groesste Menge echter Logik im Frontend dieses Projekts und war bis
 * 2026-08-21 auf keiner Ebene abgedeckt.
 */
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, vi } from "vitest";

import { anforderung } from "../anforderung.js";

const api = {
  matchdayReport: vi.fn(),
  myLeagues: vi.fn(),
  matchdayStandings: vi.fn(),
  seasonStandingsThroughMatchday: vi.fn(),
};
vi.mock("../../src/league/api.js", () => ({ leagueApi: api }));

const { ReportScreen } = await import("../../src/league/ReportScreen.jsx");

function spiel(overrides = {}) {
  return {
    gameId: "g1",
    homeTeamName: "Green Bay Packers",
    awayTeamName: "Chicago Bears",
    finalScore: { home: 24, away: 17 },
    ownPrediction: { home: 24, away: 17 },
    points: 6,
    ...overrides,
  };
}

function ranglistenEintrag(overrides = {}) {
  return {
    displayName: "Anna",
    totalPoints: 12,
    exactCount: 0,
    correctTendencyCount: 2,
    rank: 1,
    isSelf: false,
    ...overrides,
  };
}

const LIGA = { id: "liga-1", name: "Büro-Liga", code: "AB12CD", seasonYear: 2026, isManager: true };

/** Baut den Report auf und wartet, bis alle vier Abfragen durch sind. */
async function zeigeReport({ report, ligen = [], spieltagsrangliste = [], saisonranglisten = {} }) {
  api.matchdayReport.mockResolvedValue(report);
  api.myLeagues.mockResolvedValue(ligen);
  api.matchdayStandings.mockResolvedValue(spieltagsrangliste);
  api.seasonStandingsThroughMatchday.mockImplementation((_id, woche) =>
    Promise.resolve(saisonranglisten[woche] ?? []),
  );

  render(<ReportScreen />);
  await screen.findByText(/Spieltagssumme/);
}

describe("Der Spieltags-Report", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  anforderung("13.9-f", "zeigt die Spieltagsrangliste einer Liga des Tippers", async () => {
    await zeigeReport({
      report: { week: 1, totalPoints: 6, games: [spiel()] },
      ligen: [LIGA],
      spieltagsrangliste: [
        ranglistenEintrag({ displayName: "Anna", rank: 1, totalPoints: 6, isSelf: true }),
        ranglistenEintrag({ displayName: "Ben", rank: 2, totalPoints: 3 }),
      ],
    });

    const ueberschrift = await screen.findByText(/Liga-Rangliste/);
    const block = within(ueberschrift.closest(".report-league-standings"));
    expect(block.getByText("Büro-Liga")).toBeInTheDocument();
    // "Liga-Rangliste" erscheint schon, sobald myLeagues() durch ist; die
    // Rangliste selbst haengt an einem weiteren, spaeteren Effekt
    // (matchdayStandings ueber selectedLeagueId). Ohne findByText hier lief
    // die Pruefung unter CI-Last gelegentlich vor diesem zweiten Tick.
    expect(await block.findByText("Ben")).toBeInTheDocument();
  });

  anforderung("13.9-g", "laesst bei mehreren Ligen waehlen, welche Rangliste erscheint", async () => {
    const zweite = { ...LIGA, id: "liga-2", name: "Familien-Liga" };
    await zeigeReport({
      report: { week: 1, totalPoints: 6, games: [spiel()] },
      ligen: [LIGA, zweite],
      spieltagsrangliste: [ranglistenEintrag({ isSelf: true })],
    });

    const auswahl = await screen.findByRole("combobox");
    expect(within(auswahl).getByText("Büro-Liga")).toBeInTheDocument();
    expect(within(auswahl).getByText("Familien-Liga")).toBeInTheDocument();

    await userEvent.selectOptions(auswahl, "liga-2");
    await vi.waitFor(() =>
      expect(api.matchdayStandings).toHaveBeenCalledWith("liga-2", 1),
    );
  });

  anforderung("13.9-h", "bleibt ohne Liga ohne Rangliste und ohne Fehlermeldung", async () => {
    await zeigeReport({
      report: { week: 1, totalPoints: 6, games: [spiel()] },
      ligen: [],
    });

    expect(screen.queryByText(/Liga-Rangliste/)).toBeNull();
    expect(document.querySelector(".error")).toBeNull();
    // Die eigene Bilanz steht trotzdem da — ohne Liga fehlt nur der Zusatz.
    expect(screen.getByText(/Spieltagssumme/)).toBeInTheDocument();
  });

  anforderung("13.9-i", "zeigt einen Aufstieg gegenueber der Vorwoche", async () => {
    await zeigeReport({
      report: { week: 2, totalPoints: 6, games: [spiel()] },
      ligen: [LIGA],
      spieltagsrangliste: [ranglistenEintrag({ isSelf: true })],
      saisonranglisten: {
        1: [ranglistenEintrag({ rank: 5, isSelf: true })],
        2: [ranglistenEintrag({ rank: 2, isSelf: true })],
      },
    });

    await userEvent.click(screen.getByRole("button", { name: "›" }));
    expect(await screen.findByText(/Von Platz 5 auf Platz 2 gestiegen/)).toBeInTheDocument();
  });

  anforderung("13.9-i", "zeigt einen Abstieg gegenueber der Vorwoche", async () => {
    await zeigeReport({
      report: { week: 2, totalPoints: 0, games: [spiel({ points: 0 })] },
      ligen: [LIGA],
      spieltagsrangliste: [ranglistenEintrag({ isSelf: true })],
      saisonranglisten: {
        1: [ranglistenEintrag({ rank: 2, isSelf: true })],
        2: [ranglistenEintrag({ rank: 6, isSelf: true })],
      },
    });

    await userEvent.click(screen.getByRole("button", { name: "›" }));
    expect(await screen.findByText(/Von Platz 2 auf Platz 6 gefallen/)).toBeInTheDocument();
  });

  anforderung("13.9-i", "benennt auch den unveraenderten Platz ausdruecklich", async () => {
    await zeigeReport({
      report: { week: 2, totalPoints: 3, games: [spiel({ points: 3 })] },
      ligen: [LIGA],
      spieltagsrangliste: [ranglistenEintrag({ isSelf: true })],
      saisonranglisten: {
        1: [ranglistenEintrag({ rank: 3, isSelf: true })],
        2: [ranglistenEintrag({ rank: 3, isSelf: true })],
      },
    });

    await userEvent.click(screen.getByRole("button", { name: "›" }));
    expect(await screen.findByText(/Platz 3, unverändert gegenüber der Vorwoche/)).toBeInTheDocument();
  });

  anforderung("13.9-j", "zeigt am ersten Spieltag keine Platzveraenderung", async () => {
    await zeigeReport({
      report: { week: 1, totalPoints: 6, games: [spiel()] },
      ligen: [LIGA],
      spieltagsrangliste: [ranglistenEintrag({ isSelf: true })],
      saisonranglisten: { 1: [ranglistenEintrag({ rank: 1, isSelf: true })] },
    });

    await screen.findByText(/Liga-Rangliste/);
    expect(screen.queryByText(/gestiegen|gefallen|unverändert/)).toBeNull();
    // Es wird gar nicht erst gefragt: ohne Vorwoche gibt es nichts zu vergleichen.
    expect(api.seasonStandingsThroughMatchday).not.toHaveBeenCalled();
  });

  anforderung("13.9-k", "nennt den Spieltagssieger der eingeblendeten Liga", async () => {
    await zeigeReport({
      report: { week: 1, totalPoints: 3, games: [spiel({ points: 3 })] },
      ligen: [LIGA],
      spieltagsrangliste: [
        ranglistenEintrag({ displayName: "Ben", rank: 1, totalPoints: 6 }),
        ranglistenEintrag({ displayName: "Anna", rank: 2, totalPoints: 3, isSelf: true }),
      ],
    });

    expect(await screen.findByText(/Spieltagssieger: Ben/)).toBeInTheDocument();
  });

  anforderung("13.9-k", "nennt bei geteiltem Rang 1 alle Spieltagssieger", async () => {
    await zeigeReport({
      report: { week: 1, totalPoints: 6, games: [spiel()] },
      ligen: [LIGA],
      spieltagsrangliste: [
        ranglistenEintrag({ displayName: "Anna", rank: 1, isSelf: true }),
        ranglistenEintrag({ displayName: "Ben", rank: 1 }),
        ranglistenEintrag({ displayName: "Cem", rank: 3 }),
      ],
    });

    const zeile = await screen.findByText(/Spieltagssieger:/);
    expect(zeile).toHaveTextContent("Spieltagssieger: Anna, Ben");
    expect(zeile).not.toHaveTextContent("Cem");
  });

  anforderung("13.9-l", "nennt die Mitglieder mit mindestens einem Volltreffer", async () => {
    await zeigeReport({
      report: { week: 1, totalPoints: 6, games: [spiel()] },
      ligen: [LIGA],
      spieltagsrangliste: [
        ranglistenEintrag({ displayName: "Anna", rank: 1, exactCount: 2, isSelf: true }),
        ranglistenEintrag({ displayName: "Ben", rank: 2, exactCount: 0 }),
        ranglistenEintrag({ displayName: "Cem", rank: 3, exactCount: 1 }),
      ],
    });

    const zeile = await screen.findByText(/Volltreffer:/);
    expect(zeile).toHaveTextContent("Volltreffer: Anna, Cem");
    expect(zeile).not.toHaveTextContent("Ben");
  });

  anforderung("13.9-m", "nennt das Spiel mit dem groessten Punktabstand als groesste Ueberraschung", async () => {
    await zeigeReport({
      report: {
        week: 1,
        totalPoints: 6,
        games: [
          spiel({ gameId: "g1", finalScore: { home: 24, away: 17 } }),
          spiel({
            gameId: "g2",
            homeTeamName: "Kansas City Chiefs",
            awayTeamName: "Denver Broncos",
            finalScore: { home: 45, away: 3 },
          }),
        ],
      },
    });

    const zeile = screen.getByText(/Größte Überraschung:/);
    expect(zeile).toHaveTextContent("Kansas City Chiefs 45:3 Denver Broncos");
    expect(zeile).not.toHaveTextContent("24:17");
  });

  anforderung("13.9-m", "nennt bei gleichem Abstand alle betroffenen Spiele", async () => {
    await zeigeReport({
      report: {
        week: 1,
        totalPoints: 6,
        games: [
          spiel({ gameId: "g1", finalScore: { home: 21, away: 7 } }),
          spiel({
            gameId: "g2",
            homeTeamName: "Kansas City Chiefs",
            awayTeamName: "Denver Broncos",
            finalScore: { home: 10, away: 24 },
          }),
        ],
      },
    });

    const zeile = screen.getByText(/Größte Überraschung:/);
    expect(zeile).toHaveTextContent("Green Bay Packers 21:7 Chicago Bears");
    expect(zeile).toHaveTextContent("Kansas City Chiefs 10:24 Denver Broncos");
  });
});
