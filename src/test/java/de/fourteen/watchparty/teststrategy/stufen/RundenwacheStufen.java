package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zeit- und Reihenfolge-Szenarien der Port-to-Port-Ebene (docs/teststrategie.md,
 * Abschnitt 2.2), allen voran die Rundenwache aus ADR-010: Der
 * {@code FakeScheduler} macht die Verschraenkung zwischen manuellem und
 * automatischem Schluss deterministisch nachspielbar, statt auf echte 15
 * Sekunden zu warten. Belegt 5-a bis 5-d.
 */
public class RundenwacheStufen extends RaumStufen<RundenwacheStufen> {

    public RundenwacheStufen derHostIstImRaum() {
        beitreten("Host");
        return this;
    }

    public RundenwacheStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public RundenwacheStufen fuenfzehnSekundenVergehenUndDerAutoCloseTimerFeuert() {
        clock.advance(Duration.ofSeconds(15));
        scheduler.fireAll();
        actor.awaitIdle();
        return this;
    }

    public RundenwacheStufen derHostSchliesstVonHand() {
        actor.closeBet(sessionVon("Host"));
        actor.awaitIdle();
        return this;
    }

    public RundenwacheStufen derVeralteteAutoCloseTimerFeuertTrotzdem() {
        scheduler.fireOldestIgnoringCancellation();
        actor.awaitIdle();
        return this;
    }

    /**
     * Der Host ist hier allein im Raum, sein Tipp schliesst das Fenster
     * deshalb schon selbst (5-g). Das {@code closeBet} bleibt trotzdem
     * stehen: Es belegt, dass ein Schluss auf einer bereits geschlossenen
     * Runde nichts kaputtmacht (ADR-020) -- genau die Verschraenkung, um die
     * es in dieser Stufe geht.
     */
    public RundenwacheStufen derHostTipptSchliesstUndLoestZugunstenVonTouchdownAuf() {
        actor.placePick(sessionVon("Host"), "touchdown", null);
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public RundenwacheStufen dasFensterIstAutomatischGeschlossen() {
        assertThat(phase()).isEqualTo("CLOSED");
        return this;
    }

    public RundenwacheStufen dieRundeBleibtEinfachGeschlossen() {
        assertThat(phase()).isEqualTo("CLOSED");
        return this;
    }

    public RundenwacheStufen dieNeueRundeBleibtOffen() {
        assertThat(phase()).isEqualTo("OPEN");
        return this;
    }

    /** 1-b: ein zweites Oeffnen waehrend einer laufenden Runde aendert nichts an der einen, bereits laufenden. */
    public RundenwacheStufen derHostVersuchtEinZweitesMalZuOeffnen() {
        Long ersteRoundId = neuesterStatusFuer("Host").roundId();
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        assertThat(neuesterStatusFuer("Host").roundId())
                .as("immer nur eine Runde gleichzeitig (1-b)")
                .isEqualTo(ersteRoundId);
        return this;
    }

    private String phase() {
        return neuesterStatusFuer("Host").phase();
    }
}
