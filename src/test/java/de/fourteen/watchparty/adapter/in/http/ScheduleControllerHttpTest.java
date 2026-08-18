package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.ApiTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der Handeintrag-Notweg ueber die Leitung (Kriterium 13.3-g/13.3-h,
 * ADR-036): nur das konfigurierte Admin-Konto darf ein Endergebnis von Hand
 * setzen, jedes andere authentifizierte Konto wird abgelehnt.
 *
 * {@code watchparty.league.feed.base-url} zeigt bewusst auf einen
 * unerreichbaren lokalen Port statt auf ESPN — der Nachfuehr-Job laeuft
 * beim Start trotzdem an (Kriterium 9), soll in Tests aber nicht wirklich
 * das Netz erreichen (derselbe Grundsatz wie in {@code LeagueHttpFlowTest}),
 * nur eben mit gesetztem {@code season-year}, weil {@code ScheduleController}
 * genau davon abhaengt.
 */
@ApiTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ScheduleControllerHttpTest {

    private static final String ADMIN_EMAIL = "admin@example.org";
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("watchparty.league.db.url", POSTGRES::getJdbcUrl);
        registry.add("watchparty.league.db.username", POSTGRES::getUsername);
        registry.add("watchparty.league.db.password", POSTGRES::getPassword);
        registry.add("watchparty.league.admin.email", () -> ADMIN_EMAIL);
        registry.add("watchparty.league.schedule.season-year", () -> "2026");
        registry.add("watchparty.league.feed.base-url", () -> "http://127.0.0.1:1");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private GameRepository games;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String loginLinkTokenFor(String email) {
        return jdbc.queryForObject(
                "SELECT token FROM login_link WHERE email = :email ORDER BY created_at DESC LIMIT 1",
                new MapSqlParameterSource("email", email), String.class);
    }

    private String redeemAndGetCookie(String email, String displayName) {
        rest.postForEntity(baseUrl() + "/api/league/login", Map.of("email", email, "displayName", displayName), Void.class);

        String token = loginLinkTokenFor(email);
        ResponseEntity<Void> redeemed = rest.postForEntity(baseUrl() + "/api/league/login/" + token, null, Void.class);
        String setCookie = redeemed.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Anmeldung sollte ein Sitzungscookie setzen").isNotNull();
        return setCookie.split(";", 2)[0];
    }

    private <T> HttpEntity<T> authenticated(String cookie, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return new HttpEntity<>(body, headers);
    }

    private GameId newGame() {
        String gameId = "test-" + UUID.randomUUID();
        games.save(Game.of(GameId.of(gameId), Matchday.of(SeasonId.of(2026), 1),
                Team.of(TeamId.of("KC"), "Kansas City Chiefs"), Team.of(TeamId.of("SF"), "San Francisco 49ers"),
                Instant.now().minusSeconds(3600), GameStatus.SCHEDULED, null, false));
        return GameId.of(gameId);
    }

    @Test
    void dasAdminKontoKannEinErgebnisVonHandSetzen() {
        String cookie = redeemAndGetCookie(ADMIN_EMAIL, "Admin");
        GameId gameId = newGame();

        ResponseEntity<Void> response = rest.exchange(baseUrl() + "/api/league/admin/games/" + gameId.value() + "/result",
                HttpMethod.POST, authenticated(cookie, Map.of("home", 24, "away", 17)), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Game updated = games.findById(gameId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GameStatus.FINAL);
        assertThat(updated.getScore()).isEqualTo(de.fourteen.watchparty.domain.model.league.GameScore.of(24, 17));
    }

    @Test
    @Anforderung("13.3-h")
    void einGewoehnlicherTipperDarfKeinenHandeintragSetzen() {
        String cookie = redeemAndGetCookie("anna@example.org", "Anna");
        GameId gameId = newGame();

        ResponseEntity<Void> response = rest.exchange(baseUrl() + "/api/league/admin/games/" + gameId.value() + "/result",
                HttpMethod.POST, authenticated(cookie, Map.of("home", 24, "away", 17)), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Game unchanged = games.findById(gameId).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(GameStatus.SCHEDULED);
    }
}
