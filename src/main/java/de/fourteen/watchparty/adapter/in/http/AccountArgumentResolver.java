package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.out.AccountSessionRepository;
import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.SessionToken;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Clock;
import java.util.Optional;

/**
 * Loest einen {@code @AuthenticatedAccount EmailAddress}-Parameter aus dem
 * Sitzungscookie auf (Kriterium 5). Kein gueltiges Cookie — sei es fehlend,
 * unbekannt oder abgelaufen — wird strukturell nicht unterschieden, alle
 * drei fuehren zu {@link NotAuthenticatedException}.
 */
public class AccountArgumentResolver implements HandlerMethodArgumentResolver {

    private final AccountSessionRepository sessions;
    private final Clock clock;

    public AccountArgumentResolver(AccountSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedAccount.class)
                && parameter.getParameterType() == EmailAddress.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String token = request == null ? null : SessionCookie.read(request.getCookies());
        if (token == null) {
            throw new NotAuthenticatedException();
        }

        Optional<AccountSession> session;
        try {
            session = sessions.findByToken(SessionToken.of(token));
        } catch (IllegalArgumentException e) {
            throw new NotAuthenticatedException();
        }

        AccountSession found = session.orElseThrow(NotAuthenticatedException::new);
        if (!found.isValid(clock.instant())) {
            throw new NotAuthenticatedException();
        }
        return found.getAccountEmail();
    }
}
