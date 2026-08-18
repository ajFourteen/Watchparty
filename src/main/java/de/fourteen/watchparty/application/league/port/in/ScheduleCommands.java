package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;

/** Was von aussen ausgeloest werden kann, um Spielplan und Ergebnisse zu pflegen (ADR-037). */
public interface ScheduleCommands {

    /**
     * Gleicht einen Spieltag mit dem Feed ab (Kriterium 9), per Live-Abruf
     * durch die Anwendung selbst. Ein Ausfall des Feeds laesst den Stand
     * unangetastet (Kriterium 11). Produktiv nicht mehr automatisch
     * ausgeloest (ADR-037-Nachtrag vom 2026-08-18, siehe {@link
     * #ingestRelayedFeed}), bleibt aber als Faehigkeit bestehen.
     */
    void syncMatchday(Matchday matchday);

    /**
     * Gleicht die gesamte Regular Season ab, Spieltag fuer Spieltag — ein
     * einzelner ausgefallener Spieltag haelt die uebrigen nicht auf.
     *
     * @return Anzahl der Spieltage, fuer die der Feed in diesem Lauf nicht
     *         erreichbar war (0 bedeutet: der Lauf war vollstaendig
     *         erfolgreich).
     */
    int syncSeason(SeasonId season);

    /**
     * Wertet eine andernorts abgerufene Feed-Antwort fuer einen Spieltag aus
     * und gleicht sie ab — derselbe Effekt wie {@link #syncMatchday}, nur
     * ohne selbst eine Netzwerkverbindung zu ESPN aufzubauen
     * (ADR-037-Nachtrag vom 2026-08-18: ESPN blockiert Fly.ios IP-Bereich,
     * ein taeglicher GitHub-Actions-Workflow ruft stattdessen von dort ab).
     */
    void ingestRelayedFeed(Matchday matchday, String rawResponse);

    /** Der Notweg aus Kriterium 14: der Betreiber setzt ein Endergebnis von Hand. */
    void setResultManually(GameId gameId, GameScore score);
}
