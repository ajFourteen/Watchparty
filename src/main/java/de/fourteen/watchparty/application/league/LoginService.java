package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.LoginCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.AccountSessionRepository;
import de.fourteen.watchparty.application.league.port.out.LoginLinkRepository;
import de.fourteen.watchparty.application.league.port.out.MailSender;
import de.fourteen.watchparty.application.league.port.out.RateLimiter;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.ClientIp;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLink;
import de.fourteen.watchparty.domain.model.league.LoginLinkToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Setzt {@link LoginCommands} um (ADR-036). Analog zu {@code RoomActor} fuer
 * {@code RoomCommands}, aber auf Request-Threads statt auf dem Raum-Thread —
 * kein Zustand ausser den injizierten Ports, keine Warteschlange noetig.
 */
public class LoginService implements LoginCommands {

    /** Kriterium 2. */
    static final Duration LINK_VALIDITY = Duration.ofMinutes(15);
    /** Kriterium 5. */
    static final Duration SESSION_VALIDITY = Duration.ofDays(90);

    private final Clock clock;
    private final AccountRepository accounts;
    private final LoginLinkRepository loginLinks;
    private final AccountSessionRepository sessions;
    private final MailSender mailSender;
    private final RateLimiter rateLimiter;

    public LoginService(Clock clock, AccountRepository accounts, LoginLinkRepository loginLinks,
            AccountSessionRepository sessions, MailSender mailSender, RateLimiter rateLimiter) {
        this.clock = clock;
        this.accounts = accounts;
        this.loginLinks = loginLinks;
        this.sessions = sessions;
        this.mailSender = mailSender;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Prueft beide Limits, unabhaengig vom Ergebnis des jeweils anderen —
     * sonst zaehlte ein durch die E-Mail-Adresse bereits blockierter Versuch
     * nicht gegen das IP-Limit, und Kriterium 4 gilt fuer beide gleichermassen.
     * Kein Zweig meldet nach aussen, welches (oder ob ueberhaupt ein) Limit
     * gegriffen hat (Kriterium 3/4): {@link #requestLink} kehrt in jedem Fall
     * gleich zurueck.
     */
    @Override
    public void requestLink(EmailAddress email, DisplayName displayName, ClientIp clientIp) {
        Instant now = clock.instant();
        boolean emailErlaubt = rateLimiter.allow("email:" + email.value(), now);
        boolean ipErlaubt = rateLimiter.allow("ip:" + clientIp.value(), now);
        if (!emailErlaubt || !ipErlaubt) {
            return;
        }

        LoginLink link = LoginLink.issue(email, displayName, now, LINK_VALIDITY);
        loginLinks.save(link);
        mailSender.sendLoginLink(link);
    }

    @Override
    public Optional<AccountSession> redeemLink(LoginLinkToken token) {
        Instant now = clock.instant();
        Optional<LoginLink> found = loginLinks.findByToken(token);
        if (found.isEmpty() || !found.get().isValid(now)) {
            return Optional.empty();
        }

        LoginLink link = found.get();
        link.redeem(now);
        loginLinks.save(link);

        Account account = accounts.findByEmail(link.getEmail())
                .orElseGet(() -> Account.of(link.getEmail(), link.getDisplayName(), now));
        accounts.save(account);

        AccountSession session = AccountSession.start(account.getEmail(), now, SESSION_VALIDITY);
        sessions.save(session);
        return Optional.of(session);
    }

    @Override
    public void deleteAccount(EmailAddress email) {
        sessions.deleteByAccountEmail(email);
        accounts.delete(email);
    }
}
