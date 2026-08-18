package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.application.league.port.out.AlertSender;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schreibt einen Feed-Alarm nur ins Log statt ihn zu versenden.
 *
 * Faellt ein, solange kein echter Mailversand konfiguriert ist (dieselbe
 * Bedingung wie {@link LoggingMailSender}) — fuers lokale Entwickeln und
 * fuer einen Betrieb ohne Strato-Zugangsdaten.
 */
public class LoggingAlertSender implements AlertSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertSender.class);

    @Override
    public void feedUnreachable(SeasonId season, int consecutiveFailedRuns) {
        log.warn("ALARM: Feed fuer Saison {} ist {} Mal in Folge fehlgeschlagen", season, consecutiveFailedRuns);
    }
}
