/**
 * Gemeinsame Hülle für Impressum und Datenschutzerklärung — dieselbe
 * Overlay/Sheet-Optik wie die Kurzanleitung (Guide.jsx), aber als
 * eigenständige Seite statt als Overlay über dem Spiel: Beide Rechtstexte
 * sind über /impressum bzw. /datenschutz direkt erreichbar (WebConfig),
 * auch ohne dass zuvor die App geladen wurde.
 */
export function LegalPage({ title, children }) {
  return (
    <div className="overlay legal-page" role="document">
      <div className="sheet">
        <header className="sheet-head">
          <p className="eyebrow">Rechtliches</p>
          <a className="button ghost" href="/">
            Zurück
          </a>
        </header>
        <h2 className="display">{title}</h2>
        <div className="legal-body">{children}</div>
      </div>
    </div>
  );
}
