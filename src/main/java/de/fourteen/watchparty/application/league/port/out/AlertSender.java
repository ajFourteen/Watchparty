package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.SeasonId;

/**
 * Benachrichtigt den Betreiber, wenn der Spielplan-Abgleich ueber mehrere
 * Laeufe hinweg fehlschlaegt (docs/betrieb-tippspiel.md). Wie {@link
 * MailSender} ohne Nicht-blockierend-Zusicherung an der Signatur selbst —
 * die traegt stattdessen die jeweilige Implementierung: {@code
 * ScheduleSyncJob} ruft ueber denselben geteilten Scheduler-Thread wie das
 * Auto-Close bei den Live-Wetten, der darf nicht auf einen Mailversand
 * warten (CLAUDE.md, Invariante 2).
 */
public interface AlertSender {

    /** @param consecutiveFailedRuns Anzahl der Läufe in Folge, in denen der Feed für mindestens einen Spieltag nicht erreichbar war. */
    void feedUnreachable(SeasonId season, int consecutiveFailedRuns);
}
