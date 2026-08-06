package de.fourteen.watchparty.domain.model;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class BetsTest {

    @Test
    @Anforderung("4.1")
    void driveOutcomeHatDieSiebenKanonischenAusgaengeAusAnforderung41() {
        assertThat(Bets.DRIVE_OUTCOME.outcomes())
                .extracting(outcome -> outcome.id().value())
                .containsExactly(
                        "touchdown", "field-goal", "punt", "turnover",
                        "turnover-on-downs", "safety", "end-of-half");
    }

    @Test
    @Anforderung("4-e")
    void turnoverOnDownsTraegtDieAbgrenzungZumVerschossenenFieldGoal() {
        Outcome turnoverOnDowns = Bets.DRIVE_OUTCOME.outcomes().stream()
                .filter(outcome -> outcome.id().equals(OutcomeId.of("turnover-on-downs")))
                .findFirst()
                .orElseThrow();

        assertThat(turnoverOnDowns.note()).contains("Field Goal");
    }

    @Test
    @Anforderung({ "4.2", "4-e" })
    void bigPlayHatJaNeinUndNenntDieDreiYardSchwellenDamitDerHostNichtSchaetzenMuss() {
        assertThat(Bets.BIG_PLAY.outcomes())
                .extracting(outcome -> outcome.id().value())
                .containsExactly("yes", "no");
        assertThat(Bets.BIG_PLAY.note()).contains("20", "30", "50");
    }

    @Test
    @Anforderung("4.3")
    void fieldGoalHatGutUndKeinFieldGoal() {
        assertThat(Bets.FIELD_GOAL.outcomes())
                .extracting(outcome -> outcome.id().value())
                .containsExactly("good", "no-good");
    }

    /**
     * Der Katalog ist die einzige Quelle fuer Wetten; doppelte IDs wuerden
     * {@link Bets#byId(BetId)} zu einer Zufallsauswahl machen.
     */
    @Test
    @Anforderung("4-a")
    void jedeWetteImKatalogHatEineEigeneId() {
        Set<BetId> ids = new HashSet<>();
        for (Bet bet : Bets.CATALOG) {
            assertThat(ids.add(bet.id())).as("doppelte Wett-ID %s", bet.id()).isTrue();
        }
        assertThat(ids).hasSize(Bets.CATALOG.size());
    }

    /**
     * Innerhalb einer Wette muessen die Ausgaenge unterscheidbar sein, sonst
     * trifft die Aufloesung den falschen Eimer.
     */
    @Test
    @Anforderung("4-c")
    void jedeWetteHatMindestensZweiUnterscheidbareAusgaenge() {
        for (Bet bet : Bets.CATALOG) {
            assertThat(bet.outcomes()).as("Ausgaenge von %s", bet.id()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(bet.outcomes()).extracting(outcome -> outcome.id().value()).doesNotHaveDuplicates();
        }
    }

    /**
     * Der Versuch nach dem Touchdown deckt beide Varianten in einer Wette ab
     * (Anforderung 4.4-a). Faellt eine davon weg, muesste der Host die
     * Entscheidung des Teams vorwegnehmen und stuende ohne passenden Ausgang
     * da, wenn er falsch liegt.
     */
    @Test
    @Anforderung({ "4.4", "4.4-a" })
    void derVersuchNachDemTouchdownKenntKickUndZweiPunkte() {
        assertThat(Bets.TRY_AFTER_TOUCHDOWN.outcomes())
                .extracting(outcome -> outcome.id().value())
                .containsExactly(
                        "extra-point-good", "extra-point-no-good",
                        "two-point-good", "two-point-no-good");
    }

    @Test
    void byIdLiefertNullStattEinerAusnahmeBeiUnbekannterId() {
        assertThat(Bets.byId(BetId.of("drive-outcome"))).isSameAs(Bets.DRIVE_OUTCOME);
        assertThat(Bets.byId(BetId.of("gibt-es-nicht"))).isNull();
    }
}
