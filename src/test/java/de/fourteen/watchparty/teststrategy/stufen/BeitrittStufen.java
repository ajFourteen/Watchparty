package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.domain.model.Params;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Beitritt (Anforderung 1/3) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-e, 3-a und 3-c.
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
}
