package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.domain.model.league.Account;

/**
 * Baut die klickbare Abmeldeadresse aus einem {@link Account}, an einer
 * Stelle statt in jedem Mail-Adapter einzeln — {@code
 * /league/report-mail/unsubscribe/TOKEN} ist die Route, die das Frontend
 * abfängt und einlöst (siehe {@code frontend/src/league/useLeagueAccount.js}),
 * analog zu {@link LoginLinkUrl}.
 */
final class ReportMailUnsubscribeUrl {

    private ReportMailUnsubscribeUrl() {
    }

    static String of(String baseUrl, Account account) {
        return baseUrl + "/league/report-mail/unsubscribe/" + account.getReportMailToken().value();
    }
}
