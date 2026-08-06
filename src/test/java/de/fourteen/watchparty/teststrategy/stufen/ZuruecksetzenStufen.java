package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zuruecksetzen (Anforderung 8.7) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 8.7 und 8.7-a.
 */
public class ZuruecksetzenStufen extends RaumStufen<ZuruecksetzenStufen> {

    public ZuruecksetzenStufen einHostUndAnnaSpielenGeradeEineOffeneRunde() {
        beitreten("Host");
        beitreten("Anna");
        actor.openBet(sessionVon("Host"), null);
        actor.placePick(sessionVon("Host"), "touchdown", 500);
        actor.awaitIdle();
        return this;
    }

    public ZuruecksetzenStufen einSpielerOhneHostRolleVersuchtZurueckzusetzen() {
        actor.reset(sessionVon("Anna"));
        actor.awaitIdle();
        return this;
    }

    public ZuruecksetzenStufen derHostSetztDenRaumZurueck() {
        actor.reset(sessionVon("Host"));
        actor.awaitIdle();
        return this;
    }

    public ZuruecksetzenStufen derRaumIstLeerOhneSpielerOhneHostUndOhneLaufendeRunde() {
        Messages.State status = gateway.lastStateFor(sessionVon("Host"));
        assertThat(status.players()).as("keine automatische Wiederherstellung der Spieler (8.7-a)").isEmpty();
        assertThat(status.hostPlayerId()).isNull();
        assertThat(status.phase()).isEqualTo("IDLE");
        return this;
    }

    public ZuruecksetzenStufen dieRundeLaeuftUnveraendertWeiter() {
        assertThat(gateway.lastStateFor(sessionVon("Host")).phase()).isEqualTo("OPEN");
        return this;
    }
}
