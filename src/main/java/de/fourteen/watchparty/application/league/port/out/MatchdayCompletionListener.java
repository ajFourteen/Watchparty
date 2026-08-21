package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.Matchday;

/**
 * Meldet, dass ein Spieltag gerade vollstaendig ausgewertet wurde -- das
 * letzte noch offene Spiel ist auf FINAL oder CANCELLED gewechselt
 * (13.9-o, ADR-041). {@code ScheduleSyncService} erkennt den Uebergang,
 * {@code ReportMailService} haengt als Empfaenger daran und versendet den
 * Spieltags-Report an jedes Konto mit aktivem Opt-in.
 */
public interface MatchdayCompletionListener {

    void onMatchdayCompleted(Matchday matchday);
}
