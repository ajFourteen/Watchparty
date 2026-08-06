package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verdeckte Tipps (Invariante 4, ADR-013) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitte 2.2 und 3.1), belegt 6-b und 9-b.
 *
 * Der Leck-Test quantifiziert ueber die Ausgabeflaeche, nicht ueber die
 * Anforderungen (Abschnitt 3.1): Jedes Feld von {@link Messages.State}, das
 * waehrend OPEN gesetzt ist, muss auf der Positivliste stehen. Ein neues
 * Feld an {@code Messages.State} erzwingt damit eine bewusste Entscheidung,
 * statt sich unbemerkt durchzuschmuggeln.
 */
public class VerdeckteTippsStufen extends RaumStufen<VerdeckteTippsStufen> {

    private static final Set<String> WAEHREND_OPEN_ERLAUBTE_FELDER = Set.of(
            "players", "hostPlayerId", "phase", "roundId", "bet", "closesAt", "serverNow",
            "pickCount", "participantCount");

    public VerdeckteTippsStufen einHostUndAnnaSindImRaum() {
        beitreten("Host");
        beitreten("Anna");
        return this;
    }

    public VerdeckteTippsStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public VerdeckteTippsStufen annaTipptTouchdownMitEinsatz(int einsatz) {
        actor.placePick(sessionVon("Anna"), "touchdown", einsatz);
        actor.awaitIdle();
        return this;
    }

    public VerdeckteTippsStufen keinFrameAnDenHostVerraetWasAnnaGetipptHat() {
        Messages.State status = neuesterStatusFuer("Host");
        assertThat(status.phase()).isEqualTo("OPEN");
        assertNurErlaubteFelderGesetzt(status);
        assertThat(status.pickCount()).as("nur der Zaehler, kein einzelner Tipp (6-b)").isEqualTo(1);
        assertThat(status.participantCount()).isEqualTo(2);
        return this;
    }

    public VerdeckteTippsStufen derHostHatKeinYourPickFuerAnnasTippErhalten() {
        boolean hostSahYourPick = gateway.messagesFor(sessionVon("Host")).stream()
                .anyMatch(Messages.YourPick.class::isInstance);
        assertThat(hostSahYourPick).as("YOUR_PICK geht nur an die tippende Sitzung selbst").isFalse();
        return this;
    }

    public VerdeckteTippsStufen derHostSchliesstDasFenster() {
        actor.closeBet(sessionVon("Host"));
        actor.awaitIdle();
        return this;
    }

    public VerdeckteTippsStufen jetztIstAnnasTippFuerAlleSichtbar(int erwarteterEinsatz) {
        Messages.State status = neuesterStatusFuer("Host");
        assertThat(status.phase()).isEqualTo("CLOSED");
        List<Messages.RevealedPick> revealed = status.revealedPicks();
        assertThat(revealed).hasSize(1);
        assertThat(revealed.get(0).outcomeId()).isEqualTo("touchdown");
        assertThat(revealed.get(0).stake()).isEqualTo(erwarteterEinsatz);
        return this;
    }

    public VerdeckteTippsStufen einWeitererTippversuchVonAnnaWirdAbgelehntWeilDasFensterZuIst() {
        actor.placePick(sessionVon("Anna"), "touchdown", 25);
        actor.awaitIdle();
        assertThat(gateway.errorsFor(sessionVon("Anna"))).contains("Das Wettfenster ist nicht offen.");
        return this;
    }

    private void assertNurErlaubteFelderGesetzt(Messages.State status) {
        for (RecordComponent komponente : Messages.State.class.getRecordComponents()) {
            Object wert;
            try {
                wert = komponente.getAccessor().invoke(status);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Konnte Feld " + komponente.getName() + " nicht lesen", e);
            }
            if (wert != null && !WAEHREND_OPEN_ERLAUBTE_FELDER.contains(komponente.getName())) {
                throw new AssertionError("Feld '" + komponente.getName()
                        + "' ist waehrend OPEN gesetzt, steht aber nicht auf der Positivliste: " + wert);
            }
        }
    }
}
