package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Allaussagen ueber {@link GameScore}, unabhaengig von {@code Scoring}
 * (docs/teststrategie.md, Abschnitt 4; Mutation Score ≥ 99 % nach ADR-038).
 * Ein Test, der {@link GameScore#tendency()} nur ueber
 * {@code Scoring.score} beobachtet, kann einen Fehler an der
 * Unentschieden-Grenze nicht bemerken: Vertauscht {@code tendency()} dort
 * konsequent HEIM/GAST/UNENTSCHIEDEN, bleibt der <em>Vergleich</em> zweier
 * gleichermassen falsch berechneter Tendenzen zufaellig richtig.
 */
@UnitTest
class GameScorePropertyTest {

    @Property
    @Anforderung("13.5-b")
    void tendencySpiegeltDenSieger(
            @ForAll @IntRange(min = 0, max = 100) int home,
            @ForAll @IntRange(min = 0, max = 100) int away) {

        Tendency tendency = GameScore.of(home, away).tendency();

        if (home > away) {
            assertThat(tendency).isEqualTo(Tendency.HEIM);
        } else if (away > home) {
            assertThat(tendency).isEqualTo(Tendency.GAST);
        } else {
            assertThat(tendency).isEqualTo(Tendency.UNENTSCHIEDEN);
        }
    }

    @Property
    void toStringZeigtHeimDoppelpunktGast(
            @ForAll @IntRange(min = 0, max = 100) int home,
            @ForAll @IntRange(min = 0, max = 100) int away) {

        assertThat(GameScore.of(home, away)).hasToString(home + ":" + away);
    }
}
