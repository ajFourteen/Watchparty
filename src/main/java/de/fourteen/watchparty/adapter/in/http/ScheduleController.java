package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zwei Wege ohne den interaktiven Anmeldelink der Tipper: der Handeintrag
 * (Kriterium 14/13.3-g) und der Feed-Relay (ADR-037-Nachtrag vom
 * 2026-08-18).
 *
 * Handeintrag: der Betreiber ueberschreibt ein Endergebnis von Hand, wenn der
 * Feed ausfaellt oder falsch liegt. Kein eigenes Berechtigungsmodell
 * (Kriterium 13.3-h, Ruecksprache vom 2026-08-18, ADR-036): der Betreiber
 * meldet sich wie jeder Tipper per Magic Link an, geprueft wird nur, ob die
 * Sitzung zur konfigurierten Admin-Adresse gehoert.
 *
 * Feed-Relay: ESPN blockiert Zugriffe aus Fly.ios IP-Bereich mit 403. Ein
 * taeglicher GitHub-Actions-Workflow ruft den Feed stattdessen von dort ab
 * und liefert die rohe Antwort hierher. Kein Mensch, deshalb kein
 * Sitzungscookie, sondern ein geteiltes Secret in einem Header — dieselbe
 * Idee wie {@code FLY_API_TOKEN} fuer den Deploy (ADR-019), nur fuer diese
 * eine Anwendung statt fuer die ganze Fly-App.
 */
@RestController
@ConditionalOnProperty(prefix = "watchparty.league.schedule", name = "season-year")
class ScheduleController {

    private final ScheduleCommands scheduleCommands;
    private final EmailAddress adminEmail;
    private final String relayToken;

    ScheduleController(ScheduleCommands scheduleCommands, @Value("${watchparty.league.admin.email}") String adminEmail,
            @Value("${watchparty.league.schedule.relay-token}") String relayToken) {
        this.scheduleCommands = scheduleCommands;
        this.adminEmail = EmailAddress.of(adminEmail);
        this.relayToken = relayToken;
    }

    record SetResultRequest(int home, int away) {
    }

    @PostMapping("/api/league/admin/games/{gameId}/result")
    ResponseEntity<Void> setResultManually(@AuthenticatedAccount EmailAddress account, @PathVariable String gameId,
            @RequestBody SetResultRequest body) {
        if (!account.equals(adminEmail)) {
            throw new NotAuthorizedException();
        }
        scheduleCommands.setResultManually(GameId.of(gameId), GameScore.of(body.home(), body.away()));
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/api/league/feed-relay/{seasonYear}/{week}", consumes = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<Void> relayFeed(@RequestHeader("X-Relay-Token") String token, @PathVariable int seasonYear,
            @PathVariable int week, @RequestBody String rawFeedResponse) {
        if (!relayToken.equals(token)) {
            throw new NotAuthorizedException();
        }
        scheduleCommands.ingestRelayedFeed(Matchday.of(SeasonId.of(seasonYear), week), rawFeedResponse);
        return ResponseEntity.ok().build();
    }
}
