package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Room;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Annullieren (Anforderung 8.6) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 8.6, 8.6-a und 8.6-b.
 */
public class AnnullierenStufen extends RaumStufen<AnnullierenStufen> {

    public AnnullierenStufen einHostUndBenSindImRaumBenIstGetrennt() {
        beitreten("Host");
        beitreten("Ben");
        trennen("Ben");
        return this;
    }

    public AnnullierenStufen derHostOeffnetEineWetteTipptUndAnnulliert() {
        actor.openBet(sessionVon("Host"), null);
        actor.placePick(sessionVon("Host"), "touchdown", 200);
        actor.annul(sessionVon("Host"));
        actor.awaitIdle();
        return this;
    }

    public AnnullierenStufen alleKontenSindUnveraendert() {
        for (Messages.PlayerView spieler : neuesterStatusFuer("Host").players()) {
            assertThat(spieler.points())
                    .as(spieler.name() + " nach Annullieren")
                    .isEqualTo(Room.STARTING_POINTS.value());
        }
        return this;
    }

    public AnnullierenStufen benIstAuchNachMehrerenAnnulliertenRundenNichtPausiert() {
        assertThat(neuesterStatusFuer("Host").players().stream()
                .filter(spieler -> spieler.name().equals("Ben"))
                .findFirst().orElseThrow().paused())
                .as("Annullieren zaehlt nicht als verpasste Runde (8.6-a)")
                .isFalse();
        return this;
    }

    public AnnullierenStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public AnnullierenStufen derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf() {
        actor.placePick(sessionVon("Host"), "touchdown", null);
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public AnnullierenStufen derHostVersuchtDanachZuAnnullieren() {
        actor.annul(sessionVon("Host"));
        actor.awaitIdle();
        return this;
    }

    public AnnullierenStufen dieRundeBleibtAufgeloestUndNichtAnnulliert() {
        Messages.State status = neuesterStatusFuer("Host");
        assertThat(status.phase()).isEqualTo("RESOLVED");
        assertThat(status.winningOutcomeId()).isEqualTo("touchdown");
        assertThat(status.annulled()).isFalse();
        return this;
    }

    public AnnullierenStufen derHostBekommtEinenFehlerBeimVersuchNachDemAufloesenZuAnnullieren() {
        List<String> fehler = gateway.errorsFor(sessionVon("Host"));
        assertThat(fehler).contains("Es läuft keine Runde, die sich annullieren ließe.");
        return this;
    }
}
