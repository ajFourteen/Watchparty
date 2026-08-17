package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Params;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trennung zweier Watchpartys (ADR-033, Anforderung 1-i) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2) -- der Kern von
 * Feature 004, Kritikalitaet HIGH.
 *
 * Verwaltet bewusst zwei eigene Codes ({@link #codeA}/{@link #codeB}) statt
 * des einen impliziten aus {@link RaumStufen#beitreten}: Genau die Trennung
 * zwischen ihnen ist hier der Gegenstand der Pruefung. Die eigentlichen
 * Aktionen (Wette oeffnen, tippen, aufloesen) bleiben dagegen generisch --
 * welcher Raum gemeint ist, ergibt sich allein daraus, welcher Name handelt.
 */
public class WatchpartyTrennungStufen extends RaumStufen<WatchpartyTrennungStufen> {

    private String codeA;
    private String codeB;

    private final Map<String, Integer> nachrichtenstand = new LinkedHashMap<>();

    public WatchpartyTrennungStufen istHostVonWatchpartyA(String name) {
        beitretenMitExplizitemCode(name, null);
        codeA = welcomeVon(name).roomCode();
        return this;
    }

    public WatchpartyTrennungStufen istHostVonWatchpartyB(String name) {
        beitretenMitExplizitemCode(name, null);
        codeB = welcomeVon(name).roomCode();
        return this;
    }

    public WatchpartyTrennungStufen trittWatchpartyABei(String name) {
        beitretenMitExplizitemCode(name, codeA);
        return this;
    }

    public WatchpartyTrennungStufen trittWatchpartyBBei(String name) {
        beitretenMitExplizitemCode(name, codeB);
        return this;
    }

    public WatchpartyTrennungStufen trennt(String name) {
        trennen(name);
        return this;
    }

    public WatchpartyTrennungStufen oeffnetEineWette(String name) {
        actor.openBet(sessionVon(name), null);
        actor.awaitIdle();
        return this;
    }

    public WatchpartyTrennungStufen tippt(String name, String ausgang) {
        actor.placePick(sessionVon(name), ausgang, null);
        actor.awaitIdle();
        return this;
    }

    public WatchpartyTrennungStufen schliesstUndLoestZugunstenVonAuf(String hostName, String ausgang) {
        actor.closeBet(sessionVon(hostName));
        actor.resolve(sessionVon(hostName), ausgang);
        actor.awaitIdle();
        return this;
    }

    /** Wie {@link #schliesstUndLoestZugunstenVonAuf}, aber der Host tippt vorher selbst mit (Muster wie {@code PauseStufen}). */
    public WatchpartyTrennungStufen tipptSchliesstUndLoestZugunstenVonTouchdownAuf(String hostName) {
        actor.placePick(sessionVon(hostName), "touchdown", null);
        actor.closeBet(sessionVon(hostName));
        actor.resolve(sessionVon(hostName), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public WatchpartyTrennungStufen setztDenRaumZurueck(String hostName) {
        actor.reset(sessionVon(hostName));
        actor.awaitIdle();
        return this;
    }

    /** Reconnect ueber den Token einer bestehenden Watchparty, aber in die andere (Anforderung 1-i, Kriterium 12). */
    public WatchpartyTrennungStufen trittMitDemTokenVonAberInWatchpartyBBei(String neuerName, String urspruenglicherName) {
        String token = tokenVon(urspruenglicherName);
        beitretenMitExplizitemCodeUndToken(neuerName, codeB, token);
        return this;
    }

    public WatchpartyTrennungStufen habenSichDiePunktekontenGeaendertFuer(String... namen) {
        for (String name : namen) {
            assertThat(spielerAnsicht(name).points())
                    .as("Punktekonto von " + name + " nach der Abrechnung in der eigenen Watchparty")
                    .isNotEqualTo(Params.DEFAULT.startingPoints().value());
        }
        return this;
    }

    public WatchpartyTrennungStufen sindUnveraendertBeiStartguthaben(String... namen) {
        for (String name : namen) {
            assertThat(spielerAnsicht(name).points())
                    .as("Punktekonto von " + name + " -- unberuehrt von der fremden Watchparty")
                    .isEqualTo(Params.DEFAULT.startingPoints().value());
        }
        return this;
    }

    public WatchpartyTrennungStufen istWeiterhinInPhase(String beobachter, String erwartetePhase) {
        assertThat(neuesterStatusFuer(beobachter).phase())
                .as("Phase aus Sicht von " + beobachter)
                .isEqualTo(erwartetePhase);
        return this;
    }

    public WatchpartyTrennungStufen merktSichDenNachrichtenstandVon(String name) {
        nachrichtenstand.put(name, gateway.messagesFor(sessionVon(name)).size());
        return this;
    }

    public WatchpartyTrennungStufen hatSeitdemKeineEinzigeNachrichtBekommen(String name) {
        int vorher = nachrichtenstand.getOrDefault(name, 0);
        assertThat(gateway.messagesFor(sessionVon(name)))
                .as(name + " darf keine Nachricht aus der fremden Watchparty bekommen (Anforderung 1-i)")
                .hasSize(vorher);
        return this;
    }

    /** Der letzte STATE an die eigene, alte Sitzung zeigt die frische, leere Watchparty (RESET laeuft vor dem Loesen der Bindung). */
    public WatchpartyTrennungStufen istLeerUndZurueckgesetzt(String beobachterVorReset) {
        assertThat(neuesterStatusFuer(beobachterVorReset).players())
                .as("Watchparty nach RESET")
                .isEmpty();
        return this;
    }

    public WatchpartyTrennungStufen hatWeiterhinGenauSpieler(String beobachter, int erwartet) {
        assertThat(neuesterStatusFuer(beobachter).players()).hasSize(erwartet);
        return this;
    }

    public WatchpartyTrennungStufen istDortEineNeueSpielerinMitStartguthaben(String neuerName) {
        assertThat(spielerAnsicht(neuerName).points()).isEqualTo(Params.DEFAULT.startingPoints().value());
        return this;
    }

    public WatchpartyTrennungStufen hatEineAndereSpielerIdAlsInWatchpartyA(String neuerName, String urspruenglicherName) {
        assertThat(gateway.playerIdOf(sessionVon(neuerName)))
                .as("Ein Token erkennt einen Spieler nur in seiner eigenen Watchparty wieder (Anforderung 1-i)")
                .isNotEqualTo(gateway.playerIdOf(sessionVon(urspruenglicherName)));
        return this;
    }

    /**
     * {@code name} ist in diesen beiden Schritten getrennt -- eine getrennte
     * Sitzung bekommt (wie im echten Betrieb) keine weiteren Zustandsmeldungen
     * mehr zugestellt, ihr eigener letzter Stand wäre also stets veraltet.
     * Beobachtet wird deshalb über eine Sitzung, die verbunden bleibt --
     * genau wie in {@code PauseStufen}.
     */
    public WatchpartyTrennungStufen istPausiert(String name, String beobachter) {
        assertThat(spielerAnsicht(name, beobachter).paused()).as(name + " sollte pausiert sein").isTrue();
        return this;
    }

    public WatchpartyTrennungStufen hatKeineRundeVerpasst(String name, String beobachter) {
        Messages.PlayerView ansicht = spielerAnsicht(name, beobachter);
        assertThat(ansicht.points())
                .as(name + " hat keine Strafe erhalten -- es lief keine Runde in der fremden Watchparty")
                .isEqualTo(Params.DEFAULT.startingPoints().value());
        assertThat(ansicht.paused()).as(name + " sollte nicht pausiert sein").isFalse();
        return this;
    }

    private Messages.PlayerView spielerAnsicht(String name) {
        return spielerAnsicht(name, name);
    }

    private Messages.PlayerView spielerAnsicht(String name, String beobachter) {
        String playerId = gateway.playerIdOf(sessionVon(name)).value();
        return neuesterStatusFuer(beobachter).players().stream()
                .filter(ansicht -> ansicht.id().equals(playerId))
                .findFirst()
                .orElseThrow();
    }
}
