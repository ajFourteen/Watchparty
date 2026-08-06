package de.fourteen.watchparty.domain.service;

import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Params;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PointsDelta;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Allaussagen ueber {@link Settlement}, die ein Beispieltest prinzipiell
 * nicht treffen kann (docs/teststrategie.md, Abschnitt 4). Loest den
 * handgeschriebenen Zufallstest ({@code new Random(42)}) aus
 * {@code SettlementTest} ab -- generiert statt eines festen Seeds ueber den
 * ganzen Eingaberaum, mit automatischem Shrinking bei einem Fehlschlag.
 */
@UnitTest
class SettlementPropertyTest {

    private static final Params PARAMS = Params.DEFAULT;
    private static final OutcomeId TOUCHDOWN = OutcomeId.of("touchdown");
    private static final OutcomeId PUNT = OutcomeId.of("punt");

    @Provide
    Arbitrary<List<Integer>> einsaetze() {
        return Arbitraries.integers().between(0, 500).list().ofMaxSize(6);
    }

    @Provide
    Arbitrary<List<Integer>> kontostaende() {
        return Arbitraries.integers().between(0, 500).list().ofMaxSize(6);
    }

    @Provide
    Arbitrary<List<Integer>> mindestensEinGewinner() {
        return Arbitraries.integers().between(0, 500).list().ofMinSize(1).ofMaxSize(8);
    }

    /**
     * 2-d: Punkte entstehen und verschwinden nie. Die Summe aller Deltas ist
     * exakt null, und der Pool ist exakt die Summe der Einsaetze plus der
     * <em>tatsaechlich eingesammelten</em> Strafen -- egal wie das Wettbild
     * aussieht.
     */
    @Property
    @Anforderung("2-d")
    void summeAllerDeltasIstNullUndPoolIstEinsaetzePlusEingesammelteStrafen(
            @ForAll("einsaetze") List<Integer> gewinnerEinsaetze,
            @ForAll("einsaetze") List<Integer> verliererEinsaetze,
            @ForAll("kontostaende") List<Integer> nichtTipperKontostaende) {

        List<Pick> picks = new ArrayList<>();
        int i = 0;
        for (int einsatz : gewinnerEinsaetze) {
            picks.add(new Pick(PlayerId.of("g" + i++), TOUCHDOWN, Points.of(einsatz)));
        }
        for (int einsatz : verliererEinsaetze) {
            picks.add(new Pick(PlayerId.of("l" + i++), PUNT, Points.of(einsatz)));
        }

        Set<PlayerId> nichtTipper = new LinkedHashSet<>();
        Map<PlayerId, Points> kontostaende = new LinkedHashMap<>();
        int j = 0;
        for (int kontostand : nichtTipperKontostaende) {
            PlayerId id = PlayerId.of("n" + j++);
            nichtTipper.add(id);
            kontostaende.put(id, Points.of(kontostand));
        }

        Settlement.Result result = Settlement.settle(picks, nichtTipper, kontostaende, TOUCHDOWN, PARAMS);

        assertThat(PointsDelta.sumIsZero(result.deltas().values())).isTrue();

        // Kein Konto wird negativ (Invariante 5): die eingesammelte Strafe
        // ueberschreitet nie den Kontostand.
        for (PlayerId id : nichtTipper) {
            PointsDelta delta = result.deltas().get(id);
            int eingesammelt = delta == null ? 0 : -delta.value();
            assertThat(eingesammelt).isLessThanOrEqualTo(kontostaende.get(id).value());
        }

        if (picks.isEmpty()) {
            assertThat(result.annulled()).isTrue();
            assertThat(result.pool()).isEqualTo(Points.ZERO);
            return;
        }
        assertThat(result.annulled()).isFalse();

        int erwarteteEinsaetze = gewinnerEinsaetze.stream().mapToInt(Integer::intValue).sum()
                + verliererEinsaetze.stream().mapToInt(Integer::intValue).sum();
        int erwartetePenalties = 0;
        for (PlayerId id : nichtTipper) {
            erwartetePenalties += Math.min(PARAMS.penalty().value(), kontostaende.get(id).value());
        }
        assertThat(result.pool().value()).isEqualTo(erwarteteEinsaetze + erwartetePenalties);
    }

    /**
     * 2-c: Ein selten getippter Ausgang zahlt pro Gewinner mehr als ein
     * häufig getippter -- bei gleichem Einsatz pro Kopf und gleichem
     * Verlierer-Pool bekommt eine kleinere Gewinnergruppe pro Kopf
     * mindestens so viel wie eine größere.
     */
    @Property
    @Anforderung("2-c")
    void wenigerGewinnerBekommenProKopfMindestensSoVielWieMehrGewinner(
            @ForAll @IntRange(min = 1, max = 5) int wenigerGewinner,
            @ForAll @IntRange(min = 6, max = 12) int mehrGewinner,
            @ForAll @IntRange(min = 0, max = 500) int verliererEinsatz) {

        int proKopfBeiWenigen = deltaDesErstenGewinners(wenigerGewinner, verliererEinsatz);
        int proKopfBeiVielen = deltaDesErstenGewinners(mehrGewinner, verliererEinsatz);

        assertThat(proKopfBeiWenigen).isGreaterThanOrEqualTo(proKopfBeiVielen);
    }

