package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.FakeScheduler;
import de.fourteen.watchparty.application.NoSnapshots;
import de.fourteen.watchparty.application.RecordingClientGateway;
import de.fourteen.watchparty.application.RoomActor;
import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Room;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gemeinsame Infrastruktur fuer Port-to-Port-Stufen (docs/teststrategie.md,
 * Abschnitt 2.2): Eingang ist {@link RoomActor} als Umsetzung von
 * {@code RoomCommands}, Ausgang die {@link RecordingClientGateway}. Beide
 * Test Doubles kommen aus demselben Quellbaum wie jede andere Ebene
 * (Abschnitt 1).
 *
 * {@link #invariantenGeltenWeiterhin()} setzt Abschnitt 3.2 um: Kein Konto
 * negativ ist strukturell durch {@code Points} garantiert (eine negative
 * Buchung wirft dort, bevor sie je sichtbar wird); die Punktesumme --
 * Startguthaben mal Anzahl beigetretener Spieler -- wird hier tatsaechlich
 * nachgerechnet.
 */
abstract class RaumStufen<SELF extends RaumStufen<?>> extends DeutscheStufe<SELF> {

    protected static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    protected final RecordingClientGateway gateway = new RecordingClientGateway();
    protected final FakeClock clock = new FakeClock(START);
    protected final FakeScheduler scheduler = new FakeScheduler();
    protected final RoomActor actor = new RoomActor(clock, scheduler, new NoSnapshots(), gateway);

    private final Map<String, String> sessionByName = new LinkedHashMap<>();
    private int sessionCounter;

    protected void beitreten(String name) {
        String sessionId = name + "-" + (++sessionCounter);
        sessionByName.put(name, sessionId);
        actor.connected(sessionId);
        actor.join(sessionId, name, null);
        actor.awaitIdle();
    }

    protected String sessionVon(String name) {
        return Objects.requireNonNull(sessionByName.get(name), "kein Beitritt fuer " + name);
    }

    protected Messages.State neuesterStatusFuer(String name) {
        return gateway.lastStateFor(sessionVon(name));
    }

    /** Fuer Reconnect (ADR-014): der Token aus dem WELCOME der zuletzt fuer diesen Namen verwendeten Sitzung. */
    protected String tokenVon(String name) {
        return gateway.messagesFor(sessionVon(name)).stream()
                .filter(Messages.Welcome.class::isInstance)
                .map(Messages.Welcome.class::cast)
                .reduce((first, second) -> second)
                .orElseThrow()
                .token();
    }

    protected void trennen(String name) {
        actor.disconnected(sessionVon(name));
        actor.awaitIdle();
    }

    /** Reconnect ueber denselben Token, aber eine neue Sitzung -- wie ein zweites Handy-Tab. */
    protected void wiederverbinden(String name) {
        String neueSession = name + "-reconnect-" + (++sessionCounter);
        String token = tokenVon(name);
        sessionByName.put(name, neueSession);
        actor.connected(neueSession);
        actor.join(neueSession, name, token);
        actor.awaitIdle();
    }

    @SuppressWarnings("unchecked")
    public SELF invariantenGeltenWeiterhin() {
        String ersteSession = sessionByName.values().iterator().next();
        Messages.State status = gateway.lastStateFor(ersteSession);
        int summe = status.players().stream().mapToInt(Messages.PlayerView::points).sum();
        assertThat(summe)
                .as("Punktesumme aendert sich nur durch Beitritt oder Zuruecksetzen (Anforderung 2, Invariante 5)")
                .isEqualTo(Room.STARTING_POINTS.value() * sessionByName.size());
        return (SELF) this;
    }
}
