package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.EmailAddress;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakeAccountRepository implements AccountRepository {

    private final Map<String, Account> byEmail = new LinkedHashMap<>();

    @Override
    public void save(Account account) {
        byEmail.put(account.getEmail().value(), account);
    }

    @Override
    public Optional<Account> findByEmail(EmailAddress email) {
        return Optional.ofNullable(byEmail.get(email.value()));
    }

    @Override
    public void delete(EmailAddress email) {
        byEmail.remove(email.value());
    }
}
