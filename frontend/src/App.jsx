import { useState } from "react";
import { Watchparty } from "./Watchparty.jsx";
import { League } from "./league/League.jsx";
import { Impressum } from "./legal/Impressum.jsx";
import { Datenschutz } from "./legal/Datenschutz.jsx";

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

  // /impressum und /datenschutz sind eigenständige Seiten (WebConfig leitet
  // sie auf index.html weiter) statt Teil des Moduswechsels — Rechtstexte
  // gelten für die ganze Anwendung, nicht für einen der beiden Spielmodi.
  if (window.location.pathname === "/impressum") return <Impressum />;
  if (window.location.pathname === "/datenschutz") return <Datenschutz />;

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
      <footer className="legal-footer">
        <a href="/impressum">Impressum</a>
        <a href="/datenschutz">Datenschutz</a>
      </footer>
    </>
  );
}
