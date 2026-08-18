import { useState } from "react";
import { Watchparty } from "./Watchparty.jsx";
import { League } from "./league/League.jsx";

const MODE_KEY = "watchparty.mode";

/**
 * Keiner der beiden Modi ist der Normalfall (anforderungen.md, Präambel
 * "Zwei Spielmodi") — der Einstieg entscheidet trotzdem sinnvoll vor, ohne
 * dass wer auf `/` landet, erst wählen muss (1-f: ein Link genügt): Wer über
 * `/league` oder `/league/login/...` kommt, meint das Tippspiel; sonst zählt
 * der zuletzt genutzte Modus, mit den Live-Wetten als Voreinstellung fürs
 * allererste Mal.
 */
function initialMode() {
  if (window.location.pathname.startsWith("/league")) return "league";
  return window.localStorage.getItem(MODE_KEY) ?? "watchparty";
}

export default function App() {
  const [mode, setMode] = useState(initialMode);

  const switchTo = (nextMode) => {
    setMode(nextMode);
    window.localStorage.setItem(MODE_KEY, nextMode);
  };

  return (
    <>
      <nav className="mode-switch">
        <button
          className={`mode-tab${mode === "watchparty" ? " active" : ""}`}
          onClick={() => switchTo("watchparty")}
        >
          Live-Wetten
        </button>
        <button
          className={`mode-tab${mode === "league" ? " active" : ""}`}
          onClick={() => switchTo("league")}
        >
          Tippspiel
        </button>
      </nav>
      {mode === "watchparty" ? <Watchparty /> : <League />}
    </>
  );
}
