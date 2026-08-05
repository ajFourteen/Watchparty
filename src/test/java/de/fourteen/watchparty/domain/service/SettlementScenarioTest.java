package de.fourteen.watchparty.domain.service;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.UnitTest;
import de.fourteen.watchparty.teststrategy.stufen.AbrechnungStufen;

import org.junit.jupiter.api.Test;

/**
 * Pilotszenario der Domaenen-Ebene (Phase 1 der Teststrategie-Umsetzung),
 * belegt 8.1-c. Dieselbe Regel wie {@code SettlementTest#strafeWirdAufDenKontostandGekappt},
 * hier als JGiven-Szenario in der Sprache aus {@code anforderungen.md}.
 */
@UnitTest
class SettlementScenarioTest extends DeutschesSzenario<AbrechnungStufen, AbrechnungStufen, AbrechnungStufen> {

    @Test
    @Anforderung("8.1-c")
    void strafeWirdAufDenKontostandGekappt() {
        angenommen()
                .einTippAufTouchdownMitEinsatz("a", 25)
                .und().einNichtTipperMitKontostand("d", 10);

        wenn().dieRundeWirdMitAusgangTouchdownAbgerechnet();

        dann()
                .zahlt("d", -10)
                .und().zahlt("a", 10)
                .und().dieSummeAllerDeltasIstExaktNull();
    }
}
