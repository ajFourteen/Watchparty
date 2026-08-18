package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.domain.model.league.LoginLink;

/**
 * Baut die klickbare Adresse aus einem {@link LoginLink}, an einer Stelle
 * statt in jedem Mail-Adapter einzeln — {@code /league/login/TOKEN} ist die
 * Route, die das Frontend abfängt und einlöst (siehe
 * {@code frontend/src/league/useLeagueAccount.js}). Ein Auseinanderlaufen
 * zwischen Mail-Adapter und Frontend-Route wäre sonst nur beim tatsächlichen
 * Klicken eines Links aufgefallen, nicht beim Bauen.
 */
final class LoginLinkUrl {

    private LoginLinkUrl() {
    }

    static String of(String baseUrl, LoginLink link) {
        return baseUrl + "/league/login/" + link.getToken().value();
    }
}
