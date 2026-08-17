package de.fourteen.watchparty.adapter.out.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.fourteen.watchparty.application.league.port.out.ScheduleFeed;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ScheduleFeed} ueber die offen erreichbaren, aber unbeauftragten
 * ESPN-Endpunkte (ADR-037) — Format kann sich jederzeit ohne Ankuendigung
 * aendern, deshalb bricht ein unlesbarer einzelner Spieleintrag nur diesen
 * einen Eintrag (Kriterium 11: der Rest des Spieltags bleibt unberuehrt),
 * niemals den ganzen Abgleich.
 *
 * {@link #parse} ist paketsichtbar statt privat: Adapter-Tests fuettern es
 * direkt mit einer aufgezeichneten Antwort, ohne echtes Netz (ADR-037,
 * docs/teststrategie.md 2.3).
 */
public class EspnScheduleFeed implements ScheduleFeed {

    private static final Logger log = LoggerFactory.getLogger(EspnScheduleFeed.class);
    private static final String PATH = "/apis/site/v2/sports/football/nfl/scoreboard";
    /** ESPNs eigener Code fuer die Regular Season (1 = Preseason, 3 = Postseason). */
    private static final int REGULAR_SEASON_TYPE = 2;

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public EspnScheduleFeed(String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public List<Game> fetchMatchday(Matchday matchday) {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("seasontype", REGULAR_SEASON_TYPE)
                        .queryParam("week", matchday.week())
                        .queryParam("dates", matchday.season().year())
                        .build())
                .retrieve()
                .body(String.class);
        if (body == null) {
            throw new IllegalStateException("Leere Antwort vom Feed fuer " + matchday);
        }
        return parse(body, matchday);
    }

    List<Game> parse(String responseBody, Matchday matchday) {
        JsonNode root;
        try {
            root = mapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Antwort des Feeds ist kein gueltiges JSON", e);
        }

        List<Game> games = new ArrayList<>();
        for (JsonNode event : root.path("events")) {
            Game game = mapEvent(event, matchday);
            if (game != null) {
                games.add(game);
            }
        }
        return games;
    }

    private @Nullable Game mapEvent(JsonNode event, Matchday matchday) {
        String id = event.path("id").asText(null);
        String dateText = event.path("date").asText(null);
        JsonNode competitors = event.path("competitions").path(0).path("competitors");
        if (id == null || dateText == null || !competitors.isArray() || competitors.size() != 2) {
            log.warn("Ueberspringe unvollstaendigen Feed-Eintrag ({}): fehlende Pflichtfelder", id);
            return null;
        }

        Instant kickoff;
        try {
            kickoff = OffsetDateTime.parse(dateText).toInstant();
        } catch (DateTimeParseException e) {
            log.warn("Ueberspringe Spiel {}: Anstosszeit '{}' nicht lesbar", id, dateText);
            return null;
        }

        @Nullable Team home = null;
        @Nullable Team away = null;
        @Nullable Integer homeScore = null;
        @Nullable Integer awayScore = null;
        for (JsonNode competitor : competitors) {
            Team team = mapTeam(competitor.path("team"));
            if (team == null) {
                log.warn("Ueberspringe Spiel {}: Mannschaft nicht lesbar", id);
                return null;
            }
            Integer score = parseScoreOrNull(competitor.path("score").asText(null));
            String homeAway = competitor.path("homeAway").asText("");
            if ("home".equals(homeAway)) {
                home = team;
                homeScore = score;
            } else if ("away".equals(homeAway)) {
                away = team;
                awayScore = score;
            }
        }
        if (home == null || away == null) {
            log.warn("Ueberspringe Spiel {}: Heim- oder Gastmannschaft fehlt", id);
            return null;
        }

        JsonNode statusType = event.path("competitions").path(0).path("status").path("type");
        String statusName = statusType.path("name").asText("");
        boolean completed = statusType.path("completed").asBoolean(false);

        GameStatus status;
        @Nullable GameScore score;
        if (statusName.contains("POSTPONED") || statusName.contains("CANCEL")) {
            status = GameStatus.CANCELLED;
            score = null;
        } else if (completed) {
            if (homeScore == null || awayScore == null) {
                log.warn("Ueberspringe Spiel {}: als beendet gemeldet, aber ohne vollstaendiges Ergebnis", id);
                return null;
            }
            status = GameStatus.FINAL;
            score = GameScore.of(homeScore, awayScore);
        } else {
            status = GameStatus.SCHEDULED;
            score = null;
        }

        return Game.of(GameId.of(id), matchday, home, away, kickoff, status, score, false);
    }

    private @Nullable Team mapTeam(JsonNode teamNode) {
        String abbreviation = teamNode.path("abbreviation").asText(null);
        String displayName = teamNode.path("displayName").asText(null);
        if (abbreviation == null || displayName == null) {
            return null;
        }
        return Team.of(TeamId.of(abbreviation), displayName);
    }

    private @Nullable Integer parseScoreOrNull(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
