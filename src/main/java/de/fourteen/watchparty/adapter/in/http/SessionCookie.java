package de.fourteen.watchparty.adapter.in.http;

import jakarta.servlet.http.Cookie;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Name und Bauweise des Sitzungscookies, an einer Stelle statt in jedem
 * Controller einzeln. {@code secure} ist konfigurierbar (siehe
 * {@code LeagueHttpConfig}): in Produktion immer an, fuer die lokale
 * Entwicklung ohne HTTPS abschaltbar — sonst kaeme das Cookie beim Browser
 * nie an.
 */
final class SessionCookie {

    static final String NAME = "watchparty_league_session";
    private static final Duration VALIDITY = Duration.ofDays(90);

    private SessionCookie() {
    }

    static ResponseCookie set(String token, boolean secure) {
        return ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(VALIDITY)
                .build();
    }

    static ResponseCookie clear(boolean secure) {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    static @Nullable String read(Cookie @Nullable [] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
