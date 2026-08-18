package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Der Handeintrag-Notweg (Kriterium 14/13.3-g): der Betreiber ueberschreibt
 * ein Endergebnis von Hand, wenn der Feed ausfaellt oder falsch liegt.
 *
 * Kein eigenes Berechtigungsmodell (Kriterium 13.3-h, Ruecksprache vom
 * 2026-08-18, ADR-036): der Betreiber meldet sich wie jeder Tipper per
 * Magic Link an, geprueft wird nur, ob die Sitzung zur konfigurierten
 * Admin-Adresse gehoert.
 */
@RestController
@ConditionalOnProperty(prefix = "watchparty.league.schedule", name = "season-year")
class ScheduleController {

    private final ScheduleCommands scheduleCommands;
    private final EmailAddress adminEmail;

    ScheduleController(ScheduleCommands scheduleCommands, @Value("${watchparty.league.admin.email}") String adminEmail) {
        this.scheduleCommands = scheduleCommands;
        this.adminEmail = EmailAddress.of(adminEmail);
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
}
