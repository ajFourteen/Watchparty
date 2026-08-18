package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der Rauchtest ueber die Leitung fuers Tippspiel (ADR-039, Abschnitt 2.4),
 * analog zu {@code WireProtocolSmokeTest} auf der Live-Wetten-Seite: ein
 * echter Server, ein echtes Postgres, echtes JSON. Kann der HTTP-Adapter
 * alles uebertragen, was die Kommando-Ports ausdruecken — Anmeldung,
 * Sitzungscookie, Tippen, Ligen, Rangliste? Keine neue fachliche Abdeckung,
 * die haben die Port-to-Port-Szenarien schon geliefert.
 */
@ApiTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LeagueHttpFlowTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("watchparty.league.db.url", POSTGRES::getJdbcUrl);
        registry.add("watchparty.league.db.username", POSTGRES::getUsername);
        registry.add("watchparty.league.db.password", POSTGRES::getPassword);
        // watchparty.league.schedule.season-year bewusst NICHT gesetzt: dieser
        // Test prueft den Anmelde-/Liga-Weg, nicht den Spielplan-Abgleich.
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
        rest.postForEntity(baseUrl() + "/api/league/login",
                Map.of("email", email, "displayName", displayName), Void.class);

        String token = loginLinkTokenFor(email);
        ResponseEntity<Void> redeemed = rest.postForEntity(baseUrl() + "/api/league/login/" + token, null, Void.class);
        assertThat(redeemed.getStatusCode()).isEqualTo(HttpStatus.OK);

        String setCookie = redeemed.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Anmeldung sollte ein Sitzungscookie setzen").isNotNull();
        return setCookie.split(";", 2)[0];
    }

    private HttpEntity<Void> authenticated(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authenticated(String cookie, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void ohneSitzungscookieIstDerZugriffAbgelehnt() {
        ResponseEntity<Void> response = rest.getForEntity(baseUrl() + "/api/league/me", Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anmeldenTippenUndDieRanglisteZeigtDasErgebnis() {
        String cookie = redeemAndGetCookie("anna@example.org", "Anna");

        ResponseEntity<Map> me = rest.exchange(baseUrl() + "/api/league/me", HttpMethod.GET, authenticated(cookie), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("email", "anna@example.org").containsEntry("displayName", "Anna");

        ResponseEntity<Map> created = rest.exchange(baseUrl() + "/api/league/leagues", HttpMethod.POST,
                authenticated(cookie, Map.of("name", "Büro-Liga", "seasonYear", 2026)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String leagueId = (String) created.getBody().get("id");
        assertThat(leagueId).isNotBlank();
        assertThat(created.getBody().get("code")).isNotNull();

        String gameId = "test-" + UUID.randomUUID();
        games.save(Game.of(GameId.of(gameId), Matchday.of(SeasonId.of(2026), 1),
                Team.of(TeamId.of("KC"), "Kansas City Chiefs"), Team.of(TeamId.of("SF"), "San Francisco 49ers"),
                Instant.now().plusSeconds(3600), GameStatus.SCHEDULED, null, false));

        ResponseEntity<Void> submitted = rest.exchange(baseUrl() + "/api/league/predictions", HttpMethod.POST,
                authenticated(cookie, Map.of("gameId", gameId, "home", 24, "away", 17)), Void.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);

        Game finished = games.findById(GameId.of(gameId)).orElseThrow();
        finished.applyManualResult(GameScore.of(24, 17));
        games.save(finished);

        ResponseEntity<Map> totalPoints = rest.exchange(baseUrl() + "/api/league/predictions/total-points",
                HttpMethod.GET, authenticated(cookie), Map.class);
        assertThat(totalPoints.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(totalPoints.getBody()).containsEntry("totalPoints", 6);

        ResponseEntity<List> standings = rest.exchange(baseUrl() + "/api/league/leagues/" + leagueId + "/standings/season",
                HttpMethod.GET, authenticated(cookie), List.class);
        assertThat(standings.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(standings.getBody()).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> entry = (Map<String, Object>) standings.getBody().get(0);
        assertThat(entry).containsEntry("displayName", "Anna").containsEntry("totalPoints", 6).containsEntry("rank", 1);
    }

    @Test
    void nachDemAbmeldenIstDasCookieUnwirksam() {
        String cookie = redeemAndGetCookie("ben@example.org", "Ben");

        ResponseEntity<Void> loggedOut = rest.postForEntity(baseUrl() + "/api/league/logout", authenticated(cookie), Void.class);
        String clearedCookie = loggedOut.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];

        ResponseEntity<Void> afterLogout = rest.exchange(baseUrl() + "/api/league/me", HttpMethod.GET,
                authenticated(clearedCookie), Void.class);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void einVerbrauchterAnmeldelinkMeldetNiemandenAnUeberDieLeitung() {
        rest.postForEntity(baseUrl() + "/api/league/login", Map.of("email", "cem@example.org", "displayName", "Cem"), Void.class);
        String token = loginLinkTokenFor("cem@example.org");
        rest.postForEntity(baseUrl() + "/api/league/login/" + token, null, Void.class);

        ResponseEntity<Void> secondAttempt = rest.postForEntity(baseUrl() + "/api/league/login/" + token, null, Void.class);

        assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
