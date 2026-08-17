package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.AccountSessionRepository;
import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.SessionToken;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakeAccountSessionRepository implements AccountSessionRepository {

    private final Map<String, AccountSession> byToken = new LinkedHashMap<>();

    @Override
    public void save(AccountSession session) {
        byToken.put(session.getToken().value(), session);
    }

    @Override
    public Optional<AccountSession> findByToken(SessionToken token) {
        return Optional.ofNullable(byToken.get(token.value()));
    }

    @Override
    public void deleteByAccountEmail(EmailAddress accountEmail) {
        byToken.values().removeIf(session -> session.getAccountEmail().equals(accountEmail));
    }
}
