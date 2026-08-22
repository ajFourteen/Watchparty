/**
 * 13.10-a/b, die Kurzanleitung des Tippspiels (Feature 011).
 *
 * Zwei Regeln, die sich nur an der Oberflaeche entscheiden lassen: dass ein
 * Knopf im Kopfbereich sie jederzeit oeffnet, und dass sie bei der *ersten*
 * Anmeldung auf einem Geraet von selbst aufgeht, danach aber nicht mehr.
 * Die zweite Haelfte ist die, die man beim Nachbauen leicht verliert — sie
 * haengt an einem localStorage-Schluessel und faellt sonst erst dem
 * zweiten Nutzer auf.
 */
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, vi } from "vitest";

import { anforderung } from "../anforderung.js";

const konto = {
  status: "authenticated",
  account: { email: "anna@example.org", displayName: "Anna", reportMailOptIn: false },
  error: null,
  requestLink: vi.fn(),
  logout: vi.fn(),
  deleteAccount: vi.fn(),
  confirmLogin: vi.fn(),
  continueAfterUnsubscribe: vi.fn(),
  optInReportMail: vi.fn(),
  optOutReportMail: vi.fn(),
};

vi.mock("../../src/league/useLeagueAccount.js", () => ({ useLeagueAccount: () => konto }));
vi.mock("../../src/league/api.js", () => ({
  leagueApi: {
    schedule: vi.fn().mockResolvedValue({ week: 1, games: [] }),
    myLeagues: vi.fn().mockResolvedValue([]),
    matchdayReport: vi.fn().mockResolvedValue({ week: 1, totalPoints: 0, games: [] }),
    matchdayStandings: vi.fn().mockResolvedValue([]),
    seasonStandingsThroughMatchday: vi.fn().mockResolvedValue([]),
    totalPoints: vi.fn().mockResolvedValue({ totalPoints: 0 }),
  },
  UnauthenticatedError: class extends Error {},
}));

const { League } = await import("../../src/league/League.jsx");

const GUIDE_SEEN_KEY = "watchparty.league.guideSeen";

describe("Die Kurzanleitung des Tippspiels", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  anforderung("13.10-b", "geht bei der ersten Anmeldung auf diesem Geraet von selbst auf", async () => {
    render(<League />);

    expect(await screen.findByText("Kurzanleitung")).toBeInTheDocument();
    // Sie erklaert alle fuenf Abschnitte, nicht nur einen (13.10-a).
    const overlay = within(screen.getByText("Kurzanleitung").closest("div"));
    for (const abschnitt of ["Anmelden", "Tippen", "Ligen", "Spieltags-Report"]) {
      expect(overlay.getByText(abschnitt)).toBeInTheDocument();
    }
  });

  anforderung("13.10-b", "bleibt bei jeder weiteren Anmeldung zu", () => {
    window.localStorage.setItem(GUIDE_SEEN_KEY, "1");

    render(<League />);

    expect(screen.queryByText("Kurzanleitung")).toBeNull();
  });

  anforderung("13.10-a", "laesst sich jederzeit ueber den Knopf im Kopfbereich oeffnen", async () => {
    window.localStorage.setItem(GUIDE_SEEN_KEY, "1");
    render(<League />);
    expect(screen.queryByText("Kurzanleitung")).toBeNull();

    await userEvent.click(screen.getByRole("button", { name: "Anleitung" }));

    expect(screen.getByText("Kurzanleitung")).toBeInTheDocument();
  });
});
