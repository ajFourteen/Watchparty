package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.in.LeagueCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.LeagueName;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.Membership;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.service.league.Standings;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Ligen anlegen, beitreten, verlassen und ihre Rangliste abrufen (Kapitel
 * 13.6). Antworten tragen ausschliesslich Anzeigenamen, nie die
 * E-Mail-Adresse eines anderen Kontos (Kriterium 6) — auch nicht die des
 * Verwalters.
 */
@RestController
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
class LeagueController {

    private final LeagueCommands leagueCommands;
    private final AccountRepository accounts;

    LeagueController(LeagueCommands leagueCommands, AccountRepository accounts) {
        this.leagueCommands = leagueCommands;
        this.accounts = accounts;
    }

    record CreateLeagueRequest(String name, int seasonYear) {
    }

    record CreateLeagueResponse(String id, String code) {
    }

    record JoinLeagueRequest(String code) {
    }

    record LeagueSummary(String id, String name, String code, int seasonYear, boolean isManager) {
    }

    record LeagueDetail(String id, String name, String code, int seasonYear, boolean isManager, List<String> memberNames) {
    }

    record StandingsEntryResponse(String displayName, int totalPoints, int exactCount, int correctTendencyCount,
            int rank, boolean isSelf) {
    }

    @PostMapping("/api/league/leagues")
    ResponseEntity<CreateLeagueResponse> create(@AuthenticatedAccount EmailAddress account, @RequestBody CreateLeagueRequest body) {
        LeagueId id = leagueCommands.createLeague(account, LeagueName.of(body.name()), SeasonId.of(body.seasonYear()));
        League league = leagueCommands.league(account, id).orElseThrow();
        return ResponseEntity.ok(new CreateLeagueResponse(id.value().toString(), league.getCode().value()));
    }

    @PostMapping("/api/league/leagues/join")
    ResponseEntity<Void> join(@AuthenticatedAccount EmailAddress account, @RequestBody JoinLeagueRequest body) {
        LeagueCode code = LeagueCode.parse(body.code());
        if (code == null) {
            return ResponseEntity.badRequest().build();
        }
        leagueCommands.joinLeague(account, code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/league/leagues/{leagueId}/leave")
    ResponseEntity<Void> leave(@AuthenticatedAccount EmailAddress account, @PathVariable UUID leagueId) {
        leagueCommands.leaveLeague(account, LeagueId.of(leagueId));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/league/leagues")
    ResponseEntity<List<LeagueSummary>> mine(@AuthenticatedAccount EmailAddress account) {
        List<LeagueSummary> summaries = leagueCommands.myLeagues(account).stream()
                .map(league -> new LeagueSummary(league.getId().value().toString(), league.getName().value(),
                        league.getCode().value(), league.getSeason().year(), league.getManagerEmail().equals(account)))
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/api/league/leagues/{leagueId}")
    ResponseEntity<LeagueDetail> detail(@AuthenticatedAccount EmailAddress account, @PathVariable UUID leagueId) {
        League league = leagueCommands.league(account, LeagueId.of(leagueId)).orElseThrow(NoSuchElementException::new);
        List<String> memberNames = league.getMembers().stream()
                .map(Membership::getAccountEmail)
                .map(this::displayNameOf)
                .map(DisplayName::value)
                .toList();
        return ResponseEntity.ok(new LeagueDetail(league.getId().value().toString(), league.getName().value(),
                league.getCode().value(), league.getSeason().year(), league.getManagerEmail().equals(account), memberNames));
    }

    @GetMapping("/api/league/leagues/{leagueId}/standings/season")
    ResponseEntity<List<StandingsEntryResponse>> seasonStandings(@AuthenticatedAccount EmailAddress account, @PathVariable UUID leagueId) {
        return ResponseEntity.ok(toResponse(leagueCommands.seasonStandings(LeagueId.of(leagueId)), account));
    }

    @GetMapping("/api/league/leagues/{leagueId}/standings/matchday/{week}")
    ResponseEntity<List<StandingsEntryResponse>> matchdayStandings(@AuthenticatedAccount EmailAddress account,
            @PathVariable UUID leagueId, @PathVariable int week) {
        League league = leagueCommands.league(account, LeagueId.of(leagueId)).orElseThrow(NoSuchElementException::new);
        Matchday matchday = Matchday.of(league.getSeason(), week);
        return ResponseEntity.ok(toResponse(leagueCommands.matchdayStandings(LeagueId.of(leagueId), matchday), account));
    }

    private List<StandingsEntryResponse> toResponse(List<Standings.Entry> entries, EmailAddress requester) {
        return entries.stream()
                .map(entry -> new StandingsEntryResponse(entry.displayName().value(), entry.totalPoints().value(),
                        entry.exactCount(), entry.correctTendencyCount(), entry.rank(), entry.email().equals(requester)))
                .toList();
    }

    private DisplayName displayNameOf(EmailAddress email) {
        return accounts.findByEmail(email).map(Account::getDisplayName).orElse(DisplayName.of("?"));
    }
}
