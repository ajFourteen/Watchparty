package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.LoginLinkRepository;
import de.fourteen.watchparty.domain.model.league.LoginLink;
import de.fourteen.watchparty.domain.model.league.LoginLinkToken;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakeLoginLinkRepository implements LoginLinkRepository {

    private final Map<String, LoginLink> byToken = new LinkedHashMap<>();

    @Override
    public void save(LoginLink link) {
        byToken.put(link.getToken().value(), link);
    }

    @Override
    public Optional<LoginLink> findByToken(LoginLinkToken token) {
        return Optional.ofNullable(byToken.get(token.value()));
    }
}
