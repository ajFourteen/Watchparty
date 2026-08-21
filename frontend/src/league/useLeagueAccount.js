import { useCallback, useEffect, useState } from "react";
import { leagueApi } from "./api.js";

/**
 * `/league/login/TOKEN` kommt aus der Mail (Kriterium 1). Der Token wird
 * bewusst NICHT schon beim bloßen Laden dieser Seite eingelöst: Mail-
 * Programme öffnen solche Links oft in einem eingebauten Vorschau-Browser
 * (oder rendern ihn für eine Link-Vorschau) und führen dabei das Skript der
 * Seite mit aus — ohne einen expliziten Klick würde das den einmal
 * verwendbaren Link schon verbrauchen, bevor er im eigentlichen Browser
 * geöffnet werden kann. Deshalb bleibt der Token bis zum Klick auf
 * "Jetzt anmelden" unangetastet und auch in der URL stehen, damit sich der
 * Link notfalls noch kopieren und anderswo öffnen lässt.
 */
function loginTokenFromLocation() {
  const match = window.location.pathname.match(/^\/league\/login\/([^/]+)$/);
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * `/league/report-mail/unsubscribe/TOKEN` kommt aus der Report-Mail
 * (13.9-p, ADR-041). Anders als der Anmeldelink wirkt dieser Link sofort
 * beim Laden, ohne extra Klick: Er ist beliebig oft wirksam und ohne
 * Anmeldung gültig (ADR-041, "sofort wirkt") -- verbraucht ihn ein
 * Vorschau-Browser eines Mail-Programms vorzeitig, ist das folgenlos, ein
 * erneutes Bestellen im Konto-Menü macht es rückgängig.
 */
function unsubscribeTokenFromLocation() {
  const match = window.location.pathname.match(/^\/league\/report-mail\/unsubscribe\/([^/]+)$/);
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Hält den Anmeldestatus des Tippspiel-Kontos. Der Server ist die einzige
 * Quelle der Wahrheit: Angemeldet ist, wessen Sitzungscookie der Server
 * gerade als gültig ansieht (`GET /me`), nicht was zuletzt lokal gespeichert
 * wurde.
 */
export function useLeagueAccount() {
  const [status, setStatus] = useState("loading");
  const [account, setAccount] = useState(null);
  const [error, setError] = useState(null);
  const [pendingToken, setPendingToken] = useState(() => loginTokenFromLocation());
  const [unsubscribeToken] = useState(() => unsubscribeTokenFromLocation());

  const refresh = useCallback(async () => {
    try {
      const [me, points] = await Promise.all([leagueApi.me(), leagueApi.totalPoints()]);
      setAccount({ ...me, totalPoints: points.totalPoints });
      setStatus("authenticated");
    } catch (e) {
      setAccount(null);
      setStatus("anonymous");
      if (!e.unauthenticated) setError(e.message);
    }
  }, []);

  useEffect(() => {
    if (unsubscribeToken) {
      leagueApi.unsubscribeReportMail(unsubscribeToken).finally(() => {
        window.history.replaceState(null, "", "/league");
        setStatus("unsubscribed");
      });
      return;
    }
    if (pendingToken) {
      setStatus("pendingLogin");
      return;
    }
    refresh();
  }, [pendingToken, unsubscribeToken, refresh]);

  const confirmLogin = useCallback(async () => {
    if (!pendingToken) return;
    try {
      await leagueApi.redeem(pendingToken);
      window.history.replaceState(null, "", "/league");
      setPendingToken(null);
      await refresh();
    } catch {
      window.history.replaceState(null, "", "/league");
      setPendingToken(null);
      setStatus("anonymous");
      setError("Der Anmeldelink ist ungültig oder schon verbraucht.");
    }
  }, [pendingToken, refresh]);

  const continueAfterUnsubscribe = useCallback(async () => {
    await refresh();
  }, [refresh]);

  const requestLink = useCallback(async (email, displayName) => {
    setError(null);
    await leagueApi.requestLink(email, displayName);
  }, []);

  const logout = useCallback(async () => {
    await leagueApi.logout();
    setAccount(null);
    setStatus("anonymous");
  }, []);

  const deleteAccount = useCallback(async () => {
    await leagueApi.deleteAccount();
    setAccount(null);
    setStatus("anonymous");
  }, []);

  const optInReportMail = useCallback(async () => {
    await leagueApi.optInReportMail();
    setAccount((current) => (current ? { ...current, reportMailOptIn: true } : current));
  }, []);

  const optOutReportMail = useCallback(async () => {
    await leagueApi.optOutReportMail();
    setAccount((current) => (current ? { ...current, reportMailOptIn: false } : current));
  }, []);

  return {
    status,
    account,
    error,
    requestLink,
    logout,
    deleteAccount,
    confirmLogin,
    continueAfterUnsubscribe,
    optInReportMail,
    optOutReportMail,
  };
}
