/**
 * 10.1-d, der Screen Wake Lock (ADR-032). Eigene Datei, weil hier nicht die
 * Darstellung geprueft wird, sondern eine Nebenwirkung am Browser — und
 * weil die API gestellt werden muss, die jsdom nicht mitbringt.
 *
 * Die Regel hat zwei Haelften, und die zweite ist die wichtigere: Der Lock
 * wird angefordert, *und* sein Fehlen bleibt folgenlos. "Best effort" heisst
 * genau das — ein Browser ohne die API oder ein leerer Akku darf keine
 * Fehlermeldung erzeugen und den Spieler nicht aus dem Spiel werfen.
 */
import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, vi } from "vitest";

import { anforderung } from "../anforderung.js";
import { raum } from "../zustand.js";

let aktuellerRaum;
vi.mock("../../src/useRoom.js", () => ({ useRoom: () => aktuellerRaum }));

const { Watchparty } = await import("../../src/Watchparty.jsx");

const urspruenglich = Object.getOwnPropertyDescriptor(navigator, "wakeLock");

afterEach(() => {
  if (urspruenglich) {
    Object.defineProperty(navigator, "wakeLock", urspruenglich);
  } else {
    delete navigator.wakeLock;
  }
});

describe("Der Bildschirm bleibt wach, solange jemand mitspielt", () => {
  beforeEach(() => {
    aktuellerRaum = raum();
  });

  anforderung("10.1-d", "fordert einen Wake Lock an, sobald ein Spieler beigetreten ist", async () => {
    const angefordert = [];
    Object.defineProperty(navigator, "wakeLock", {
      configurable: true,
      value: {
        request: async (typ) => {
          angefordert.push(typ);
          return { release: () => {} };
        },
      },
    });

    render(<Watchparty />);
    await vi.waitFor(() => expect(angefordert).toEqual(["screen"]));
  });

  anforderung("10.1-d", "fordert keinen Wake Lock an, solange niemand beigetreten ist", async () => {
    const angefordert = [];
    Object.defineProperty(navigator, "wakeLock", {
      configurable: true,
      value: {
        request: async (typ) => {
          angefordert.push(typ);
          return { release: () => {} };
        },
      },
    });

    aktuellerRaum = raum({ playerId: null, state: null });
    render(<Watchparty />);

    expect(screen.getByPlaceholderText("Dein Name")).toBeInTheDocument();
    expect(angefordert).toEqual([]);
  });

  anforderung("10.1-d", "spielt ohne Wake-Lock-API unveraendert weiter", () => {
    delete navigator.wakeLock;

    render(<Watchparty />);

    // Kein Fehlerhinweis, die Oberflaeche steht normal da: "best effort"
    // heisst, dass der fehlende Lock den Spieler nichts angeht.
    expect(document.querySelector(".error")).toBeNull();
    expect(screen.getByText("K7QM")).toBeInTheDocument();
  });

  anforderung("10.1-d", "spielt weiter, wenn der Browser den Wake Lock verweigert", async () => {
    Object.defineProperty(navigator, "wakeLock", {
      configurable: true,
      value: { request: async () => { throw new Error("wenig Akku"); } },
    });

    render(<Watchparty />);

    await vi.waitFor(() => expect(screen.getByText("K7QM")).toBeInTheDocument());
    expect(document.querySelector(".error")).toBeNull();
  });
});
