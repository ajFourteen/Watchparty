/**
 * Schmaler Zugriff auf die REST-Schnittstelle des Tippspiels (ADR-039) —
 * Anfrage/Antwort statt WebSocket, weil hier nichts in Sekunden geschieht.
 * `credentials: "include"` auf jeder Anfrage, damit das Sitzungscookie
 * mitgeschickt wird (dieselbe Notwendigkeit, egal ob Frontend und Backend
 * dieselbe Origin teilen wie im Jar oder per Vite-Proxy verbunden sind).
 */
const BASE = "/api/league";

class UnauthenticatedError extends Error {
  constructor() {
    super("nicht angemeldet");
    this.unauthenticated = true;
  }
}

async function request(path, options = {}) {
  const response = await fetch(BASE + path, {
    ...options,
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers ?? {}) },
  });

  if (response.status === 401) {
    throw new UnauthenticatedError();
  }
  if (!response.ok) {
    throw new Error(`Anfrage fehlgeschlagen (${response.status})`);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const leagueApi = {
  me: () => request("/me"),
  requestLink: (email, displayName) =>
    request("/login", { method: "POST", body: JSON.stringify({ email, displayName }) }),
  redeem: (token) => request(`/login/${encodeURIComponent(token)}`, { method: "POST" }),
  logout: () => request("/logout", { method: "POST" }),
  deleteAccount: () => request("/account", { method: "DELETE" }),
  optInReportMail: () => request("/report-mail/opt-in", { method: "POST" }),
  optOutReportMail: () => request("/report-mail/opt-out", { method: "POST" }),
  unsubscribeReportMail: (token) =>
    request(`/report-mail/unsubscribe/${encodeURIComponent(token)}`, { method: "POST" }),

  schedule: (year, week) => request(`/schedule/${year}/${week}`),
  submitPrediction: (gameId, home, away) =>
    request("/predictions", { method: "POST", body: JSON.stringify({ gameId, home, away }) }),
  totalPoints: () => request("/predictions/total-points"),
  matchdayReport: (year, week) => request(`/report/${year}/${week}`),

  myLeagues: () => request("/leagues"),
  createLeague: (name, seasonYear) =>
    request("/leagues", { method: "POST", body: JSON.stringify({ name, seasonYear }) }),
  joinLeague: (code) => request("/leagues/join", { method: "POST", body: JSON.stringify({ code }) }),
  leaveLeague: (leagueId) => request(`/leagues/${leagueId}/leave`, { method: "POST" }),
  leagueDetail: (leagueId) => request(`/leagues/${leagueId}`),
  seasonStandings: (leagueId) => request(`/leagues/${leagueId}/standings/season`),
  matchdayStandings: (leagueId, week) => request(`/leagues/${leagueId}/standings/matchday/${week}`),
  seasonStandingsThroughMatchday: (leagueId, week) => request(`/leagues/${leagueId}/standings/season/through/${week}`),
};

export { UnauthenticatedError };
