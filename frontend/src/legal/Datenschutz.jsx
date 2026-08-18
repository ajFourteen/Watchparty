import { LegalPage } from "./LegalPage.jsx";

export function Datenschutz() {
  return (
    <LegalPage title="Datenschutzerklärung">
      <h3>Verantwortlicher</h3>
      <p>
        FOURTEEN IT UG (haftungsbeschränkt), vertreten durch Andreas Jürgensen
        <br />
        Salomon-Petri-Ring 27, 22117 Hamburg
        <br />
        E-Mail: info@fourteen-it.de
      </p>
      <p>
        Weitere Angaben (Registereintrag, USt-ID) siehe <a href="/impressum">Impressum</a>.
      </p>

      <h3>Um welche Anwendung es geht</h3>
      <p>
        Diese Erklärung gilt für die gesamte Anwendung. Sie hat zwei getrennte Spielmodi mit sehr
        unterschiedlichem Umgang mit personenbezogenen Daten:
      </p>
      <ul>
        <li>
          <strong>Live-Wetten</strong> (ein Spielabend): Beitritt nur mit einem frei gewählten
          Namen, ohne Konto, ohne E-Mail-Adresse. Der Name wird nicht überprüft und keiner realen
          Identität zugeordnet. Der Raumzustand liegt ausschließlich im Arbeitsspeicher des
          Servers, gesichert nur als Abzug für einen möglichen Neustart innerhalb desselben
          Abends und mit eingebauter Verfallszeit — danach ist er weg. Ein
          Wiedererkennungs-Token liegt lokal im Browser des jeweiligen Geräts.
        </li>
        <li>
          <strong>Tippspiel-Liga</strong> (eine Saison): Verlangt ein Konto mit E-Mail-Adresse
          und Anzeigename. Diese Erklärung betrifft in der Sache vor allem diesen Modus — hier
          entstehen personenbezogene Daten, die über einen Abend hinaus gespeichert werden.
        </li>
      </ul>

      <h3>Konto und Anmeldung (Tippspiel)</h3>
      <p>
        Bei der Anmeldung erheben wir E-Mail-Adresse und einen selbst gewählten Anzeigenamen.
        Damit versenden wir einen Anmeldelink („Magic Link"); ein Kennwort gibt es nicht.
        Existiert zur Adresse noch kein Konto, entsteht es beim ersten erfolgreichen Einlösen des
        Links.
      </p>
      <ul>
        <li>
          <strong>Zweck:</strong> Wiedererkennung über eine ganze Saison hinweg, damit
          Ergebnistipps und Ligazugehörigkeit zugeordnet werden können.
        </li>
        <li>
          <strong>Rechtsgrundlage:</strong> Art. 6 Abs. 1 lit. b DSGVO (Vertragserfüllung bzw.
          vorvertragliche Maßnahme auf eigene, freiwillige Anfrage).
        </li>
        <li>
          <strong>Speicherdauer:</strong> bis zur Löschung des Kontos durch die betroffene Person
          selbst.
        </li>
      </ul>
      <p>
        Der Anmeldelink ist einmal verwendbar und verfällt nach 15 Minuten. Die Antwort auf eine
        Anmeldeanfrage ist immer dieselbe, unabhängig davon, ob zur Adresse schon ein Konto
        besteht.
      </p>

      <h3>Sitzung</h3>
      <p>
        Nach dem Einlösen des Anmeldelinks setzen wir ein Sitzungscookie. Es hält 90 Tage, damit
        man sich nicht innerhalb einer Saison wiederholt neu anmelden muss. Das Cookie ist
        technisch notwendig für die Anmeldefunktion selbst; dafür ist keine gesonderte
        Einwilligung nötig (§ 25 Abs. 2 Nr. 2 TDDDG).
      </p>

      <h3>Anmeldeversuche und IP-Adresse (Rate Limit)</h3>
      <p>
        Anmeldeanfragen sind je E-Mail-Adresse und je absendender IP-Adresse in ihrer Häufigkeit
        begrenzt, damit niemand die Funktion missbrauchen kann. Die dafür nötigen Daten
        (IP-Adresse, Zeitpunkt) liegen ausschließlich im Arbeitsspeicher des Servers, werden
        nicht in die Datenbank geschrieben und fallen automatisch aus einem gleitenden
        Zeitfenster von 15 Minuten heraus — spätestens bei einem Neustart des Servers sind sie
        vollständig weg. Rechtsgrundlage: Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an
        einem funktionsfähigen, missbrauchsfreien Dienst).
      </p>

      <h3>Ergebnistipps und Ligen</h3>
      <p>
        Ergebnistipps und Ligamitgliedschaften sind an das Konto gebunden und werden für die
        Dauer der Saison bzw. bis zur Löschung des Kontos gespeichert. In Ranglisten ist
        ausschließlich der Anzeigename sichtbar, niemals die E-Mail-Adresse — auch nicht
        gegenüber anderen Mitgliedern derselben Liga oder dem Verwalter einer Liga.
        Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO, wie bei der Anmeldung.
      </p>

      <h3>Weitergabe an Dritte</h3>
      <p>Wir setzen folgende Auftragsverarbeiter ein:</p>
      <ul>
        <li>
          <strong>Versand der Anmeldelinks und Betriebs-Alarme:</strong> IONOS (SMTP).
        </li>
        <li>
          <strong>Hosting:</strong> Fly.io, Region Frankfurt.
        </li>
      </ul>
      <p>
        Eine automatische Ergebnis-Erkennung ruft Daten von ESPN ab (öffentliche Spielpläne und
        Ergebnisse); dorthin werden keine personenbezogenen Daten der Nutzer übertragen.
      </p>

      <h3>Löschung</h3>
      <p>
        Ein Konto kann von der betroffenen Person jederzeit selbst gelöscht werden. Dabei werden
        E-Mail-Adresse und Anzeigename entfernt, und alle Ergebnistipps verschwinden aus
        sämtlichen Ranglisten — es bleibt kein abrufbarer Rest.
      </p>

      <h3>Rechte der betroffenen Person</h3>
      <p>Jede betroffene Person hat das Recht auf:</p>
      <ul>
        <li>Auskunft über die zu ihr gespeicherten Daten (Art. 15 DSGVO),</li>
        <li>Berichtigung unrichtiger Daten (Art. 16 DSGVO),</li>
        <li>
          Löschung (Art. 17 DSGVO) — direkt über die Selbstlöschfunktion des Kontos möglich,
          alternativ per Anfrage an die oben genannte Kontaktadresse,
        </li>
        <li>Einschränkung der Verarbeitung (Art. 18 DSGVO),</li>
        <li>Datenübertragbarkeit (Art. 20 DSGVO),</li>
        <li>
          Widerspruch gegen eine Verarbeitung auf Grundlage berechtigten Interesses (Art. 21
          DSGVO),
        </li>
        <li>Beschwerde bei einer Datenschutzaufsichtsbehörde (Art. 77 DSGVO).</li>
      </ul>

      <h3>Automatisierte Entscheidungsfindung</h3>
      <p>
        Findet nicht statt. Wertungspunkte und Ranglisten werden nach einer festen, offengelegten
        Regel berechnet, nicht durch ein lernendes oder profilbildendes System.
      </p>

      <h3>Änderungen dieser Erklärung</h3>
      <p>
        Diese Erklärung kann angepasst werden, wenn sich die Verarbeitung ändert (z. B. neue
        Auftragsverarbeiter). Es gilt jeweils die zuletzt veröffentlichte Fassung.
      </p>
    </LegalPage>
  );
}
