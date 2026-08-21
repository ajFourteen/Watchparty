package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.league.FakeAccountRepository;
import de.fourteen.watchparty.application.league.FakeGameRepository;
import de.fourteen.watchparty.application.league.FakeLeagueRepository;
import de.fourteen.watchparty.application.league.FakePredictionRepository;
import de.fourteen.watchparty.application.league.LeagueService;
import de.fourteen.watchparty.application.league.port.in.LeagueCommands;
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
import de.fourteen.watchparty.domain.model.league.Prediction;
import de.fourteen.watchparty.domain.model.league.PredictionId;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;
import de.fourteen.watchparty.domain.service.league.Standings;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port-to-Port-Stufe (docs/teststrategie.md, Abschnitt 2.2) fuer Ligen
 * (Kapitel 13.6): Eingang ist {@link LeagueService} als Umsetzung von
 * {@link LeagueCommands}, Ausgaenge sind handgeschriebene Test Doubles
 * (ADR-025). Ergebnistipps werden hier direkt ins Test Double gelegt, ohne
 * den Umweg ueber {@code PredictionService} — dessen Anstoss-Pruefung ist
 * Gegenstand von {@code TippenScenarioTest}, hier zaehlt nur die Wertung.
 */
public class LigaStufen extends DeutscheStufe<LigaStufen> {

    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");

    private final FakeClock clock = new FakeClock(NOW);
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeGameRepository games = new FakeGameRepository();
    private final FakePredictionRepository predictions = new FakePredictionRepository();
    private final FakeLeagueRepository leagues = new FakeLeagueRepository();
    private final LeagueCommands liga = new LeagueService(clock, leagues, games, predictions, accounts);

    private final Map<String, LeagueId> ligaIdVon = new LinkedHashMap<>();
    private List<Standings.Entry> letzteRangliste = List.of();

    public LigaStufen einKontoMitNamenExistiertFuer(String name, String email) {
        accounts.save(Account.of(EmailAddress.of(email), DisplayName.of(name), clock.instant()));
        return self();
    }

    public LigaStufen legtEineLigaAnFuerDieSaisonAls(String ligaSchluessel, String ligaName, int seasonYear, String managerEmail) {
        LeagueId id = liga.createLeague(EmailAddress.of(managerEmail), LeagueName.of(ligaName), SeasonId.of(seasonYear));
        ligaIdVon.put(ligaSchluessel, id);
        return self();
    }

    public LigaStufen trittDerLigaBei(String email, String ligaSchluessel) {
        liga.joinLeague(EmailAddress.of(email), codeVon(ligaSchluessel));
        return self();
    }

    public LigaStufen verlaesstDieLiga(String email, String ligaSchluessel) {
        liga.leaveLeague(EmailAddress.of(email), ligaIdVon.get(ligaSchluessel));
        return self();
    }

    public LigaStufen einSpielIstAmSpieltagBeendetMit(String gameId, int week, int heim, int gast) {
        games.save(Game.of(GameId.of(gameId), Matchday.of(SeasonId.of(2026), week), HOME, AWAY,
                NOW.minusSeconds(3600), GameStatus.FINAL, GameScore.of(heim, gast), false));
        return self();
    }

    public LigaStufen korrigiertDasErgebnisDesSpielsAuf(String gameId, int heim, int gast) {
        Game game = games.findById(GameId.of(gameId)).orElseThrow();
        game.applyManualResult(GameScore.of(heim, gast));
        games.save(game);
        return self();
    }

    public LigaStufen tipptFuerDasSpiel(String email, String gameId, int heim, int gast) {
        predictions.save(Prediction.of(PredictionId.of(EmailAddress.of(email), GameId.of(gameId)), GameScore.of(heim, gast)));
        return self();
    }

    public LigaStufen ruftDieSaisonRanglisteDerLigaAb(String ligaSchluessel) {
        letzteRangliste = liga.seasonStandings(ligaIdVon.get(ligaSchluessel));
        return self();
    }

    public LigaStufen ruftDieSpieltagsRanglisteDerLigaAb(String ligaSchluessel, int week) {
        letzteRangliste = liga.matchdayStandings(ligaIdVon.get(ligaSchluessel), Matchday.of(SeasonId.of(2026), week));
        return self();
    }

    public LigaStufen ruftDieKumulierteRanglisteDerLigaBisSpieltagAb(String ligaSchluessel, int week) {
        letzteRangliste = liga.seasonStandingsThroughMatchday(ligaIdVon.get(ligaSchluessel), Matchday.of(SeasonId.of(2026), week));
        return self();
    }

    public LigaStufen zeigtInDerRanglisteGenauDieseKonten(String... emails) {
        assertThat(letzteRangliste).extracting(e -> e.email().value()).containsExactlyInAnyOrder(emails);
        return self();
    }

    public LigaStufen zeigtFuerDasKontoWertungspunkte(String email, int erwartet) {
        assertThat(eintragVon(email).totalPoints().value()).isEqualTo(erwartet);
        return self();
    }

    public LigaStufen istIstNochMitgliedDerLiga(String email, String ligaSchluessel) {
        League league = leagues.findById(ligaIdVon.get(ligaSchluessel)).orElseThrow();
        assertThat(league.isMember(EmailAddress.of(email))).isTrue();
        return self();
    }

    private Standings.Entry eintragVon(String email) {
        return letzteRangliste.stream()
                .filter(e -> e.email().equals(EmailAddress.of(email)))
                .findFirst()
                .orElseThrow(() -> new AssertionError(email + " sollte in der Rangliste stehen"));
    }

    private LeagueCode codeVon(String ligaSchluessel) {
        return leagues.findById(ligaIdVon.get(ligaSchluessel)).orElseThrow().getCode();
    }
}
