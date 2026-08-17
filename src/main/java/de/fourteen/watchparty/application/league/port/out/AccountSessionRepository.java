package de.fourteen.watchparty.application.league.port.out;

import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.SessionToken;

import java.util.Optional;

/** Ausgangs-Port fuer Sitzungen. Spricht {@link AccountSession} — ein Datenmodell der Domaene. */
public interface AccountSessionRepository {

    void save(AccountSession session);

    Optional<AccountSession> findByToken(SessionToken token);

    /** Beendet alle Sitzungen eines Kontos (Kriterium 7: Loeschen raeumt auch angemeldete Sitzungen ab). */
    void deleteByAccountEmail(EmailAddress accountEmail);
}
