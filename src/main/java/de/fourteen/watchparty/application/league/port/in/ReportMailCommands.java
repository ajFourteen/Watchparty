package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;

/**
 * Was von aussen ausgeloest werden kann, um den Mailversand des
 * Spieltags-Reports zu verwalten (13.9-n/p, ADR-041).
 */
public interface ReportMailCommands {

    /** Bestellt den Mailversand (Opt-in). */
    void optIn(EmailAddress account);

    /** Bestellt den Mailversand ab (Opt-out). */
    void optOut(EmailAddress account);

    /**
     * Loest den Ein-Klick-Abmeldelink ein, ohne Anmeldung (13.9-p). Ein
     * unbekannter Token quittiert denselben Erfolg wie ein gueltiger --
     * dieselbe Zusage wie bei {@link LoginCommands#requestLink}, kein
     * unterscheidbares Ergebnis nach aussen.
     */
    void unsubscribe(ReportMailToken token);
}
