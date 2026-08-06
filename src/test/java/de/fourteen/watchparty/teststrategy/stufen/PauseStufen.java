package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Params;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die Verpasste-Runden-Pause (Anforderung 8.1) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 8.1, 8.1-d und 8.1-e.
 */
public class PauseStufen extends RaumStufen<PauseStufen> {

    public PauseStufen einHostUndSpielerSindImRaum(String spieler) {
        beitreten("Host");
        beitreten(spieler);
        return this;
    }

    public PauseStufen derSpielerTrenntSich(String spieler) {
        trennen(spieler);
        return this;
    }

    public PauseStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public PauseStufen derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf() {
        actor.placePick(sessionVon("Host"), "touchdown", null);
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public PauseStufen derTeilnehmerkreisUmfasstGenauSpieler(int erwarteteAnzahl) {
        assertThat(neuesterStatusFuer("Host").participantCount()).isEqualTo(erwarteteAnzahl);
        return this;
    }

    public PauseStufen hatInsgesamtPunkteVerlorenUndIstPausiert(String spieler, int verlorenePunkte, boolean pausiert) {
        Messages.PlayerView ansicht = spielerAnsicht(spieler);
        assertThat(Params.DEFAULT.startingPoints().value() - ansicht.points())
                .as("verlorene Punkte von " + spieler)
                .isEqualTo(verlorenePunkte);
        assertThat(ansicht.paused()).as("pausiert-Status von " + spieler).isEqualTo(pausiert);
        return this;
    }

    private Messages.PlayerView spielerAnsicht(String spieler) {
        String playerId = gateway.playerIdOf(sessionVon(spieler)).value();
        return neuesterStatusFuer("Host").players().stream()
                .filter(ansicht -> ansicht.id().equals(playerId))
                .findFirst()
                .orElseThrow();
    }
}
