import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach } from "vitest";

// Jeder Test beginnt auf einem leeren Gerät: kein gemerkter Name, keine
// gesehene Anleitung, kein aufgeklappter Punktestand. Sonst haengt das
// Ergebnis davon ab, welcher Test vorher lief.
beforeEach(() => {
  window.localStorage.clear();
  window.history.replaceState(null, "", "/");
  // Standardfall ist ein Geraet, das die Kurzanleitung schon gesehen hat.
  // Sonst geht das Overlay bei jedem Beitritt von selbst auf und legt sich
  // ueber die Ansicht, um die es im jeweiligen Test geht. Das Aufgehen
  // selbst ist eine eigene Regel und hat einen eigenen Test.
  window.localStorage.setItem("watchparty.guideSeen", "1");
});

afterEach(() => {
  cleanup();
});
