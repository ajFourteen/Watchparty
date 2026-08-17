package de.fourteen.watchparty.application.league.port.in;

import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.ClientIp;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLinkToken;

import java.util.Optional;

/**
 * Was von aussen ausgeloest werden kann, um sich anzumelden (ADR-036). Kein
 * Rueckgabewert bei {@link #requestLink}: Die Antwort auf eine
 * Anmeldeanfrage ist immer dieselbe, egal ob die Adresse bekannt ist oder
 * das Limit greift (Kriterium 3/4) — dieser Vertrag macht ein
 * unterscheidbares Ergebnis strukturell unmoeglich, statt sich auf
 * Disziplin im Adapter zu verlassen.
 */
public interface LoginCommands {

    /**
     * Fordert einen Anmeldelink an. {@code displayName} wird nur gebraucht,
     * falls zur Adresse noch kein Konto existiert (Kriterium 1/6) — existiert
     * eines bereits, wird er beim Einloesen verworfen.
     */
    void requestLink(EmailAddress email, DisplayName displayName, ClientIp clientIp);

    /** Loest einen Link ein. Leer bei unbekanntem, verbrauchtem oder abgelaufenem Token (Kriterium 2). */
    Optional<AccountSession> redeemLink(LoginLinkToken token);

    /** Loescht ein Konto samt seiner Sitzungen (Kriterium 7). */
    void deleteAccount(EmailAddress email);
}
