package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Params;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Beitritt (Anforderung 1/3) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-e, 1-g, 1-h, 1-i, 3-a,
 * 3-c und 3.1-c.
 */
public class BeitrittStufen extends RaumStufen<BeitrittStufen> {

    /** 1-e: {@link #beitreten} verlangt selbst strukturell nur einen Namen -- kein Account, kein Kennwort. */
    public BeitrittStufen einSpielerTrittNurMitEinemNamenBei(String name) {
        beitreten(name);
        return this;
    }

    /** 1-g: ein Beitritt ohne Code erzeugt eine neue Watchparty, deren Host der Beitretende wird (ADR-016). */
    public BeitrittStufen istHostDerNeuenWatchparty(String name) {
        assertThat(welcomeVon(name).roomCode()).as("ein vierstelliger Code wurde vergeben").hasSize(4);
        assertThat(neuesterStatusFuer(name).hostPlayerId())
                .isEqualTo(gateway.playerIdOf(sessionVon(name)).value());
        return this;
    }

    /** 1-g: wer den Code kennt, tritt derselben Watchparty bei statt einer neuen. */
    public BeitrittStufen istInDerselbenWatchpartyWie(String name, String andererName) {
        assertThat(welcomeVon(name).roomCode()).isEqualTo(welcomeVon(andererName).roomCode());
        return this;
    }

    /** 1-h/1-i: ein Code, zu dem keine Watchparty existiert, fuehrt zum Fehler statt zu einer neuen Watchparty. */
    public BeitrittStufen trittMitUnbekanntemCodeBei(String name, String unbekannterCode) {
        beitretenMitExplizitemCode(name, unbekannterCode);
        return this;
    }

    /** 1-h: Groß-/Kleinschreibung des Codes ist beim Beitritt gleichgueltig. */
    public BeitrittStufen trittMitDemCodeVonInKleinschreibungBei(String name, String andererName) {
        String code = welcomeVon(andererName).roomCode();
        beitretenMitExplizitemCode(name, code.toLowerCase());
        return this;
    }

    /** Kriterium 3 aus Feature 004: kein WELCOME, also keine Watchparty entstanden oder betreten. */
    public BeitrittStufen bekommtEinenFehlerWeilDerCodeUnbekanntIst(String name) {
        assertThat(gateway.errorsFor(sessionVon(name))).contains("Unbekannter Raum-Code.");
        assertThat(gateway.messagesFor(sessionVon(name)))
                .as("kein WELCOME -- kein Raum wurde angelegt oder betreten (Anforderung 1-i)")
                .noneMatch(Messages.Welcome.class::isInstance);
        return this;
    }

    /** 3-a und 3-c: das Startguthaben ist fest und wird ueber STATE tatsaechlich vom Server gemeldet. */
    public BeitrittStufen hatDasFestgelegteStartguthaben(String name) {
        String playerId = gateway.playerIdOf(sessionVon(name)).value();
        int punkte = neuesterStatusFuer(name).players().stream()
                .filter(spieler -> spieler.id().equals(playerId))
                .findFirst().orElseThrow()
                .points();
        assertThat(punkte).isEqualTo(Params.DEFAULT.startingPoints().value());
        return this;
    }

    /**
     * 3.1-c: Die drei Parameter kommen mit dem WELCOME. Verglichen wird
     * gegen {@link Params#DEFAULT} und nicht gegen feste Zahlen -- der
     * Sinn der Regel ist ja gerade, dass es nur eine Quelle gibt (3.1-a).
     * Eine Kopie der Werte hier im Test waere die zweite.
     */
    public BeitrittStufen kenntDieDreiParameterAusDemWelcome(String name) {
        Messages.Params params = welcomeVon(name).params();
        assertThat(params).as("WELCOME nennt die Parameter (3.1-c)").isNotNull();
        assertThat(params.startingPoints()).isEqualTo(Params.DEFAULT.startingPoints().value());
        assertThat(params.minStake()).isEqualTo(Params.DEFAULT.minStake().value());
        assertThat(params.penalty()).isEqualTo(Params.DEFAULT.penalty().value());
        return this;
    }
}
