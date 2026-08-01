package de.fourteenit.watchparty.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketsTest {

    @Test
    void driveOutcomeHatDieSiebenKanonischenAusgaengeAusAnforderung41() {
        assertThat(Markets.DRIVE_OUTCOME.outcomes())
                .extracting(Outcome::id)
                .containsExactly(
                        "touchdown", "field-goal", "punt", "turnover",
                        "turnover-on-downs", "safety", "end-of-half");
    }

    @Test
    void turnoverOnDownsTraegtDieAbgrenzungZumVerschossenenFieldGoal() {
        Outcome turnoverOnDowns = Markets.DRIVE_OUTCOME.outcomes().stream()
                .filter(outcome -> outcome.id().equals("turnover-on-downs"))
                .findFirst()
                .orElseThrow();

        assertThat(turnoverOnDowns.note()).contains("Field Goal");
    }
}
