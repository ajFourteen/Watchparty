package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.PlayerId;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reconnect in jeder Phase (ADR-014) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-c.
 */
public class ReconnectStufen extends RaumStufen<ReconnectStufen> {

    private PlayerId annasIdVorDemTrennen;

    public ReconnectStufen hostUndAnnaSindImRaum() {
        beitreten("Host");
        beitreten("Anna");
        annasIdVorDemTrennen = gateway.playerIdOf(sessionVon("Anna"));
        return this;
    }

    public ReconnectStufen annaTrenntSich() {
        trennen("Anna");
        return this;
    }

    public ReconnectStufen annaVerbindetSichWiederUndBehaeltDasselbeKonto() {
        wiederverbinden("Anna");
        assertThat(gateway.playerIdOf(sessionVon("Anna")))
                .as("Reconnect ueber denselben Token liefert dasselbe Konto (ADR-014)")
                .isEqualTo(annasIdVorDemTrennen);
        boolean verbunden = neuesterStatusFuer("Anna").players().stream()
                .filter(p -> p.id().equals(annasIdVorDemTrennen.value()))
                .findFirst().orElseThrow().connected();
        assertThat(verbunden).isTrue();
        return this;
    }

    public ReconnectStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public ReconnectStufen annaTipptPuntMitEinsatz(int einsatz) {
        actor.placePick(sessionVon("Anna"), "punt", einsatz);
        actor.awaitIdle();
        return this;
    }

    public ReconnectStufen annaKannJetztNochTippen() {
        actor.placePick(sessionVon("Anna"), "touchdown", null);
        actor.awaitIdle();
        assertThat(gateway.errorsFor(sessionVon("Anna"))).isEmpty();
        return this;
    }

    /** ADR-013: Der wiederhergestellte Tipp kommt separat als neues YOUR_PICK, nicht rueckwirkend im alten Frame. */
    public ReconnectStufen annaBekommtIhrenBereitsAbgegebenenTippErneutAlsYourPick(String ausgang, int einsatz) {
        Messages.YourPick yourPick = gateway.messagesFor(sessionVon("Anna")).stream()
                .filter(Messages.YourPick.class::isInstance)
                .map(Messages.YourPick.class::cast)
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(yourPick.outcomeId()).isEqualTo(ausgang);
        assertThat(yourPick.stake()).isEqualTo(einsatz);
        return this;
    }

    public ReconnectStufen derHostTipptTouchdownMitEinsatzUndSchliesstDasFenster(int einsatz) {
        actor.placePick(sessionVon("Host"), "touchdown", einsatz);
        actor.closeBet(sessionVon("Host"));
        actor.awaitIdle();
        return this;
    }

    public ReconnectStufen derHostLoestZugunstenVonTouchdownAuf() {
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public ReconnectStufen dieAufgedecktenTippsSindWeiterhinVollstaendigSichtbar(int erwarteteAnzahl) {
        List<Messages.RevealedPick> revealed = neuesterStatusFuer("Anna").revealedPicks();
        assertThat(revealed).hasSize(erwarteteAnzahl);
        return this;
    }

    public ReconnectStufen dasErgebnisIstWeiterhinSichtbar(String erwarteterAusgang) {
        Messages.State status = neuesterStatusFuer("Anna");
        assertThat(status.phase()).isEqualTo("RESOLVED");
        assertThat(status.winningOutcomeId()).isEqualTo(erwarteterAusgang);
        Map<String, Integer> deltas = status.deltas();
        assertThat(deltas).isNotEmpty();
        return this;
    }

    public ReconnectStufen annaVerpasstEineRundeGetrenntUndIstNochNichtPausiert() {
        actor.openBet(sessionVon("Host"), null);
        actor.placePick(sessionVon("Host"), "touchdown", null);
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        assertThat(annaAnsicht().paused()).isFalse();
        return this;
    }

    public ReconnectStufen annaIstNachDemReconnectNochImmerNichtPausiert() {
        assertThat(annaAnsicht().paused())
                .as("der Verpasste-Runden-Zaehler beginnt bei Reconnect von vorn (8.1-d)")
                .isFalse();
        return this;
    }

    private Messages.PlayerView annaAnsicht() {
        return neuesterStatusFuer("Anna").players().stream()
                .filter(p -> p.id().equals(annasIdVorDemTrennen.value()))
                .findFirst().orElseThrow();
    }
}
