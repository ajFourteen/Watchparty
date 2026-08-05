package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.FakeScheduler;
import de.fourteen.watchparty.application.NoSnapshots;
import de.fourteen.watchparty.application.RecordingClientGateway;
import de.fourteen.watchparty.application.RoomActor;
import de.fourteen.watchparty.application.message.Messages;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Der eingefrorene Teilnehmerkreis (Anforderung 8.1) auf der
 * Port-to-Port-Ebene (docs/teststrategie.md, Abschnitt 2.2): Eingang ist
 * {@link RoomActor} als Umsetzung von {@code RoomCommands}, Ausgang die
 * {@link RecordingClientGateway}. Pilotszenario aus Phase 1 der
 * Teststrategie-Umsetzung, belegt 8.1-b.
 */
public class TeilnehmerkreisStufen extends DeutscheStufe<TeilnehmerkreisStufen> {

    private static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    private final RecordingClientGateway gateway = new RecordingClientGateway();
    private final RoomActor actor =
            new RoomActor(new FakeClock(START), new FakeScheduler(), new NoSnapshots(), gateway);
    private final Map<String, String> sessionByName = new LinkedHashMap<>();
    private int sessionCounter;

    public TeilnehmerkreisStufen einHostUndBenSindImRaum() {
        beitreten("Host");
        beitreten("Ben");
        return this;
    }

    public TeilnehmerkreisStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public TeilnehmerkreisStufen annaTrittJetztErstBei() {
        beitreten("Anna");
        return this;
    }

    public TeilnehmerkreisStufen derHostTipptTouchdownSchliesstUndLoestAuf() {
        actor.placePick(sessionVon("Host"), "touchdown", 25);
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public TeilnehmerkreisStufen zahltEineStrafe(String spieler) {
        Integer delta = deltaVon(spieler);
        assertThat(delta).as("Delta fuer " + spieler).isNotNull();
        assertThat(delta).isNegative();
        return this;
    }

    public TeilnehmerkreisStufen zahltKeineStrafeWeilErstNachOeffnenBeigetreten(String spieler) {
        assertThat(deltaVon(spieler)).as("Delta fuer " + spieler).isNull();
        return this;
    }

    private Integer deltaVon(String spieler) {
        Messages.State state = gateway.lastStateFor(sessionVon(spieler));
        String playerId = gateway.playerIdOf(sessionVon(spieler)).value();
        Map<String, Integer> deltas = state.deltas();
        return deltas == null ? null : deltas.get(playerId);
    }

    private void beitreten(String name) {
        String sessionId = name + "-" + (++sessionCounter);
        sessionByName.put(name, sessionId);
        actor.connected(sessionId);
        actor.join(sessionId, name, null);
        actor.awaitIdle();
    }

    private String sessionVon(String name) {
        return Objects.requireNonNull(sessionByName.get(name), "kein Beitritt fuer " + name);
    }
}
