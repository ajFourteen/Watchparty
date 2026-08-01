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
        List<Pick> picks = List.of(
                new Pick("a", "td", 100),
                new Pick("b", "td", 50),
                new Pick("c", "punt", 25));

        Map<String, Integer> deltas = Settlement.settle(picks, Set.of(), Map.of(), "td", PARAMS);

        assertThat(deltas.get("a")).isEqualTo(17);
        assertThat(deltas.get("b")).isEqualTo(8);
        assertThat(deltas.get("c")).isEqualTo(-25);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void pushGibtEinsaetzeZurueckUndVerteiltStrafenAnAlleTipper() {
        List<Pick> picks = List.of(
                new Pick("a", "punt", 50),
                new Pick("b", "punt", 25));
        Map<String, Integer> balances = Map.of("d", 1000);

        Map<String, Integer> deltas = Settlement.settle(picks, Set.of("d"), balances, "td", PARAMS);

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
        List<Pick> picks = List.of(
                new Pick("a", "td", 25),
                new Pick("b", "td", 25));

        Map<String, Integer> deltas = Settlement.settle(picks, Set.of(), Map.of(), "td", PARAMS);

        assertThat(deltas.get("a")).isZero();
        assertThat(deltas.get("b")).isZero();
    }

    @Test
    void spielerMitNullPunktenGehtAllInUndGewinntEchtePunkte() {
        List<Pick> picks = List.of(
                new Pick("a", "td", 0),
                new Pick("b", "punt", 100));
        Map<String, Integer> balances = Map.of("a", 0);

        Map<String, Integer> deltas = Settlement.settle(picks, Set.of(), balances, "td", PARAMS);

        assertThat(deltas.get("a")).isEqualTo(100);
        assertThat(deltas.get("b")).isEqualTo(-100);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void mindestAnteilGreiftBeiEinsatzUnterDemMinimum() {
        List<Pick> picks = List.of(
                new Pick("a", "td", 10),
                new Pick("b", "td", 25),
                new Pick("c", "punt", 50));

        Map<String, Integer> deltas = Settlement.settle(picks, Set.of(), Map.of(), "td", PARAMS);

        // a hat nur 10 gesetzt, zaehlt aber wie b mit dem Mindesteinsatz 25 (7.1).
        assertThat(deltas.get("a")).isEqualTo(33);
        assertThat(deltas.get("b")).isEqualTo(17);
        assertThat(deltas.get("c")).isEqualTo(-50);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void restVerteilungNachGroesstenRestenTrifftExaktDenPool() {
        List<Pick> picks = List.of(
                new Pick("a", "td", 25),
                new Pick("b", "td", 25),
                new Pick("c", "td", 25));
        Map<String, Integer> balances = Map.of("d", 1000);

        Map<String, Integer> deltas = Settlement.settle(picks, Set.of("d"), balances, "td", PARAMS);

        assertThat(deltas.get("a")).isEqualTo(9);
        assertThat(deltas.get("b")).isEqualTo(8);
        assertThat(deltas.get("c")).isEqualTo(8);
        assertThat(deltas.get("d")).isEqualTo(-25);
        assertThat(sumOf(deltas)).isZero();
    }

    @Test
    void strafeWirdAufDenKontostandGekappt() {
        List<Pick> picks = List.of(new Pick("a", "td", 25));
        Map<String, Integer> balances = Map.of("d", 10);

        Map<String, Integer> deltas = Settlement.settle(picks, Set.of("d"), balances, "td", PARAMS);

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
            List<Pick> picks = new java.util.ArrayList<>();
            Map<String, Integer> balances = new java.util.HashMap<>();
            java.util.Set<String> nonPickers = new java.util.HashSet<>();

            for (int i = 0; i < playerCount; i++) {
                String playerId = "p" + i;
                int balance = random.nextInt(500);
                balances.put(playerId, balance);

                boolean picks_ = random.nextBoolean();
                if (picks_) {
                    int minStake = PARAMS.minStake();
                    int stake = balance < minStake ? balance : minStake + random.nextInt(Math.max(1, balance - minStake + 1));
                    picks.add(new Pick(playerId, outcomes.get(random.nextInt(outcomes.size())), stake));
                } else {
                    nonPickers.add(playerId);
                }
            }

            String winningOutcome = outcomes.get(random.nextInt(outcomes.size()));
            Map<String, Integer> deltas = Settlement.settle(picks, nonPickers, balances, winningOutcome, PARAMS);

            assertThat(sumOf(deltas))
                    .as("Lauf %d: picks=%s nonPickers=%s balances=%s winner=%s", run, picks, nonPickers, balances,
                            winningOutcome)
                    .isZero();
        }
    }

    private static int sumOf(Map<String, Integer> deltas) {
        return deltas.values().stream().mapToInt(Integer::intValue).sum();
    }
}
