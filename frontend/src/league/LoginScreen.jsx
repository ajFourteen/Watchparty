import { useState } from "react";

/**
 * Die Antwort auf eine Anmeldeanfrage ist immer dieselbe (Kriterium 3) —
 * die Oberfläche darf deshalb gar nicht erst versuchen zu erraten, ob die
 * Adresse bekannt war. Nach dem Absenden gibt es nur einen einzigen
 * Folgezustand, unabhängig vom tatsächlichen Ausgang.
 */
export function LoginScreen({ onRequestLink, error }) {
  const [email, setEmail] = useState(
    () => window.localStorage.getItem("watchparty.league.email") ?? ""
  );
  const [displayName, setDisplayName] = useState(
    () => window.localStorage.getItem("watchparty.league.name") ?? ""
  );
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);

  const canSubmit = email.trim() !== "" && displayName.trim() !== "" && !sending;

  const submit = async () => {
    if (!canSubmit) return;
    setSending(true);
    window.localStorage.setItem("watchparty.league.email", email.trim());
    window.localStorage.setItem("watchparty.league.name", displayName.trim());
    try {
      await onRequestLink(email.trim(), displayName.trim());
      setSent(true);
    } finally {
      setSending(false);
    }
  };

  if (sent) {
    return (
      <div className="league-card">
        <p className="eyebrow">Tippspiel</p>
        <h1 className="display">Fast geschafft</h1>
        <p className="hint">
          Falls die Adresse mitspielt oder gerade neu dazukommt: gleich kommt eine
          Mail mit einem Anmeldelink. Der Link gilt 15 Minuten und einmal.
        </p>
        <button className="button ghost wide" onClick={() => setSent(false)}>
          Andere Adresse
        </button>
      </div>
    );
  }

  return (
    <div className="league-card">
      <p className="eyebrow">Tippspiel</p>
      <h1 className="display">Anmelden</h1>
      <p className="hint">
        Ein Konto entsteht beim ersten Anmelden von selbst — einfach E-Mail-Adresse
        und Anzeigename angeben.
      </p>
      <input
        className="field"
        type="email"
        value={email}
        placeholder="E-Mail-Adresse"
        autoComplete="email"
        onChange={(event) => setEmail(event.target.value)}
      />
      <input
        className="field"
        value={displayName}
        maxLength={20}
        placeholder="Anzeigename"
        autoComplete="nickname"
        onChange={(event) => setDisplayName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter" && canSubmit) submit();
        }}
      />
      <button className="button primary" disabled={!canSubmit} onClick={submit}>
        Anmeldelink schicken
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
