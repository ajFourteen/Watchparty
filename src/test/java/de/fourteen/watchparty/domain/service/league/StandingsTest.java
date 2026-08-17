package de.fourteen.watchparty.domain.service.league;

import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class StandingsTest {

    private static final EmailAddress ANNA = EmailAddress.of("anna@example.org");
    private static final EmailAddress BEN = EmailAddress.of("ben@example.org");
    private static final EmailAddress CEM = EmailAddress.of("cem@example.org");
    private static final GameId GAME_1 = GameId.of("1");
    private static final GameId GAME_2 = GameId.of("2");

    private static Standings.Member member(EmailAddress email, String name) {
        return new Standings.Member(email, DisplayName.of(name));
    }

    private static BiFunction<EmailAddress, GameId, Optional<GameScore>> predictionsFrom(
            Map<EmailAddress, Map<GameId, GameScore>> data) {
        return (email, gameId) -> Optional.ofNullable(data.getOrDefault(email, Map.of()).get(gameId));
    }

    @Test
    @Anforderung("13.6-e")
    void summiertWertungspunkteUeberMehrereSpiele() {
        List<Standings.Member> members = List.of(member(ANNA, "Anna"));
        List<Standings.ScoredGame> games = List.of(
                new Standings.ScoredGame(GAME_1, GameScore.of(24, 17)),
                new Standings.ScoredGame(GAME_2, GameScore.of(20, 20)));
        var predictions = predictionsFrom(Map.of(ANNA, Map.of(
                GAME_1, GameScore.of(24, 17),
                GAME_2, GameScore.of(17, 20))));

        List<Standings.Entry> entries = Standings.compute(members, games, predictions);

        assertThat(entries.get(0).totalPoints().value()).isEqualTo(6 + 0);
    }

    @Test
    @Anforderung("13.6-f")
    void einNichtGetipptesSpielBringtNullPunkteOhneStrafe() {
        List<Standings.Member> members = List.of(member(ANNA, "Anna"));
        List<Standings.ScoredGame> games = List.of(new Standings.ScoredGame(GAME_1, GameScore.of(24, 17)));
        var predictions = predictionsFrom(Map.of());

        List<Standings.Entry> entries = Standings.compute(members, games, predictions);

        assertThat(entries.get(0).totalPoints().value()).isZero();
    }

    @Test
    @Anforderung("13.6-e")
    void sortiertAbsteigendNachGesamtpunktzahl() {
        List<Standings.Member> members = List.of(member(ANNA, "Anna"), member(BEN, "Ben"));
        List<Standings.ScoredGame> games = List.of(new Standings.ScoredGame(GAME_1, GameScore.of(24, 17)));
        var predictions = predictionsFrom(Map.of(
                ANNA, Map.of(GAME_1, GameScore.of(24, 17)),
                BEN, Map.of(GAME_1, GameScore.of(27, 20))));

        List<Standings.Entry> entries = Standings.compute(members, games, predictions);

        assertThat(entries).extracting(Standings.Entry::email).containsExactly(ANNA, BEN);
        assertThat(entries.get(0).rank()).isEqualTo(1);
        assertThat(entries.get(1).rank()).isEqualTo(2);
    }

    @Test
    @Anforderung("13.6-g")
    void beiGleicherPunktzahlEntscheidetZuerstDieZahlDerExaktenErgebnisse() {
        // Beide erzielen 6 Punkte insgesamt -- Anna durch ein exaktes Ergebnis
        // (1 Treffer), Ben durch zwei richtige Tendenzen ohne Treffer.
        List<Standings.Member> members = List.of(member(ANNA, "Anna"), member(BEN, "Ben"));
        List<Standings.ScoredGame> games = List.of(
                new Standings.ScoredGame(GAME_1, GameScore.of(24, 17)),
                new Standings.ScoredGame(GAME_2, GameScore.of(21, 14)));
        var predictions = predictionsFrom(Map.of(
                ANNA, Map.of(GAME_1, GameScore.of(24, 17), GAME_2, GameScore.of(10, 20)),
                BEN, Map.of(GAME_1, GameScore.of(30, 10), GAME_2, GameScore.of(35, 10))));

        List<Standings.Entry> entries = Standings.compute(members, games, predictions);

        assertThat(entries.get(0).email()).isEqualTo(ANNA);
        assertThat(entries.get(0).totalPoints().value()).isEqualTo(entries.get(1).totalPoints().value());
        assertThat(entries.get(0).exactCount()).isGreaterThan(entries.get(1).exactCount());
        assertThat(entries.get(0).rank()).isEqualTo(1);
        assertThat(entries.get(1).rank()).isEqualTo(2);
    }

    @Test
    @Anforderung("13.6-g")
    void beiGleicherPunktzahlUndGleicherExaktzahlEntscheidetDieZahlDerRichtigenTendenzen() {
        // Fuenf Spiele mit identischer Form (Heimsieg um 7). Anna trifft bei drei
        // davon Tendenz UND Abstand (5 Punkte je Spiel, macht 15), Ben trifft bei
        // allen fuenf nur die Tendenz (3 Punkte je Spiel, macht ebenfalls 15) --
        // gleiche Summe, keiner von beiden ein exaktes Ergebnis, aber
        // unterschiedliche Anzahl richtiger Tendenzen.
        List<Standings.Member> members = List.of(member(ANNA, "Anna"), member(BEN, "Ben"));
        List<Standings.ScoredGame> games = List.of(
                new Standings.ScoredGame(GameId.of("1"), GameScore.of(24, 17)),
                new Standings.ScoredGame(GameId.of("2"), GameScore.of(24, 17)),
                new Standings.ScoredGame(GameId.of("3"), GameScore.of(24, 17)),
                new Standings.ScoredGame(GameId.of("4"), GameScore.of(24, 17)),
                new Standings.ScoredGame(GameId.of("5"), GameScore.of(24, 17)));

        Map<GameId, GameScore> annasTipps = new HashMap<>();
        Map<GameId, GameScore> bensTipps = new HashMap<>();
        for (Standings.ScoredGame game : games) {
            bensTipps.put(game.gameId(), GameScore.of(40, 10)); // richtige Tendenz, falscher Abstand (3-Score-Game statt 1-Score-Game)
        }
        annasTipps.put(GameId.of("1"), GameScore.of(21, 14)); // richtige Tendenz UND richtiger Abstand (5)
        annasTipps.put(GameId.of("2"), GameScore.of(21, 14));
        annasTipps.put(GameId.of("3"), GameScore.of(21, 14));

        var predictions = predictionsFrom(Map.of(ANNA, annasTipps, BEN, bensTipps));

        List<Standings.Entry> entries = Standings.compute(members, games, predictions);

        assertThat(entries.get(0).totalPoints().value()).isEqualTo(15);
        assertThat(entries.get(1).totalPoints().value()).isEqualTo(15);
        assertThat(entries.get(0).exactCount()).isZero();
        assertThat(entries.get(1).exactCount()).isZero();
        assertThat(entries.get(0).email()).isEqualTo(BEN);
        assertThat(entries.get(0).correctTendencyCount()).isEqualTo(5);
        assertThat(entries.get(1).email()).isEqualTo(ANNA);
        assertThat(entries.get(1).correctTendencyCount()).isEqualTo(3);
        assertThat(entries.get(0).rank()).isEqualTo(1);
        assertThat(entries.get(1).rank()).isEqualTo(2);
    }

    @Test
    @Anforderung("13.6-g")
    void beiVollstaendigemGleichstandTeilenSichDieTipperDenPlatz() {
        List<Standings.Member> members = List.of(member(ANNA, "Anna"), member(BEN, "Ben"), member(CEM, "Cem"));
        List<Standings.ScoredGame> games = List.of(new Standings.ScoredGame(GAME_1, GameScore.of(24, 17)));
        var predictions = predictionsFrom(Map.of(
                ANNA, Map.of(GAME_1, GameScore.of(24, 17)),
                BEN, Map.of(GAME_1, GameScore.of(24, 17)),
                CEM, Map.of(GAME_1, GameScore.of(0, 1))));

        List<Standings.Entry> entries = Standings.compute(members, games, predictions);

        assertThat(entries).extracting(Standings.Entry::rank).containsExactly(1, 1, 3);
    }

    @Test
    void ohneGewerteteSpieleHabenAlleNullPunkteUndTeilenSichPlatzEins() {
        List<Standings.Member> members = List.of(member(ANNA, "Anna"), member(BEN, "Ben"));

        List<Standings.Entry> entries = Standings.compute(members, List.of(), predictionsFrom(Map.of()));

        assertThat(entries).extracting(Standings.Entry::rank).containsExactly(1, 1);
    }
}