    private int deltaDesErstenGewinners(int gewinnerAnzahl, int verliererEinsatz) {
        List<Pick> picks = new ArrayList<>();
        for (int i = 0; i < gewinnerAnzahl; i++) {
            picks.add(new Pick(PlayerId.of("g" + i), TOUCHDOWN, PARAMS.minStake()));
        }
        picks.add(new Pick(PlayerId.of("verlierer"), PUNT, Points.of(verliererEinsatz)));

        Settlement.Result result = Settlement.settle(picks, Set.of(), Map.of(), TOUCHDOWN, PARAMS);
        return result.deltas().get(PlayerId.of("g0")).value();
    }

    /**
     * 7.1: Anteil = max(Einsatz, Mindesteinsatz). Zwei Gewinner, die beide
     * unter dem Mindesteinsatz setzen, zaehlen deshalb gleich viel -- ihre
     * Auszahlung unterscheidet sich (bis auf Rundung) nur um die Differenz
     * ihrer tatsaechlichen Einsaetze, nie um ihren Anteil am Pool.
     */
    @Property
    @Anforderung("7.1")
    void zweiGewinnerUnterhalbDesMindesteinsatzesZaehlenGleichViel(
            @ForAll @IntRange(min = 0, max = 24) int einsatzA,
            @ForAll @IntRange(min = 0, max = 24) int einsatzB,
            @ForAll @IntRange(min = 0, max = 500) int verliererEinsatz) {

        List<Pick> picks = List.of(
                new Pick(PlayerId.of("a"), TOUCHDOWN, Points.of(einsatzA)),
                new Pick(PlayerId.of("b"), TOUCHDOWN, Points.of(einsatzB)),
                new Pick(PlayerId.of("verlierer"), PUNT, Points.of(verliererEinsatz)));

        Settlement.Result result = Settlement.settle(picks, Set.of(), Map.of(), TOUCHDOWN, PARAMS);

        int deltaA = result.deltas().getOrDefault(PlayerId.of("a"), PointsDelta.NONE).value();
        int deltaB = result.deltas().getOrDefault(PlayerId.of("b"), PointsDelta.NONE).value();

        // (deltaA - deltaB) waere bei exakt gleichem Anteil genau (einsatzB - einsatzA);
        // das Groesste-Reste-Verfahren darf das um hoechstens 1 verschieben.
        assertThat((double) (deltaA - deltaB)).isCloseTo(einsatzB - einsatzA, offset(1.0));
    }

    /**
     * 7.2: Auszahlungen sind ganzzahlig, und ihre Summe entspricht exakt dem
     * Pool -- fuer eine beliebige Anzahl Gewinner mit beliebigen Einsaetzen.
     */
    @Property
    @Anforderung({ "7.2", "3-b" })
    void summeDerAuszahlungenTrifftExaktDenPool(
            @ForAll("mindestensEinGewinner") List<Integer> gewinnerEinsaetze,
            @ForAll @IntRange(min = 0, max = 500) int verliererEinsatz) {

        List<Pick> picks = new ArrayList<>();
        int i = 0;
        for (int einsatz : gewinnerEinsaetze) {
            picks.add(new Pick(PlayerId.of("g" + i++), TOUCHDOWN, Points.of(einsatz)));
        }
        picks.add(new Pick(PlayerId.of("verlierer"), PUNT, Points.of(verliererEinsatz)));

        Settlement.Result result = Settlement.settle(picks, Set.of(), Map.of(), TOUCHDOWN, PARAMS);

        int summeAuszahlungen = 0;
        for (int j = 0; j < gewinnerEinsaetze.size(); j++) {
            PointsDelta delta = result.deltas().getOrDefault(PlayerId.of("g" + j), PointsDelta.NONE);
            summeAuszahlungen += delta.value() + gewinnerEinsaetze.get(j);
        }
        assertThat(summeAuszahlungen).isEqualTo(result.pool().value());
    }

    /**
     * 8.1-c: Die Strafe wird auf den Kontostand gekappt -- eingesammelt wird
     * nie mehr als min(Strafe, Kontostand), fuer jeden moeglichen Kontostand.
     */
    @Property
    @Anforderung({ "8.1-c", "3-e" })
    void eingesammelteStrafeUeberschreitetNieDenKontostand(@ForAll @IntRange(min = 0, max = 100) int kontostand) {
        PlayerId nichtTipper = PlayerId.of("n");
        List<Pick> picks = List.of(new Pick(PlayerId.of("a"), TOUCHDOWN, PARAMS.minStake()));

        Settlement.Result result = Settlement.settle(
                picks, Set.of(nichtTipper), Map.of(nichtTipper, Points.of(kontostand)), TOUCHDOWN, PARAMS);

        PointsDelta delta = result.deltas().get(nichtTipper);
        int eingesammelt = delta == null ? 0 : -delta.value();
        assertThat(eingesammelt).isLessThanOrEqualTo(kontostand);
        assertThat(eingesammelt).isLessThanOrEqualTo(PARAMS.penalty().value());
    }
}
