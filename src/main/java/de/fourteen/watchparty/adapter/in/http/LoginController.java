package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.in.LoginCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.ClientIp;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLinkToken;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Anmeldung per Magic Link (ADR-036, Kapitel 13.2) und Kontoloeschung
 * (Kriterium 7). {@link #requestLink} antwortet immer gleich (Kriterium 3)
 * — kein Zweig hier verraet, ob die Adresse bekannt war, weil
 * {@code LoginService} das ohnehin nie prueft.
 */
@RestController
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
class LoginController {

    private final LoginCommands loginCommands;
    private final AccountRepository accounts;
    private final boolean cookieSecure;

    LoginController(LoginCommands loginCommands, AccountRepository accounts,
            @Value("${watchparty.league.session.cookie-secure:true}") boolean cookieSecure) {
        this.loginCommands = loginCommands;
        this.accounts = accounts;
        this.cookieSecure = cookieSecure;
    }

    record LoginRequest(String email, String displayName) {
    }

    record AccountResponse(String email, String displayName) {
    }

    @PostMapping("/api/league/login")
    ResponseEntity<Void> requestLink(@RequestBody LoginRequest body, HttpServletRequest request) {
        loginCommands.requestLink(EmailAddress.of(body.email()), DisplayName.of(body.displayName()),
                ClientIp.of(clientIpOf(request)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/league/login/{token}")
    ResponseEntity<Void> redeem(@PathVariable String token) {
        Optional<AccountSession> session = loginCommands.redeemLink(LoginLinkToken.of(token));
        if (session.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, SessionCookie.set(session.get().getToken().value(), cookieSecure).toString())
                .build();
    }

    @PostMapping("/api/league/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, SessionCookie.clear(cookieSecure).toString())
                .build();
    }

    @GetMapping("/api/league/me")
    ResponseEntity<AccountResponse> me(@AuthenticatedAccount EmailAddress account) {
        Account found = accounts.findByEmail(account).orElseThrow();
        return ResponseEntity.ok(new AccountResponse(found.getEmail().value(), found.getDisplayName().value()));
    }

    @DeleteMapping("/api/league/account")
    ResponseEntity<Void> deleteAccount(@AuthenticatedAccount EmailAddress account) {
        loginCommands.deleteAccount(account);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, SessionCookie.clear(cookieSecure).toString())
                .build();
    }

    /** X-Forwarded-For zuerst: Fly.io (ADR-018) reicht Verbindungen durch einen Proxy, die direkte Adresse waere dessen eigene. */
    private static String clientIpOf(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
