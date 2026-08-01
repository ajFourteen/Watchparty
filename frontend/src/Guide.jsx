/**
 * Die Kurzanleitung. Liegt als Overlay über dem Spiel, weil sie am
 * Spielabend auf jedem Handy erreichbar sein muss, ohne dass der Host etwas
 * verteilt oder erklärt.
 *
 * Der Wettkatalog wird nicht abgeschrieben, sondern aus dem gerenderten
 * Katalog aufgebaut: Die Ausgänge und ihre Abgrenzungen (Anforderung 4.1,
 * Big-Play-Schwellen) stehen einmal auf dem Server, damit hier nichts
 * veralten kann, wenn eine Wette dazukommt.
 */
export function Guide({ catalog, onClose }) {
  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-label="Kurzanleitung">
      <div className="sheet">
        <header className="sheet-head">
          <p className="eyebrow">Kurzanleitung</p>
          <button className="button ghost" onClick={onClose} aria-label="Schließen">
            ✕
          </button>
        </header>

        <h2 className="display">So läuft's</h2>
        <ol className="steps">
          <li>
            <strong>Der Host öffnet eine Wette.</strong> Dann läuft eine Uhr:{" "}
            <strong>15 Sekunden</strong>, um zu tippen.
          </li>
          <li>
            <strong>Du tippst verdeckt.</strong> Solange die Uhr läuft, sieht niemand — auch
            der Server verrät es nicht —, worauf du gesetzt hast. Sichtbar ist nur, wie viele
            schon getippt haben.
          </li>
          <li>
            <strong>Die Wette schließt</strong> und alle Tipps werden gleichzeitig aufgedeckt.
            Jetzt läuft der Spielzug im Fernsehen.
          </li>
          <li>
            <strong>Der Host löst auf</strong> und erst dann werden Punkte verrechnet.
          </li>
        </ol>

        <h2 className="display">Wie Punkte entstehen</h2>
        <p>
          Es gibt keinen Buchmacher und keine festen Quoten. Alle Einsätze einer Runde wandern
          in einen <strong>gemeinsamen Pool</strong>, und wer richtig liegt, teilt ihn sich.
        </p>
        <p>
          Daraus folgt alles Weitere von selbst: Ein Ausgang, auf den alle setzen, zahlt kaum
          etwas. Ein Ausgang, den sonst niemand auf dem Zettel hatte, zahlt viel. Es geht nicht
          darum, richtig zu liegen — es geht darum, richtiger zu liegen als der Rest.
        </p>

        <h2 className="display">Die Regeln in Zahlen</h2>
        <ul className="facts">
          <li>
            <span className="fact-value">1000</span>
            <span className="fact-label">Punkte zum Start</span>
          </li>
          <li>
            <span className="fact-value">25</span>
            <span className="fact-label">Mindesteinsatz — mehr geht, weniger nicht</span>
          </li>
          <li>
            <span className="fact-value">25</span>
            <span className="fact-label">
              Strafe, wenn du eine Runde gar nicht tippst. Aussitzen kostet also genauso viel
              wie mitspielen, nur ohne Gewinnchance.
            </span>
          </li>
          <li>
            <span className="fact-value">1</span>
            <span className="fact-label">
              Tipp pro Runde. Kein Aufteilen, kein Nachbessern.
            </span>
          </li>
        </ul>
        <p className="hint">
          Bei 0 Punkten bist du nicht raus: Du darfst weiter mittippen und zählst beim Verteilen
          trotzdem mit dem vollen Mindest-Anteil. Von null kommt man zurück.
        </p>
        <p className="hint">
          Tippt niemand den richtigen Ausgang, bekommen alle ihren Einsatz zurück — nur die
          eingesammelten Strafen werden unter denen verteilt, die überhaupt getippt haben.
        </p>

        <h2 className="display">Die Wetten</h2>
        <p className="hint">
          Damit es beim Auflösen keinen Streit gibt, ist festgelegt, was in welchen Topf fällt:
        </p>
        {catalog.map((bet) => (
          <section className="guide-bet" key={bet.id}>
            <h3>{bet.question}</h3>
            {bet.note && <p className="rule">{bet.note}</p>}
            <ul className="guide-outcomes">
              {bet.outcomes.map((outcome) => (
                <li key={outcome.id}>
                  <strong>{outcome.label}</strong>
                  {outcome.note && <span className="note"> — {outcome.note}</span>}
                </li>
              ))}
            </ul>
          </section>
        ))}

        <h2 className="display">Host</h2>
        <p className="hint">
          Der Host ist eine Rolle, kein besonderes Gerät: derselbe Spieler mit denselben
          Punkten, nur mit den Steuerknöpfen. Er entscheidet, welche Wette wann öffnet — die
          15 Sekunden sollen ablaufen, <em>bevor</em> der Spielzug beginnt. Geht er offline,
          rückt automatisch jemand nach.
        </p>
        <p className="hint">
          Passt die offene Wette nicht mehr zum Spiel — das Team geht statt des Field Goals
          doch auf den vierten Versuch —, kann er die Runde abbrechen. Dann passiert nichts:
          keine Einsätze, keine Strafen, keine Punkte.
        </p>

        <button className="button primary wide" onClick={onClose}>
          Alles klar
        </button>
      </div>
    </div>
  );
}
