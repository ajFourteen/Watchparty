package de.fourteenit.watchparty.room;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BetsTest {

    @Test
    void driveOutcomeHatDieSiebenKanonischenAusgaengeAusAnforderung41() {
        assertThat(Bets.DRIVE_OUTCOME.outcomes())
                .extracting(Outcome::id)
                .containsExactly(
                        "touchdown", "field-goal", "punt", "turnover",
                        "turnover-on-downs", "safety", "end-of-half");
    }

    @Test
    void turnoverOnDownsTraegtDieAbgrenzungZumVerschossenenFieldGoal() {
        Outcome turnoverOnDowns = Bets.DRIVE_OUTCOME.outcomes().stream()
                .filter(outcome -> outcome.id().equals("turnover-on-downs"))
                .findFirst()
                .orElseThrow();

        assertThat(turnoverOnDowns.note()).contains("Field Goal");
    }

    @Test
    void bigPlayNenntDieDreiYardSchwellenDamitDerHostNichtSchaetzenMuss() {
        assertThat(Bets.BIG_PLAY.note()).contains("20", "30", "50");
    }

    /**
     * Der Katalog ist die einzige Quelle fuer Wetten; doppelte IDs wuerden
     * {@link Bets#byId(String)} zu einer Zufallsauswahl machen.
     */
    @Test
    void jedeWetteImKatalogHatEineEigeneId() {
        Set<String> ids = new HashSet<>();
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
    void jedeWetteHatMindestensZweiUnterscheidbareAusgaenge() {
        for (Bet bet : Bets.CATALOG) {
            assertThat(bet.outcomes()).as("Ausgaenge von %s", bet.id()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(bet.outcomes()).extracting(Outcome::id).doesNotHaveDuplicates();
        }
    }

    @Test
    void byIdLiefertNullStattEinerAusnahmeBeiUnbekannterId() {
        assertThat(Bets.byId("drive-outcome")).isSameAs(Bets.DRIVE_OUTCOME);
        assertThat(Bets.byId("gibt-es-nicht")).isNull();
    }
}
