/**
 * Die Kurzanleitung des Tippspiels (13.10). Analog zu Guide.jsx bei den
 * Live-Wetten: ein Overlay, das aus dem Kopfbereich jederzeit erreichbar
 * ist, damit niemand am Tisch etwas erklären muss. Reine Textzusammenfassung
 * bereits geltender Regeln (13.2, 13.4, 13.5, 13.6, 13.9) — sie führt keine
 * eigene Regel ein, deshalb hier fest hinterlegt statt aus Serverdaten
 * aufgebaut wie der Wettkatalog bei den Live-Wetten.
 */
export function LeagueGuide({ onClose }) {
  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-label="Kurzanleitung">
      <div className="sheet">
        <header className="sheet-head">
          <p className="eyebrow">Kurzanleitung</p>
          <button className="button ghost" onClick={onClose} aria-label="Schließen">
            ✕
          </button>
        </header>

        <h2 className="display">Anmelden</h2>
        <p>
          Es gibt kein Kennwort. E-Mail-Adresse und Anzeigename genügen — du bekommst einen
          Anmeldelink zugeschickt, der dich mit einem Klick anmeldet. Existiert zu deiner
          Adresse noch kein Konto, entsteht es beim ersten Einlösen mit dem angegebenen Namen.
        </p>

        <h2 className="display">Tippen</h2>
        <ol className="steps">
          <li>
            <strong>Ein Tipp je Spiel.</strong> Du gibst zwei Zahlen ab — den erwarteten
            Endstand von Heim- und Gastmannschaft.
          </li>
          <li>
            <strong>Änderbar bis zum Anstoß.</strong> Ein neuer Tipp ersetzt den alten. Ab dem
            Anstoß des jeweiligen Spiels geht nichts mehr, weder ändern noch nachtragen.
          </li>
          <li>
            <strong>Fremde Tipps bleiben verdeckt.</strong> Was die anderen getippt haben,
            siehst du erst, sobald das jeweilige Spiel angepfiffen ist. Dein eigener Tipp ist
            dagegen immer sichtbar.
          </li>
        </ol>

        <h2 className="display">Wie Wertungspunkte entstehen</h2>
        <p>Es zählt die höchste Stufe, die du erreichst — nicht die Summe:</p>
        <ul className="facts">
          <li>
            <span className="fact-value">6</span>
            <span className="fact-label">Exaktes Ergebnis getroffen</span>
          </li>
          <li>
            <span className="fact-value">5</span>
            <span className="fact-label">Tendenz und Abstand richtig, aber nicht exakt</span>
          </li>
          <li>
            <span className="fact-value">3</span>
            <span className="fact-label">Nur die Tendenz richtig</span>
          </li>
          <li>
            <span className="fact-value">0</span>
            <span className="fact-label">
              Tendenz falsch — der Abstand zählt nur bei richtiger Tendenz überhaupt
            </span>
          </li>
        </ul>
        <p className="hint">
          Ein nicht abgegebener Tipp bringt 0 Wertungspunkte, aber keine Strafe — anders als
          bei den Live-Wetten gibt es im Tippspiel kein Aussitzen, das etwas kostet.
        </p>

        <h2 className="display">Ligen</h2>
        <p>
          Du kannst eine Liga anlegen oder einer bestehenden mit ihrem Beitrittscode
          beitreten — beliebig vielen gleichzeitig. Dein Tipp gehört dir und dem Spiel, nicht
          einer einzelnen Liga, und zählt deshalb in allen Ligen mit, denen du angehörst.
        </p>
        <p className="hint">
          Bei Punktgleichheit in der Rangliste entscheidet zuerst, wer mehr exakte Ergebnisse
          getroffen hat, danach, wer mehr richtige Tendenzen hat. Bleibt es dabei gleich,
          teilt ihr euch denselben Platz.
        </p>

        <h2 className="display">Spieltags-Report</h2>
        <p className="hint">
          Im Konto-Menü kannst du den Spieltags-Report per Mail bestellen und jederzeit
          wieder abbestellen — er kommt automatisch, sobald ein Spieltag vollständig
          ausgewertet ist.
        </p>

        <button className="button primary wide" onClick={onClose}>
          Alles klar
        </button>
      </div>
    </div>
  );
}
