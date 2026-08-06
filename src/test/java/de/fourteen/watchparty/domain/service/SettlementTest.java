package de.fourteen.watchparty.domain.service;

import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Params;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PointsDelta;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
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

    // summeAllerDeltasIstImmerExaktNullUndKeinKontoWirdNegativ entfaellt hier:
    // Der handgeschriebene Zufallstest (new Random(42)) ist durch echte
    // Property-Tests in SettlementPropertyTest abgeloest (docs/teststrategie.md,
    // Abschnitt 4) -- generiert ueber den ganzen Eingaberaum statt eines festen
    // Seeds, mit automatischem Shrinking bei einem Fehlschlag.
}
