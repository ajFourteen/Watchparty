import { useEffect, useRef } from "react";

/**
 * Hält den Bildschirm wach, solange `active` gilt (ADR-032) — best effort,
 * ohne Fehler, wenn der Browser die Wake-Lock-API nicht kennt.
 */
export function useWakeLock(active) {
  const lockRef = useRef(null);

  useEffect(() => {
    if (!active || !("wakeLock" in navigator)) return;

    let cancelled = false;

    async function acquire() {
      try {
        const lock = await navigator.wakeLock.request("screen");
        if (cancelled) {
          lock.release();
          return;
        }
        lockRef.current = lock;
      } catch {
        // Kein Wake Lock (z. B. wenig Akku) — reiner Komfortverlust, kein
        // Fehlerzustand für den Spieler (ADR-032).
      }
    }

    // Die Spezifikation gibt den Lock beim Verstecken des Tabs automatisch
    // frei; beim Zurückkommen muss er erneut angefordert werden.
    function onVisibilityChange() {
      if (document.visibilityState === "visible") acquire();
    }

    acquire();
    document.addEventListener("visibilitychange", onVisibilityChange);

    return () => {
      cancelled = true;
      document.removeEventListener("visibilitychange", onVisibilityChange);
      lockRef.current?.release();
      lockRef.current = null;
    };
  }, [active]);
}
