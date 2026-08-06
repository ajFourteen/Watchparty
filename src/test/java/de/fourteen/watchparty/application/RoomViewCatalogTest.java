package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Bets;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RoomView#catalog()} traegt WELCOME den ganzen Wettkatalog (4-b) und
 * war bislang nur ueber den Umweg des WELCOME-Frames auf der API-Ebene
 * geprueft ({@code WireProtocolSmokeTest}) -- ein Mutationstest mit dem
 * Testfilter auf die Ebenen unit/port (docs/teststrategie.md, Abschnitt 7.2)
 * deckte die Luecke auf: ohne diesen Test liesse sich {@code catalog()}
 * durch eine leere Liste ersetzen, ohne dass ein unit- oder port-Test das
 * bemerkt.
 */
@UnitTest
class RoomViewCatalogTest {

    @Test
    @Anforderung("4-b")
    void catalogBildetDenGesamtenWettkatalogAb() {
        List<Messages.BetView> catalog = RoomView.catalog();

        assertThat(catalog).hasSameSizeAs(Bets.CATALOG);
        for (int i = 0; i < Bets.CATALOG.size(); i++) {
            assertThat(catalog.get(i).id()).isEqualTo(Bets.CATALOG.get(i).id().value());
            assertThat(catalog.get(i).question()).isEqualTo(Bets.CATALOG.get(i).question());
        }
    }
}
