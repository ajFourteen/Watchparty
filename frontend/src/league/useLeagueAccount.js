import { useCallback, useEffect, useState } from "react";
import { leagueApi } from "./api.js";

/**
 * `/league/login/TOKEN` kommt aus der Mail (Kriterium 1) — der Token wird
 * einmalig eingelöst, danach wird die URL bereinigt, damit ein Neuladen
 * nicht denselben (dann schon verbrauchten) Link erneut versucht.
 */
function redeemTokenFromLocation() {
  const match = window.location.pathname.match(/^\/league\/login\/([^/]+)$/);
  if (!match) return null;
  window.history.replaceState(null, "", "/league");
  return decodeURIComponent(match[1]);
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

  const refresh = useCallback(async () => {
    try {
      const me = await leagueApi.me();
      setAccount(me);
      setStatus("authenticated");
    } catch (e) {
      setAccount(null);
      setStatus("anonymous");
      if (!e.unauthenticated) setError(e.message);
    }
  }, []);

  useEffect(() => {
    const token = redeemTokenFromLocation();
    if (!token) {
      refresh();
      return;
    }
    leagueApi
      .redeem(token)
      .then(refresh)
      .catch(() => {
        setStatus("anonymous");
        setError("Der Anmeldelink ist ungültig oder schon verbraucht.");
      });
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

  return { status, account, error, requestLink, logout, deleteAccount };
}
