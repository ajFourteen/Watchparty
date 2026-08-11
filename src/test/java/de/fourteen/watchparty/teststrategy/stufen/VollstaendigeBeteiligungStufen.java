package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der dritte Ausloeser fuers Schliessen (Anforderung 5-g) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2): Haben alle
 * Teilnehmer des eingefrorenen Kreises getippt, schliesst das Fenster
 * sofort -- ohne Countdown und ohne Host-Klick.
 *
 * Massgeblich ist derselbe eingefrorene Kreis wie bei der Strafe (8.1-b/d),
 * nicht die Zahl der abgegebenen Tipps: Wer erst nach dem Oeffnen
 * beigetreten ist oder pausiert, haelt die Runde nicht auf.
 */
public class VollstaendigeBeteiligungStufen extends RaumStufen<VollstaendigeBeteiligungStufen> {

    public VollstaendigeBeteiligungStufen einHostUndAnnaSindImRaum() {
        beitreten("Host");
        beitreten("Anna");
        return this;
    }

    public VollstaendigeBeteiligungStufen derHostIstImRaum() {
        beitreten("Host");
        return this;
    }

    public VollstaendigeBeteiligungStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public VollstaendigeBeteiligungStufen annaTrittJetztErstBei() {
        beitreten("Anna");
        return this;
    }

    public VollstaendigeBeteiligungStufen annaTrenntSich() {
        trennen("Anna");
        return this;
    }

    /**
     * Zwei verpasste Runden reichen (8.1-d): Ab der dritten pausiert Anna und
     * gehoert damit nicht mehr zum eingefrorenen Kreis.
     */
    public VollstaendigeBeteiligungStufen annaVerpasstZweiRundenUndPausiert() {
        for (int runde = 0; runde < 2; runde++) {
            actor.openBet(sessionVon("Host"), null);
            actor.placePick(sessionVon("Host"), "touchdown", null);
            actor.closeBet(sessionVon("Host"));
            actor.resolve(sessionVon("Host"), "touchdown");
            actor.awaitIdle();
        }
        assertThat(annaAnsicht().paused()).as("Anna pausiert nach zwei verpassten Runden (8.1-d)").isTrue();
        return this;
    }

    public VollstaendigeBeteiligungStufen annaTipptTouchdown() {
        actor.placePick(sessionVon("Anna"), "touchdown", null);
        actor.awaitIdle();
        return this;
    }

    public VollstaendigeBeteiligungStufen derHostTipptPunt() {
        actor.placePick(sessionVon("Host"), "punt", null);
        actor.awaitIdle();
        return this;
    }

    public VollstaendigeBeteiligungStufen derHostTipptTouchdown() {
        actor.placePick(sessionVon("Host"), "touchdown", null);
        actor.awaitIdle();
        return this;
    }

    public VollstaendigeBeteiligungStufen derHostLoestZugunstenVonTouchdownAuf() {
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public VollstaendigeBeteiligungStufen fuenfzehnSekundenVergehenUndDerAutoCloseTimerFeuert() {
        clock.advance(Duration.ofSeconds(15));
        scheduler.fireAll();
        actor.awaitIdle();
        return this;
    }

    public VollstaendigeBeteiligungStufen dasFensterIstGeschlossen() {
        assertThat(neuesterStatusFuer("Host").phase()).isEqualTo("CLOSED");
        return this;
    }

    public VollstaendigeBeteiligungStufen dasFensterIstNochOffen() {
        assertThat(neuesterStatusFuer("Host").phase()).isEqualTo("OPEN");
        return this;
    }

    /**
     * Belegt, dass wirklich die vollstaendige Beteiligung geschlossen hat und
     * nicht doch der Countdown oder ein Klick: Die Uhr steht noch am Anfang
     * des Fensters, und der Auto-Close-Task wurde abbestellt, statt zu feuern.
     */
    public VollstaendigeBeteiligungStufen wederDieZeitNochDerHostHabenGeschlossen() {
        assertThat(clock.instant()).as("die 15 Sekunden sind nicht abgelaufen").isEqualTo(START);
        assertThat(scheduler.pendingCount()).as("der Auto-Close-Task ist abbestellt").isZero();
        return this;
    }

    /**
     * Anforderung 5-g laesst keinen Zwischenzustand zu: Kein einziger
     * gesendeter Zustand darf ein offenes Fenster zeigen, in dem schon alle
     * getippt haben. Geprueft ueber den gesamten Verlauf, nicht nur ueber den
     * letzten Frame -- ein spaeteres Nachschliessen waere sonst nicht zu
     * unterscheiden.
     */
    public VollstaendigeBeteiligungStufen keinZustandZeigteEinOffenesFensterMitVollzaehligenTipps() {
        for (Object nachricht : gateway.messagesFor(sessionVon("Host"))) {
            if (nachricht instanceof Messages.State status && "OPEN".equals(status.phase())) {
                assertThat(status.pickCount())
                        .as("offenes Fenster mit bereits vollzaehligen Tipps (5-g)")
                        .isLessThan(status.participantCount());
            }
        }
        return this;
    }

    public VollstaendigeBeteiligungStufen beideTippsLiegenOffen() {
        assertThat(neuesterStatusFuer("Host").revealedPicks()).hasSize(2);
        return this;
    }

    public VollstaendigeBeteiligungStufen esFehltNochEinTipp() {
        Messages.State status = neuesterStatusFuer("Host");
        assertThat(status.pickCount()).isEqualTo(status.participantCount() - 1);
        return this;
    }

    public VollstaendigeBeteiligungStufen hatDenGanzenPoolGewonnen(String spieler) {
        Messages.State status = neuesterStatusFuer("Host");
        Map<String, Integer> deltas = status.deltas();
        assertThat(deltas).as("Deltas nach dem Aufloesen").isNotNull();
        String playerId = gateway.playerIdOf(sessionVon(spieler)).value();
        assertThat(deltas.get(playerId))
                .as("Gewinn von " + spieler + " ist der Pool abzueglich des eigenen Einsatzes")
                .isEqualTo(status.pool() - einsatzVon(playerId));
        return this;
    }

    private int einsatzVon(String playerId) {
        return neuesterStatusFuer("Host").revealedPicks().stream()
                .filter(tipp -> tipp.playerId().equals(playerId))
                .mapToInt(tipp -> tipp.stake())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Kein aufgedeckter Tipp fuer " + playerId));
    }

    private Messages.PlayerView annaAnsicht() {
        String playerId = gateway.playerIdOf(sessionVon("Anna")).value();
        return neuesterStatusFuer("Host").players().stream()
                .filter(ansicht -> ansicht.id().equals(playerId))
                .findFirst()
                .orElseThrow();
    }
}
