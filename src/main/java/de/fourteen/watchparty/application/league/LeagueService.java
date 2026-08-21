package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.in.LeagueCommands;
import de.fourteen.watchparty.application.league.port.out.AccountRepository;
import de.fourteen.watchparty.application.league.port.out.GameRepository;
import de.fourteen.watchparty.application.league.port.out.LeagueRepository;
import de.fourteen.watchparty.application.league.port.out.PredictionRepository;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.LeagueName;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.Membership;
import de.fourteen.watchparty.domain.model.league.Prediction;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.service.league.Standings;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/** Setzt {@link LeagueCommands} um (Kapitel 13.6). Die Wertung selbst steckt in {@link Standings}, hier wird nur beschafft. */
public class LeagueService implements LeagueCommands {

    private final Clock clock;
    private final LeagueRepository leagues;
    private final GameRepository games;
    private final PredictionRepository predictions;
    private final AccountRepository accounts;

    public LeagueService(Clock clock, LeagueRepository leagues, GameRepository games,
            PredictionRepository predictions, AccountRepository accounts) {
        this.clock = clock;
        this.leagues = leagues;
        this.games = games;
        this.predictions = predictions;
        this.accounts = accounts;
    }

    @Override
    public LeagueId createLeague(EmailAddress manager, LeagueName name, SeasonId season) {
        League league = League.create(season, name, manager, clock.instant());
        leagues.save(league);
        return league.getId();
    }

    /**
     * Kriterium 29. Geschrieben wird ueber {@link LeagueRepository#addMember}
     * statt ueber {@code save(league)}: Der gelesene Stand dient nur noch
     * dazu, die Liga zu finden und {@link League#join} die Fachregel pruefen
     * zu lassen — er wird nicht zurueckgeschrieben. Sonst ueberschreiben
     * sich zwei Beitritte, die denselben Stand gelesen haben, gegenseitig
     * (nachgewiesen in {@code NebenlaeufigeSchreibzugriffeTest}); anders als
     * bei den Live-Wetten gibt es hier keinen Raum-Thread, der das
     * ausschliesst.
     */
    @Override
    public void joinLeague(EmailAddress account, LeagueCode code) {
        League league = leagues.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Unbekannter Liga-Code: " + code));
        Instant now = clock.instant();
        league.join(account, now);
        leagues.addMember(league.getId(), Membership.of(account, now));
    }

    /** Kriterium 34, dasselbe Vorgehen wie beim Beitritt. */
    @Override
    public void leaveLeague(EmailAddress account, LeagueId leagueId) {
        League league = leagueOrThrow(leagueId);
        league.leave(account);
        leagues.removeMember(league.getId(), account);
    }

    @Override
    public List<Standings.Entry> seasonStandings(LeagueId leagueId) {
        League league = leagueOrThrow(leagueId);
        return standingsFor(league, games.findBySeason(league.getSeason()));
    }

    @Override
    public List<Standings.Entry> matchdayStandings(LeagueId leagueId, Matchday matchday) {
        League league = leagueOrThrow(leagueId);
        return standingsFor(league, games.findByMatchday(matchday));
    }

    @Override
    public List<Standings.Entry> seasonStandingsThroughMatchday(LeagueId leagueId, Matchday matchday) {
        League league = leagueOrThrow(leagueId);
        List<Game> gamesThroughMatchday = games.findBySeason(league.getSeason()).stream()
                .filter(g -> g.getMatchday().week() <= matchday.week())
                .toList();
        return standingsFor(league, gamesThroughMatchday);
    }

    @Override
    public List<League> myLeagues(EmailAddress account) {
        return leagues.findByMember(account);
    }

    @Override
    public Optional<League> league(EmailAddress requester, LeagueId leagueId) {
        return leagues.findById(leagueId).filter(league -> league.isMember(requester));
    }

    private League leagueOrThrow(LeagueId leagueId) {
        return leagues.findById(leagueId).orElseThrow(() -> new NoSuchElementException("Unbekannte Liga: " + leagueId));
    }

    private List<Standings.Entry> standingsFor(League league, List<Game> scopedGames) {
        List<Game> finalGames = scopedGames.stream().filter(g -> g.getStatus() == GameStatus.FINAL).toList();

        List<Standings.ScoredGame> scoredGames = finalGames.stream()
                .map(g -> new Standings.ScoredGame(g.getId(), requireScore(g)))
                .toList();

        Map<GameId, Map<EmailAddress, GameScore>> predictionsByGame = new HashMap<>();
        for (Game g : finalGames) {
            predictionsByGame.put(g.getId(), predictions.findByGame(g.getId()).stream()
                    .collect(Collectors.toMap(p -> p.getId().accountEmail(), Prediction::getScore)));
        }

        List<Standings.Member> members = league.getMembers().stream()
                .map(m -> new Standings.Member(m.getAccountEmail(), displayNameOf(m.getAccountEmail())))
                .toList();

        return Standings.compute(members, scoredGames, (email, gameId) ->
                Optional.ofNullable(predictionsByGame.getOrDefault(gameId, Map.of()).get(email)));
    }

    private static GameScore requireScore(Game game) {
        GameScore score = game.getScore();
        if (score == null) {
            throw new IllegalStateException("Als FINAL markiertes Spiel ohne Ergebnis: " + game.getId());
        }
        return score;
    }

    private DisplayName displayNameOf(EmailAddress email) {
        return accounts.findByEmail(email).map(Account::getDisplayName).orElse(DisplayName.of("?"));
    }
}
