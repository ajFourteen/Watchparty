package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.in.ReportMailCommands;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verwaltung des Mailversands des Spieltags-Reports (13.9-n/p, ADR-041).
 * {@link #unsubscribe} braucht bewusst kein {@code @AuthenticatedAccount} —
 * der Ein-Klick-Abmeldelink muss ohne Anmeldung wirken, der Token im Pfad
 * ist bereits der Nachweis (Kriterium analog 13.9-p).
 */
@RestController
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
class ReportMailController {

    private final ReportMailCommands reportMailCommands;

    ReportMailController(ReportMailCommands reportMailCommands) {
        this.reportMailCommands = reportMailCommands;
    }

    @PostMapping("/api/league/report-mail/opt-in")
    ResponseEntity<Void> optIn(@AuthenticatedAccount EmailAddress account) {
        reportMailCommands.optIn(account);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/league/report-mail/opt-out")
    ResponseEntity<Void> optOut(@AuthenticatedAccount EmailAddress account) {
        reportMailCommands.optOut(account);
        return ResponseEntity.ok().build();
    }

    /** Immer derselbe Erfolg, auch bei unbekanntem Token (13.9-p) — kein unterscheidbares Ergebnis nach aussen. */
    @PostMapping("/api/league/report-mail/unsubscribe/{token}")
    ResponseEntity<Void> unsubscribe(@PathVariable String token) {
        reportMailCommands.unsubscribe(ReportMailToken.of(token));
        return ResponseEntity.ok().build();
    }
}
