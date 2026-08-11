package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Params;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Beitritt (Anforderung 1/3) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-e, 3-a, 3-c und 3.1-c.
 */
public class BeitrittStufen extends RaumStufen<BeitrittStufen> {

    /** 1-e: {@link #beitreten} verlangt selbst strukturell nur einen Namen -- kein Account, kein Kennwort. */
    public BeitrittStufen einSpielerTrittNurMitEinemNamenBei(String name) {
        beitreten(name);
        return this;
    }

    /** 3-a und 3-c: das Startguthaben ist fest und wird ueber STATE tatsaechlich vom Server gemeldet. */
    public BeitrittStufen hatDasFestgelegteStartguthaben(String name) {
        String playerId = gateway.playerIdOf(sessionVon(name)).value();
        int punkte = neuesterStatusFuer(name).players().stream()
                .filter(spieler -> spieler.id().equals(playerId))
                .findFirst().orElseThrow()
                .points();
        assertThat(punkte).isEqualTo(Params.DEFAULT.startingPoints().value());
        return this;
    }

    /**
     * 3.1-c: Die drei Parameter kommen mit dem WELCOME. Verglichen wird
     * gegen {@link Params#DEFAULT} und nicht gegen feste Zahlen -- der
     * Sinn der Regel ist ja gerade, dass es nur eine Quelle gibt (3.1-a).
     * Eine Kopie der Werte hier im Test waere die zweite.
     */
    public BeitrittStufen kenntDieDreiParameterAusDemWelcome(String name) {
        Messages.Params params = welcomeVon(name).params();
        assertThat(params).as("WELCOME nennt die Parameter (3.1-c)").isNotNull();
        assertThat(params.startingPoints()).isEqualTo(Params.DEFAULT.startingPoints().value());
        assertThat(params.minStake()).isEqualTo(Params.DEFAULT.minStake().value());
        assertThat(params.penalty()).isEqualTo(Params.DEFAULT.penalty().value());
        return this;
    }
}
