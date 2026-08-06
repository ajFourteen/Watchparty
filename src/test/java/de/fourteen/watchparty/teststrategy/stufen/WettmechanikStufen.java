package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wettmechanik (Anforderung 6) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 6-a, 6-c, 6-e und 6-f.
 */
public class WettmechanikStufen extends RaumStufen<WettmechanikStufen> {

    public WettmechanikStufen einHostUndBenSindImRaum() {
        beitreten("Host");
        beitreten("Ben");
        return this;
    }

    public WettmechanikStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    /** 4-b: der Host waehlt aus dem servereigenen Katalog aus, statt eine Wette selbst zu erfinden. */
    public WettmechanikStufen derHostWaehltAusDemKatalogDieWette(String betId) {
        actor.openBet(sessionVon("Host"), betId);
        actor.awaitIdle();
        return this;
    }

    public WettmechanikStufen dieOffeneRundeIstDieGewaehlteWette(String erwarteteBetId) {
        assertThat(neuesterStatusFuer("Host").bet().id()).isEqualTo(erwarteteBetId);
        return this;
    }

    public WettmechanikStufen derHostTipptOhneEinsatzanzugeben() {
        actor.placePick(sessionVon("Host"), "touchdown", null);
        actor.awaitIdle();
        return this;
    }

    public WettmechanikStufen derEigeneTippDesHostsZeigtEinsatz(int erwarteterEinsatz) {
        assertThat(letzterYourPick("Host").stake()).isEqualTo(erwarteterEinsatz);
        return this;
    }

    public WettmechanikStufen derHostVersuchtErneutZuTippenMitAnderemAusgangUndEinsatz() {
        actor.placePick(sessionVon("Host"), "punt", 999);
        actor.awaitIdle();
        return this;
    }

    public WettmechanikStufen derZweiteTippversuchWirdAbgelehnt() {
        assertThat(gateway.errorsFor(sessionVon("Host"))).contains("Du hast in dieser Runde schon getippt.");
        return this;
    }

    public WettmechanikStufen nachDemSchliessenBleibtDerErsteTippGueltig(String erwarteterAusgang, int erwarteterEinsatz) {
        actor.closeBet(sessionVon("Host"));
        actor.awaitIdle();
        String hostPlayerId = gateway.playerIdOf(sessionVon("Host")).value();
        Messages.RevealedPick pick = neuesterStatusFuer("Host").revealedPicks().stream()
                .filter(p -> p.playerId().equals(hostPlayerId))
                .findFirst().orElseThrow();
        assertThat(pick.outcomeId()).isEqualTo(erwarteterAusgang);
        assertThat(pick.stake()).isEqualTo(erwarteterEinsatz);
        return this;
    }

    public WettmechanikStufen benTipptMitEinemEinsatzWeitUeberDemEigenenKontostand() {
        actor.placePick(sessionVon("Ben"), "touchdown", 999_999);
        actor.awaitIdle();
        return this;
    }

    public WettmechanikStufen bensEinsatzIstAufDenEigenenKontostandGedeckelt(int kontostand) {
        assertThat(letzterYourPick("Ben").stake()).isEqualTo(kontostand);
        return this;
    }

    public WettmechanikStufen derHostGewinntUndBenVerliertSeinenGesamtenKontostandInEinerRunde() {
        actor.placePick(sessionVon("Host"), "touchdown", 25);
        actor.placePick(sessionVon("Ben"), "punt", 1000);
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public WettmechanikStufen benTipptOhneEinsatzanzugebenMitNullPunkten() {
        actor.placePick(sessionVon("Ben"), "touchdown", null);
        actor.awaitIdle();
        return this;
    }

    public WettmechanikStufen bensZwangsweiserAllInEinsatzIst(int erwarteterEinsatz) {
        assertThat(letzterYourPick("Ben").stake()).isEqualTo(erwarteterEinsatz);
        return this;
    }

    private Messages.YourPick letzterYourPick(String spieler) {
        return gateway.messagesFor(sessionVon(spieler)).stream()
                .filter(Messages.YourPick.class::isInstance)
                .map(Messages.YourPick.class::cast)
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
