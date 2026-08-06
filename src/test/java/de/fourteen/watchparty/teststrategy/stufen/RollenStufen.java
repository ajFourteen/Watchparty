package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rollen und Host-Uebergabe (Anforderung 10) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 10-a, 10.1, 10.1-a, 10.1-b
 * und 10.1-c.
 */
public class RollenStufen extends RaumStufen<RollenStufen> {

    public RollenStufen dreiSpielerTretenBeiInDieserReihenfolge(String erster, String zweiter, String dritter) {
        beitreten(erster);
        beitreten(zweiter);
        beitreten(dritter);
        return this;
    }

    public RollenStufen istHost(String spieler) {
        assertThat(neuesterStatusFuer(spieler).hostPlayerId())
                .as("Host ist immer der am fruehesten beigetretene verbundene Spieler (10.1)")
                .isEqualTo(gateway.playerIdOf(sessionVon(spieler)).value());
        return this;
    }

    public RollenStufen derHostSteuertEinenVollstaendigenRundenablaufUndSetztDanachZurueck(String host) {
        actor.openBet(sessionVon(host), null);
        actor.placePick(sessionVon(host), "touchdown", null);
        actor.closeBet(sessionVon(host));
        actor.resolve(sessionVon(host), "touchdown");
        actor.openBet(sessionVon(host), null);
        actor.annul(sessionVon(host));
        actor.reset(sessionVon(host));
        actor.awaitIdle();
        return this;
    }

    public RollenStufen keinDieserBefehleWurdeVomHostAbgelehnt(String host) {
        assertThat(gateway.errorsFor(sessionVon(host))).isEmpty();
        return this;
    }

    public RollenStufen derHostVerliertDieVerbindungWaehrendDasFensterOffenIst(String host) {
        actor.openBet(sessionVon(host), null);
        actor.awaitIdle();
        trennen(host);
        return this;
    }

    public RollenStufen wirdSofortHost(String spieler) {
        assertThat(neuesterStatusFuer(spieler).hostPlayerId())
                .isEqualTo(gateway.playerIdOf(sessionVon(spieler)).value());
        return this;
    }

    public RollenStufen derFruehereHostKehrtWaehrendDesOffenenFenstersZurueck(String fruehererHost) {
        wiederverbinden(fruehererHost);
        return this;
    }

    public RollenStufen dieHostRolleBleibtVorerstBeim(String amtierenderHost) {
        assertThat(neuesterStatusFuer(amtierenderHost).hostPlayerId())
                .as("die Uebergabe an den frueheren Host ist vorgemerkt, aber nicht sofort wirksam (10.1-b)")
                .isEqualTo(gateway.playerIdOf(sessionVon(amtierenderHost)).value());
        return this;
    }

    public RollenStufen derAmtierendeHostSchliesstUndLoestZugunstenVonTouchdownAuf(String amtierenderHost) {
        actor.closeBet(sessionVon(amtierenderHost));
        actor.resolve(sessionVon(amtierenderHost), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public RollenStufen derFruehereHostBekommtDieRolleZurueck(String fruehererHost) {
        assertThat(neuesterStatusFuer(fruehererHost).hostPlayerId())
                .as("erst beim Erreichen von RESOLVED wird die vorgemerkte Uebergabe wirksam (10.1-b)")
                .isEqualTo(gateway.playerIdOf(sessionVon(fruehererHost)).value());
        return this;
    }
}
