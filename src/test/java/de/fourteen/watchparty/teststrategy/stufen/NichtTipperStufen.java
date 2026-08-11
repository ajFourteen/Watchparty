package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die Teilnehmer ohne Tipp (Anforderung 8.1-f, Feature 002) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2).
 *
 * Das Feld ist die Umkehrung des Pick-Zaehlers: Wer waehrend OPEN nicht in
 * der Liste steht, hat getippt. Deshalb steht hier neben den positiven
 * Zusicherungen auch die negative -- waehrend des offenen Fensters darf es
 * gar nicht erst gesetzt sein (Invariante 4). Die Positivliste in
 * {@link VerdeckteTippsStufen} haelt dieselbe Grenze noch einmal von der
 * anderen Seite.
 */
public class NichtTipperStufen extends RaumStufen<NichtTipperStufen> {

    public NichtTipperStufen einHostUndAnnaSindImRaum() {
        beitreten("Host");
        beitreten("Anna");
        return this;
    }

    public NichtTipperStufen einHostUndBenSindImRaum() {
        beitreten("Host");
        beitreten("Ben");
        return this;
    }

    public NichtTipperStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public NichtTipperStufen annaTrittJetztErstBei() {
        beitreten("Anna");
        return this;
    }

    public NichtTipperStufen annaTippt() {
        actor.placePick(sessionVon("Anna"), "touchdown", 25);
        actor.awaitIdle();
        return this;
    }

    public NichtTipperStufen derHostTippt() {
        actor.placePick(sessionVon("Host"), "touchdown", 25);
        actor.awaitIdle();
        return this;
    }

    public NichtTipperStufen derHostSchliesstDasFenster() {
        actor.closeBet(sessionVon("Host"));
        actor.awaitIdle();
        return this;
    }

    public NichtTipperStufen derHostLoestAuf() {
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public NichtTipperStufen giltAlsTeilnehmerOhneTipp(String spieler) {
        assertThat(nichtTipper()).as("Teilnehmer ohne Tipp (8.1-f)").contains(spielerId(spieler));
        return this;
    }

    public NichtTipperStufen giltNichtAlsTeilnehmerOhneTipp(String spieler) {
        assertThat(nichtTipper()).as("Teilnehmer ohne Tipp (8.1-f)").doesNotContain(spielerId(spieler));
        return this;
    }

    /**
     * Invariante 4: Waehrend OPEN darf das Feld nicht einmal leer
     * mitgeschickt werden. Eine leere Liste waere die Aussage "alle haben
     * getippt" und damit schon zu viel.
     */
    public NichtTipperStufen verraetDerZustandDieNichtTipperNicht() {
        Messages.State status = neuesterStatusFuer("Host");
        assertThat(status.phase()).isEqualTo("OPEN");
        assertThat(status.nonPickers()).as("waehrend OPEN gar nicht gesetzt (Invariante 4)").isNull();
        return this;
    }

    private List<String> nichtTipper() {
        List<String> nonPickers = neuesterStatusFuer("Host").nonPickers();
        assertThat(nonPickers).as("ab dem Schliessen nennt der Zustand die Teilnehmer ohne Tipp (8.1-f)")
                .isNotNull();
        return nonPickers;
    }

    private String spielerId(String spieler) {
        return gateway.playerIdOf(sessionVon(spieler)).value();
    }
}
