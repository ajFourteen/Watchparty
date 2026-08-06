package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der eingefrorene Teilnehmerkreis (Anforderung 8.1) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2). Pilotszenario
 * aus Phase 1 der Teststrategie-Umsetzung, belegt 8.1-b.
 */
public class TeilnehmerkreisStufen extends RaumStufen<TeilnehmerkreisStufen> {

    public TeilnehmerkreisStufen einHostUndBenSindImRaum() {
        beitreten("Host");
        beitreten("Ben");
        return this;
    }

    public TeilnehmerkreisStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public TeilnehmerkreisStufen annaTrittJetztErstBei() {
        beitreten("Anna");
        return this;
    }

    public TeilnehmerkreisStufen derHostTipptTouchdownSchliesstUndLoestAuf() {
        actor.placePick(sessionVon("Host"), "touchdown", 25);
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public TeilnehmerkreisStufen zahltEineStrafe(String spieler) {
        Integer delta = deltaVon(spieler);
        assertThat(delta).as("Delta fuer " + spieler).isNotNull();
        assertThat(delta).isNegative();
        return this;
    }

    public TeilnehmerkreisStufen zahltKeineStrafeWeilErstNachOeffnenBeigetreten(String spieler) {
        assertThat(deltaVon(spieler)).as("Delta fuer " + spieler).isNull();
        return this;
    }

    private Integer deltaVon(String spieler) {
        Messages.State state = neuesterStatusFuer(spieler);
        String playerId = gateway.playerIdOf(sessionVon(spieler)).value();
        Map<String, Integer> deltas = state.deltas();
        return deltas == null ? null : deltas.get(playerId);
    }
}
