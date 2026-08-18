package de.fourteen.watchparty.config.league;

import de.fourteen.watchparty.adapter.in.http.AccountArgumentResolver;
import de.fourteen.watchparty.application.league.port.out.AccountSessionRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.util.List;

/** Registriert den Argument-Resolver, der {@code @AuthenticatedAccount} aus dem Sitzungscookie aufloest (ADR-039). */
@Configuration
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
public class LeagueWebConfig implements WebMvcConfigurer {

    private final AccountSessionRepository sessions;
    private final Clock clock;

    public LeagueWebConfig(AccountSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AccountArgumentResolver(sessions, clock));
    }
}
