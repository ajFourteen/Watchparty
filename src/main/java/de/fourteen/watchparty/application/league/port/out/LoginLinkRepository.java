package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.LoginLink;
import de.fourteen.watchparty.domain.model.league.LoginLinkToken;

import java.util.Optional;

/** Ausgangs-Port fuer Anmeldelinks. Spricht {@link LoginLink} — ein Datenmodell der Domaene. */
public interface LoginLinkRepository {

    void save(LoginLink link);

    Optional<LoginLink> findByToken(LoginLinkToken token);
}
