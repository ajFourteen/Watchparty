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
function tokenFromLocation() {
  const match = window.location.pathname.match(/^\/league\/login\/([^/]+)$/);
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
  const [pendingToken, setPendingToken] = useState(() => tokenFromLocation());

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
    if (pendingToken) {
      setStatus("pendingLogin");
      return;
    }
    refresh();
  }, [pendingToken, refresh]);

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

  return { status, account, error, requestLink, logout, deleteAccount, confirmLogin };
}
