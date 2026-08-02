package de.fourteen.watchparty.domain.service;

import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Params;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PointsDelta;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementTest {

    private static final Params PARAMS = Params.DEFAULT;
    private static final OutcomeId TD = OutcomeId.of("td");
    private static final OutcomeId PUNT = OutcomeId.of("punt");

    // --- Kuerzel, damit die Wettbilder lesbar bleiben ------------------------

    private static PlayerId spieler(String id) {
        return PlayerId.of(id);
    }

    private static Pick pick(String playerId, OutcomeId outcome, int stake) {
        return new Pick(PlayerId.of(playerId), outcome, Points.of(stake));
    }

    private static PointsDelta delta(int value) {
        return PointsDelta.of(value);
    }

    @Test
    void normalfallVerteiltPoolNachAnteilenUndZiehtVerliererAb() {
        List<Pick> picks = List.of(
                pick("a", TD, 100),
                pick("b", TD, 50),
                pick("c", PUNT, 25));

        Map<PlayerId, PointsDelta> deltas = Settlement.settle(picks, Set.of(), Map.of(), TD, PARAMS).deltas();

        assertThat(deltas.get(spieler("a"))).isEqualTo(delta(17));
        assertThat(deltas.get(spieler("b"))).isEqualTo(delta(8));
        assertThat(deltas.get(spieler("c"))).isEqualTo(delta(-25));
        assertThat(PointsDelta.sumIsZero(deltas.values())).isTrue();
    }

    @Test
    void pushGibtEinsaetzeZurueckUndVerteiltStrafenAnAlleTipper() {
        List<Pick> picks = List.of(
                pick("a", PUNT, 50),
                pick("b", PUNT, 25));
        Map<PlayerId, Points> balances = Map.of(spieler("d"), Points.of(1000));

        Map<PlayerId, PointsDelta> deltas =
                Settlement.settle(picks, Set.of(spieler("d")), balances, TD, PARAMS).deltas();

        assertThat(deltas.get(spieler("a"))).isEqualTo(delta(17));
        assertThat(deltas.get(spieler("b"))).isEqualTo(delta(8));
        assertThat(deltas.get(spieler("d"))).isEqualTo(delta(-25));
        assertThat(PointsDelta.sumIsZero(deltas.values())).isTrue();
    }

    @Test
    void niemandTipptAnnulliertDieRundeOhneStrafenUndOhneAuszahlung() {
        Map<PlayerId, PointsDelta> deltas = Settlement.settle(
                List.of(),
                Set.of(spieler("a"), spieler("b")),
                Map.of(spieler("a"), Points.of(1000), spieler("b"), Points.of(1000)),
                TD, PARAMS).deltas();

        assertThat(deltas).isEmpty();
    }

    @Test
    void alleTippenDenselbenRichtigenAusgangErgibtNettoNull() {
        List<Pick> picks = List.of(
                pick("a", TD, 25),
                pick("b", TD, 25));

        Map<PlayerId, PointsDelta> deltas = Settlement.settle(picks, Set.of(), Map.of(), TD, PARAMS).deltas();

        assertThat(deltas.get(spieler("a"))).isEqualTo(PointsDelta.NONE);
        assertThat(deltas.get(spieler("b"))).isEqualTo(PointsDelta.NONE);
    }

    @Test
    void spielerMitNullPunktenGehtAllInUndGewinntEchtePunkte() {
        List<Pick> picks = List.of(
                pick("a", TD, 0),
                pick("b", PUNT, 100));
        Map<PlayerId, Points> balances = Map.of(spieler("a"), Points.ZERO);

        Map<PlayerId, PointsDelta> deltas = Settlement.settle(picks, Set.of(), balances, TD, PARAMS).deltas();

        assertThat(deltas.get(spieler("a"))).isEqualTo(delta(100));
        assertThat(deltas.get(spieler("b"))).isEqualTo(delta(-100));
        assertThat(PointsDelta.sumIsZero(deltas.values())).isTrue();
    }

    @Test
    void mindestAnteilGreiftBeiEinsatzUnterDemMinimum() {
        List<Pick> picks = List.of(
                pick("a", TD, 10),
                pick("b", TD, 25),
                pick("c", PUNT, 50));

        Map<PlayerId, PointsDelta> deltas = Settlement.settle(picks, Set.of(), Map.of(), TD, PARAMS).deltas();

        // a hat nur 10 gesetzt, zaehlt aber wie b mit dem Mindesteinsatz 25 (7.1).
        assertThat(deltas.get(spieler("a"))).isEqualTo(delta(33));
        assertThat(deltas.get(spieler("b"))).isEqualTo(delta(17));
        assertThat(deltas.get(spieler("c"))).isEqualTo(delta(-50));
        assertThat(PointsDelta.sumIsZero(deltas.values())).isTrue();
    }

    @Test
    void restVerteilungNachGroesstenRestenTrifftExaktDenPool() {
        List<Pick> picks = List.of(
                pick("a", TD, 25),
                pick("b", TD, 25),
                pick("c", TD, 25));
        Map<PlayerId, Points> balances = Map.of(spieler("d"), Points.of(1000));

        Map<PlayerId, PointsDelta> deltas =
                Settlement.settle(picks, Set.of(spieler("d")), balances, TD, PARAMS).deltas();

        assertThat(deltas.get(spieler("a"))).isEqualTo(delta(9));
        assertThat(deltas.get(spieler("b"))).isEqualTo(delta(8));
        assertThat(deltas.get(spieler("c"))).isEqualTo(delta(8));
        assertThat(deltas.get(spieler("d"))).isEqualTo(delta(-25));
        assertThat(PointsDelta.sumIsZero(deltas.values())).isTrue();
    }

    @Test
    void strafeWirdAufDenKontostandGekappt() {
        List<Pick> picks = List.of(pick("a", TD, 25));
        Map<PlayerId, Points> balances = Map.of(spieler("d"), Points.of(10));

        Map<PlayerId, PointsDelta> deltas =
                Settlement.settle(picks, Set.of(spieler("d")), balances, TD, PARAMS).deltas();

        assertThat(deltas.get(spieler("d"))).isEqualTo(delta(-10));
        assertThat(deltas.get(spieler("a"))).isEqualTo(delta(10));
        assertThat(PointsDelta.sumIsZero(deltas.values())).isTrue();
    }

    /**
     * Der Pool zaehlt nur, was wirklich eingesammelt wurde (Anforderung 8.1):
     * Der Nicht-Tipper hat 10 Punkte, die Strafe waere 25 — in den Pool gehen
     * 10. Diese Regel stand frueher zusaetzlich im Actor; der Test haelt fest,
     * dass sie jetzt nur noch aus einer Rechnung kommt.
     */
    @Test
    void poolZaehltNurTatsaechlichEingesammelteStrafen() {
        List<Pick> picks = List.of(pick("a", TD, 25));
        Map<PlayerId, Points> balances = Map.of(spieler("d"), Points.of(10));

        Settlement.Result result = Settlement.settle(picks, Set.of(spieler("d")), balances, TD, PARAMS);

        assertThat(result.pool()).isEqualTo(Points.of(35));
        assertThat(result.annulled()).isFalse();
    }

    @Test
    void ohneEinenEinzigenTippIstDieRundeAnnulliertUndDerPoolLeer() {
        Settlement.Result result = Settlement.settle(
                List.of(),
                Set.of(spieler("a"), spieler("b")),
                Map.of(spieler("a"), Points.of(1000), spieler("b"), Points.of(1000)),
                TD, PARAMS);

        assertThat(result.annulled()).isTrue();
        assertThat(result.pool()).isEqualTo(Points.ZERO);
        assertThat(result.deltas()).isEmpty();
    }

    /**
     * 8.2: Beim Push gehen die Einsaetze zurueck, verteilt werden nur die
     * Strafen — der Pool bleibt trotzdem Einsaetze plus Strafen, weil er
     * beschreibt, was hineingeflossen ist (Anforderung 7).
     */
    @Test
    void poolBeimPushEnthaeltDieEinsaetzeObwohlSieZurueckgehen() {
        List<Pick> picks = List.of(
                pick("a", PUNT, 50),
                pick("b", PUNT, 25));
        Map<PlayerId, Points> balances = Map.of(spieler("d"), Points.of(1000));

        Settlement.Result result = Settlement.settle(picks, Set.of(spieler("d")), balances, TD, PARAMS);

        assertThat(result.pool()).isEqualTo(Points.of(100));
        assertThat(result.annulled()).isFalse();
    }

    /**
     * Invariante 5: Punkte entstehen und verschwinden nie, nur Umverteilung.
     * Statt sie nur zu beschreiben, wird sie hier ueber viele zufaellige
     * Wettbilder direkt geprueft.
     *
     * Zusaetzlich: Kein Konto wird negativ. Das laesst sich seit der
     * Umstellung auf {@code Points} gar nicht mehr per Zusicherung pruefen —
     * {@code balance.apply(delta)} wuerde von selbst werfen. Genau das ist
     * hier der Test.
     */
    @Test
    void summeAllerDeltasIstImmerExaktNullUndKeinKontoWirdNegativ() {
        Random random = new Random(42);
        List<OutcomeId> outcomes = List.of(TD, PUNT, OutcomeId.of("turnover"));

        for (int run = 0; run < 500; run++) {
            int playerCount = 1 + random.nextInt(8);
            List<Pick> picks = new ArrayList<>();
            Map<PlayerId, Points> balances = new HashMap<>();
            Set<PlayerId> nonPickers = new HashSet<>();

            for (int i = 0; i < playerCount; i++) {
                PlayerId playerId = spieler("p" + i);
                int balance = random.nextInt(500);
                balances.put(playerId, Points.of(balance));

                if (random.nextBoolean()) {
                    int minStake = PARAMS.minStake().value();
                    int stake = balance < minStake
                            ? balance
                            : minStake + random.nextInt(Math.max(1, balance - minStake + 1));
                    picks.add(new Pick(playerId, outcomes.get(random.nextInt(outcomes.size())), Points.of(stake)));
                } else {
                    nonPickers.add(playerId);
                }
            }

            OutcomeId winningOutcome = outcomes.get(random.nextInt(outcomes.size()));
            Settlement.Result result = Settlement.settle(picks, nonPickers, balances, winningOutcome, PARAMS);

            assertThat(PointsDelta.sumIsZero(result.deltas().values()))
                    .as("Lauf %d: picks=%s nonPickers=%s balances=%s winner=%s", run, picks, nonPickers, balances,
                            winningOutcome)
                    .isTrue();
            assertThat(result.annulled())
                    .as("Lauf %d ist genau dann annulliert, wenn niemand getippt hat", run)
                    .isEqualTo(picks.isEmpty());

            // Wuerde ein Konto negativ, wirft Points.apply hier.
            result.deltas().forEach((playerId, delta) ->
                    balances.getOrDefault(playerId, Points.ZERO).apply(delta));
        }
    }
}
