package de.fourteenit.watchparty.room;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementTest {

    private static final Params PARAMS = new Params(25, 25);

    @Test
    void normalfallVerteiltPoolNachAnteilenUndZiehtVerliererAb() {
        List<Bet> bets = List.of(
                new Bet("a", "td", 100),
                new Bet("b", "td", 50),
                new Bet("c", "punt", 25));

        Map<String, Integer> deltas = Settlement.settle(bets, Set.of(), Map.of(), "td", PARAMS);

        assertThat(deltas.get("a")).isEqualTo(17);
        assertThat(deltas.get("b")).isEqualTo(8);
        assertThat(deltas.get("c")).isEqualTo(-25);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void pushGibtEinsaetzeZurueckUndVerteiltStrafenAnAlleTipper() {
        List<Bet> bets = List.of(
                new Bet("a", "punt", 50),
                new Bet("b", "punt", 25));
        Map<String, Integer> balances = Map.of("d", 1000);

        Map<String, Integer> deltas = Settlement.settle(bets, Set.of("d"), balances, "td", PARAMS);

        assertThat(deltas.get("a")).isEqualTo(17);
        assertThat(deltas.get("b")).isEqualTo(8);
        assertThat(deltas.get("d")).isEqualTo(-25);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void niemandTipptAnnulliertDieRundeOhneStrafenUndOhneAuszahlung() {
        Map<String, Integer> deltas = Settlement.settle(
                List.of(), Set.of("a", "b"), Map.of("a", 1000, "b", 1000), "td", PARAMS);

        assertThat(deltas).isEmpty();
    }

    @Test
    void alleTippenDenselbenRichtigenAusgangErgibtNettoNull() {
        List<Bet> bets = List.of(
                new Bet("a", "td", 25),
                new Bet("b", "td", 25));

        Map<String, Integer> deltas = Settlement.settle(bets, Set.of(), Map.of(), "td", PARAMS);

        assertThat(deltas.get("a")).isZero();
        assertThat(deltas.get("b")).isZero();
    }

    @Test
    void spielerMitNullPunktenGehtAllInUndGewinntEchtePunkte() {
        List<Bet> bets = List.of(
                new Bet("a", "td", 0),
                new Bet("b", "punt", 100));
        Map<String, Integer> balances = Map.of("a", 0);

        Map<String, Integer> deltas = Settlement.settle(bets, Set.of(), balances, "td", PARAMS);

        assertThat(deltas.get("a")).isEqualTo(100);
        assertThat(deltas.get("b")).isEqualTo(-100);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void mindestAnteilGreiftBeiEinsatzUnterDemMinimum() {
        List<Bet> bets = List.of(
                new Bet("a", "td", 10),
                new Bet("b", "td", 25),
                new Bet("c", "punt", 50));

        Map<String, Integer> deltas = Settlement.settle(bets, Set.of(), Map.of(), "td", PARAMS);

        // a hat nur 10 gesetzt, zaehlt aber wie b mit dem Mindesteinsatz 25 (7.1).
        assertThat(deltas.get("a")).isEqualTo(33);
        assertThat(deltas.get("b")).isEqualTo(17);
        assertThat(deltas.get("c")).isEqualTo(-50);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void restVerteilungNachGroesstenRestenTrifftExaktDenPool() {
        List<Bet> bets = List.of(
                new Bet("a", "td", 25),
                new Bet("b", "td", 25),
                new Bet("c", "td", 25));
        Map<String, Integer> balances = Map.of("d", 1000);

        Map<String, Integer> deltas = Settlement.settle(bets, Set.of("d"), balances, "td", PARAMS);

        assertThat(deltas.get("a")).isEqualTo(9);
        assertThat(deltas.get("b")).isEqualTo(8);
        assertThat(deltas.get("c")).isEqualTo(8);
        assertThat(deltas.get("d")).isEqualTo(-25);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void strafeWirdAufDenKontostandGekappt() {
        List<Bet> bets = List.of(new Bet("a", "td", 25));
        Map<String, Integer> balances = Map.of("d", 10);

        Map<String, Integer> deltas = Settlement.settle(bets, Set.of("d"), balances, "td", PARAMS);

        assertThat(deltas.get("d")).isEqualTo(-10);
        assertThat(deltas.get("a")).isEqualTo(10);
        assertThat(sumOf(deltas)).isZero();
    }

    /**
     * Invariante 5: Punkte entstehen und verschwinden nie, nur Umverteilung.
     * Statt sie nur zu beschreiben, wird sie hier ueber viele zufaellige
     * Wettbilder direkt geprueft.
     */
    @Test
    void summeAllerDeltasIstImmerExaktNull() {
        Random random = new Random(42);
        List<String> outcomes = List.of("td", "punt", "turnover");

        for (int run = 0; run < 500; run++) {
            int playerCount = 1 + random.nextInt(8);
            List<Bet> bets = new java.util.ArrayList<>();
            Map<String, Integer> balances = new java.util.HashMap<>();
            java.util.Set<String> nonBettors = new java.util.HashSet<>();

            for (int i = 0; i < playerCount; i++) {
                String playerId = "p" + i;
                int balance = random.nextInt(500);
                balances.put(playerId, balance);

                boolean bets_ = random.nextBoolean();
                if (bets_) {
                    int minStake = PARAMS.minStake();
                    int stake = balance < minStake ? balance : minStake + random.nextInt(Math.max(1, balance - minStake + 1));
                    bets.add(new Bet(playerId, outcomes.get(random.nextInt(outcomes.size())), stake));
                } else {
                    nonBettors.add(playerId);
                }
            }

            String winningOutcome = outcomes.get(random.nextInt(outcomes.size()));
            Map<String, Integer> deltas = Settlement.settle(bets, nonBettors, balances, winningOutcome, PARAMS);

            assertThat(sumOf(deltas))
                    .as("Lauf %d: bets=%s nonBettors=%s balances=%s winner=%s", run, bets, nonBettors, balances,
                            winningOutcome)
                    .isZero();
        }
    }

    private static int sumOf(Map<String, Integer> deltas) {
        return deltas.values().stream().mapToInt(Integer::intValue).sum();
    }
}
