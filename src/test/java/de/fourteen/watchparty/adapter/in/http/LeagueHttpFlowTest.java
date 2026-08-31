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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
// Das Rate Limit je IP (Kriterium 4) ist auf der Port-Ebene entschieden
// (AnmeldungScenarioTest) und wird hier bewusst nicht noch einmal geprueft
// (Abschnitt 1: jede Ebene testet nur, was die Ebene darunter nicht kann).
// Alle Anmeldungen dieser Klasse kommen von localhost, teilen sich also
// denselben Schluessel im InMemoryRateLimiter -- ohne angehobene Schranke
// wuerde das Hinzufuegen eines weiteren Szenarios ein bestehendes umwerfen.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "watchparty.league.login.rate-limit.max-attempts=100")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// Boot 4 stellt TestRestTemplate nicht mehr allein wegen des Webservers
// bereit -- die Bean kommt erst mit dieser Annotation.
@AutoConfigureTestRestTemplate
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
    void mailversandBestellenUndUeberDenAbmeldelinkOhneAnmeldungWiederAbbestellen() {
        String cookie = redeemAndGetCookie("dana@example.org", "Dana");

        ResponseEntity<Void> optedIn = rest.postForEntity(baseUrl() + "/api/league/report-mail/opt-in",
                authenticated(cookie), Void.class);
        assertThat(optedIn.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> me = rest.exchange(baseUrl() + "/api/league/me", HttpMethod.GET, authenticated(cookie), Map.class);
        assertThat(me.getBody()).containsEntry("reportMailOptIn", true);

        String token = jdbc.queryForObject(
                "SELECT report_mail_token FROM account WHERE email = :email",
                new MapSqlParameterSource("email", "dana@example.org"), String.class);

        ResponseEntity<Void> unsubscribed = rest.postForEntity(
                baseUrl() + "/api/league/report-mail/unsubscribe/" + token, null, Void.class);
        assertThat(unsubscribed.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> meAfter = rest.exchange(baseUrl() + "/api/league/me", HttpMethod.GET, authenticated(cookie), Map.class);
        assertThat(meAfter.getBody()).containsEntry("reportMailOptIn", false);
    }

    @Test
    void einUnbekannterAbmeldelinkTokenQuittiertDenselbenErfolg() {
        ResponseEntity<Void> response = rest.postForEntity(
                baseUrl() + "/api/league/report-mail/unsubscribe/unbekannter-token", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void einVerbrauchterAnmeldelinkMeldetNiemandenAnUeberDieLeitung() {
        rest.postForEntity(baseUrl() + "/api/league/login", Map.of("email", "cem@example.org", "displayName", "Cem"), Void.class);
        String token = loginLinkTokenFor("cem@example.org");
        rest.postForEntity(baseUrl() + "/api/league/login/" + token, null, Void.class);

        ResponseEntity<Void> secondAttempt = rest.postForEntity(baseUrl() + "/api/league/login/" + token, null, Void.class);

        assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Leck-Test am tatsaechlich serialisierten JSON (docs/teststrategie.md,
     * Abschnitt 3.1) fuer Kriterium 19/20 — das Gegenstueck zu
     * {@code WireProtocolSmokeTest} auf der Live-Wetten-Seite.
     *
     * {@code PredictionViewTest} prueft dieselbe Regel bereits am
     * Nachrichtenobjekt, und zwar mit Mutation Score 100 %. Das genuegt
     * nicht: Abschnitt 3.1 verlangt beide Ebenen ausdruecklich, weil ein
     * Leck auch erst durch Jackson entstehen kann — ein zusaetzlicher
     * Getter, eine Annotation, ein neues Feld in einem verschachtelten
     * Record. Der Java-seitige Test kann das grundsaetzlich nicht sehen.
     *
     * Geprueft wird deshalb ueber die Ausgabeflaeche, nicht ueber Beispiele:
     * eine Positivliste ueber die Feldnamen selbst, dazu die Abwesenheit
     * des fremden Anzeigenamens im Rohtext der Antwort.
     */
    @Test
    void vorDemAnstossStehtImJsonSelbstKeinFremderTipp() {
        String cookie = redeemAndGetCookie("elif@example.org", "Elif");
        String fremdesCookie = redeemAndGetCookie("faruk@example.org", "Faruk");

        String gameId = "leak-" + UUID.randomUUID();
        games.save(Game.of(GameId.of(gameId), Matchday.of(SeasonId.of(2026), 7),
                Team.of(TeamId.of("GB"), "Green Bay Packers"), Team.of(TeamId.of("CHI"), "Chicago Bears"),
                Instant.now().plusSeconds(3600), GameStatus.SCHEDULED, null, false));

        rest.exchange(baseUrl() + "/api/league/predictions", HttpMethod.POST,
                authenticated(cookie, Map.of("gameId", gameId, "home", 21, "away", 10)), Void.class);
        rest.exchange(baseUrl() + "/api/league/predictions", HttpMethod.POST,
                authenticated(fremdesCookie, Map.of("gameId", gameId, "home", 31, "away", 28)), Void.class);

        String json = rest.exchange(baseUrl() + "/api/league/schedule/2026/7", HttpMethod.GET,
                authenticated(cookie), String.class).getBody();

        assertThat(json)
                .as("Der Anzeigename des anderen Tippers darf vor dem Anstoss nicht uebertragen werden")
                .doesNotContain("Faruk");
        assertThat(json)
                .as("Auch die Zahlen des fremden Tipps duerfen nicht auftauchen")
                .doesNotContain("\"home\":31");

        Set<String> erlaubteFelderJeSpiel = Set.of(
                "gameId", "homeTeamName", "awayTeamName", "kickoff", "status",
                "finalScore", "ownPrediction", "otherPredictions");
        assertThat(erlaubteFelderJeSpiel).containsAll(feldnamenDesSpiels(json, gameId));
    }

    /**
     * Die Gegenprobe zum Leck-Test: Ab dem Anstoss ist derselbe fremde Tipp
     * Teil der Antwort. Ohne sie wuerde ein {@code otherPredictions}, das
     * versehentlich immer leer bleibt, als bestandener Leck-Test gelten.
     */
    @Test
    void abDemAnstossStehtDerFremdeTippImJson() {
        String cookie = redeemAndGetCookie("gita@example.org", "Gita");
        String fremdesCookie = redeemAndGetCookie("hakan@example.org", "Hakan");

        String gameId = "leak-nach-anstoss-" + UUID.randomUUID();
        games.save(Game.of(GameId.of(gameId), Matchday.of(SeasonId.of(2026), 8),
                Team.of(TeamId.of("DAL"), "Dallas Cowboys"), Team.of(TeamId.of("PHI"), "Philadelphia Eagles"),
                Instant.now().plusSeconds(3600), GameStatus.SCHEDULED, null, false));

        rest.exchange(baseUrl() + "/api/league/predictions", HttpMethod.POST,
                authenticated(cookie, Map.of("gameId", gameId, "home", 14, "away", 7)), Void.class);
        rest.exchange(baseUrl() + "/api/league/predictions", HttpMethod.POST,
                authenticated(fremdesCookie, Map.of("gameId", gameId, "home", 35, "away", 3)), Void.class);

        // Anstoss in die Vergangenheit ruecken: derselbe Weg, den auch der
        // Nachfuehr-Job nimmt (mergeFromFeed uebernimmt den Anstoss immer).
        jdbc.update("UPDATE game SET kickoff = :kickoff WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("kickoff", java.sql.Timestamp.from(Instant.now().minusSeconds(60)))
                        .addValue("id", gameId));

        String json = rest.exchange(baseUrl() + "/api/league/schedule/2026/8", HttpMethod.GET,
                authenticated(cookie), String.class).getBody();

        assertThat(json).contains("Hakan").contains("\"home\":35");
        Set<String> erlaubteFelderJeFremdemTipp = Set.of("displayName", "score");
        assertThat(erlaubteFelderJeFremdemTipp).containsAll(feldnamenDerFremdenTipps(json, gameId));
    }


    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Alle Feldnamen, die Jackson tatsaechlich fuer dieses Spiel geschrieben hat. */
    private Set<String> feldnamenDesSpiels(String json, String gameId) {
        return feldnamen(spielKnoten(json, gameId));
    }

    /** Alle Feldnamen, die Jackson je fremdem Tipp dieses Spiels geschrieben hat. */
    private Set<String> feldnamenDerFremdenTipps(String json, String gameId) {
        JsonNode tipps = spielKnoten(json, gameId).path("otherPredictions");
        assertThat(tipps).as("Nach dem Anstoss sollte mindestens ein fremder Tipp dabei sein").isNotEmpty();
        return StreamSupport.stream(tipps.spliterator(), false)
                .flatMap(tipp -> feldnamen(tipp).stream())
                .collect(Collectors.toSet());
    }

    private JsonNode spielKnoten(String json, String gameId) {
        try {
            JsonNode spiele = MAPPER.readTree(json).path("games");
            return StreamSupport.stream(spiele.spliterator(), false)
                    .filter(spiel -> gameId.equals(spiel.path("gameId").asText()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Spiel " + gameId + " fehlt in der Antwort: " + json));
        } catch (Exception e) {
            throw new AssertionError("Antwort war kein lesbares JSON: " + json, e);
        }
    }

    private Set<String> feldnamen(JsonNode knoten) {
        return Set.copyOf(knoten.propertyNames());
    }

}
