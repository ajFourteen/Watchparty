package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.adapter.out.file.SnapshotStore;
import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.FakeScheduler;
import de.fourteen.watchparty.application.RecordingClientGateway;
import de.fourteen.watchparty.application.RoomActor;
import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.PlayerId;

import com.tngtech.jgiven.annotation.AfterScenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Wiederanlauf aus dem Snapshot (ADR-023) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-c. Jede Generation des
 * {@code RoomActor} teilt sich dieselbe {@link FakeClock} und dieselbe
 * Datei -- ein Neustart ist ein neuer Actor auf demselben Stand. Da eine
 * frische Generation niemanden verbunden hat, sendet sie von sich aus
 * nichts; sichtbar wird der wiederhergestellte Zustand erst, sobald jemand
 * (wieder) beitritt -- genau wie bei einem echten Server-Neustart.
 */
public class WiederanlaufStufen extends DeutscheStufe<WiederanlaufStufen> {

    private static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    private final RecordingClientGateway gateway = new RecordingClientGateway();
    private final FakeClock clock = new FakeClock(START);
    private final Map<String, String> sessionByName = new LinkedHashMap<>();
    private final Map<String, PlayerId> playerIdVorNeustart = new LinkedHashMap<>();
    private int sessionCounter;

    private Path snapshotFile;
    private RoomActor actor;
    private FakeScheduler scheduler;

    /**
     * Jede in diesem Szenario gestartete Generation, damit sie am Ende alle
     * anhalten. Ohne das schreibt ein {@code snapshot-writer}-Thread einer
     * frueheren Generation noch, waehrend JUnit das {@code @TempDir} schon
     * loescht -- sporadisch rot ("Failed to delete temp directory"), und
     * zwar unabhaengig von dem, was das Szenario eigentlich prueft.
     */
    private final List<RoomActor> alleActors = new ArrayList<>();
    private final List<SnapshotStore> alleStores = new ArrayList<>();

    public WiederanlaufStufen dasSnapshotVerzeichnisIst(Path verzeichnis) {
        snapshotFile = verzeichnis.resolve("room.json");
        return this;
    }

    public WiederanlaufStufen derRaumStartet() {
        scheduler = new FakeScheduler();
        SnapshotStore store = new SnapshotStore(snapshotFile);
        actor = new RoomActor(clock, scheduler, store, gateway);
        alleActors.add(actor);
        alleStores.add(store);
        actor.loadOnStartup();
        actor.awaitIdle();
        return this;
    }

    /** Merkt sich die Spieler-IDs, damit nach dem Neustart auf Gleichheit statt nur auf Erreichbarkeit geprueft werden kann. */
    public WiederanlaufStufen derServerWirdNeuGestartet() {
        for (String name : sessionByName.keySet()) {
            playerIdVorNeustart.put(name, gateway.playerIdOf(sessionByName.get(name)));
        }
        // Wartet, bis der Schreib-Thread der vorigen Generation tatsaechlich
        // auf der Platte angekommen ist.
        await().atMost(Duration.ofSeconds(2)).until(() -> Files.exists(snapshotFile));
        return derRaumStartet();
    }

    @AfterScenario
    public void alleGenerationenAnhalten() {
        // Erst abwarten, dass kein Schreibvorgang mehr unterwegs ist, dann
        // erst anhalten -- sonst interrupted shutdown() einen Schreib-Thread
        // mitten im Schreiben, und @TempDir raeumt gegen eine Datei auf, die
        // gerade noch entsteht ("Failed to delete temp directory").
        for (SnapshotStore einStore : alleStores) {
            einStore.awaitWritten();
        }
        for (RoomActor einActor : alleActors) {
            einActor.shutdown();
        }
        for (SnapshotStore einStore : alleStores) {
            einStore.shutdown();
        }
    }

    public WiederanlaufStufen trittBei(String name) {
        String sessionId = name + "-" + (++sessionCounter);
        sessionByName.put(name, sessionId);
        actor.connected(sessionId);
        actor.join(sessionId, name, null);
        actor.awaitIdle();
        return this;
    }

    public WiederanlaufStufen trittMitDemAltenTokenWiederBei(String name) {
        String token = tokenVon(name);
        String sessionId = name + "-reconnect-" + (++sessionCounter);
        sessionByName.put(name, sessionId);
        actor.connected(sessionId);
        actor.join(sessionId, name, token);
        actor.awaitIdle();
        assertThat(gateway.playerIdOf(sessionId))
                .as("Reconnect nach Neustart liefert dieselbe Spieler-ID (ADR-023/ADR-014)")
                .isEqualTo(playerIdVorNeustart.get(name));
        return this;
    }

    public WiederanlaufStufen derHostOeffnetEineWette() {
        actor.openBet(sessionVon("Host"), null);
        actor.awaitIdle();
        return this;
    }

    public WiederanlaufStufen derHostTipptTouchdownMitEinsatz(int einsatz) {
        actor.placePick(sessionVon("Host"), "touchdown", einsatz);
        actor.awaitIdle();
        return this;
    }

    public WiederanlaufStufen derHostSchliesstUndLoestZugunstenVonTouchdownAuf() {
        actor.closeBet(sessionVon("Host"));
        actor.resolve(sessionVon("Host"), "touchdown");
        actor.awaitIdle();
        return this;
    }

    public WiederanlaufStufen dieZeitVergeht(int sekunden) {
        clock.advance(Duration.ofSeconds(sekunden));
        return this;
    }

    public WiederanlaufStufen dieZeitVergehtUeberDieVerfallszeitHinaus() {
        clock.advance(Duration.ofHours(6).plusSeconds(1));
        return this;
    }

    public WiederanlaufStufen istJetztDerEinzigeSpielerUndHost(String name) {
        Messages.State status = neuesterStatusFuer(name);
        assertThat(status.players()).hasSize(1);
        assertThat(status.hostPlayerId()).isEqualTo(gateway.playerIdOf(sessionVon(name)).value());
        return this;
    }

    public WiederanlaufStufen derRaumEnthaeltGenauSpieler(String beobachter, int erwartet) {
        assertThat(neuesterStatusFuer(beobachter).players()).hasSize(erwartet);
        return this;
    }

    public WiederanlaufStufen istWiederVerbunden(String name) {
        String playerId = gateway.playerIdOf(sessionVon(name)).value();
        boolean verbunden = neuesterStatusFuer(name).players().stream()
                .filter(p -> p.id().equals(playerId))
                .findFirst().orElseThrow().connected();
        assertThat(verbunden).isTrue();
        return this;
    }

    public WiederanlaufStufen hatPunkte(String name, int erwartetePunkte) {
        String playerId = gateway.playerIdOf(sessionVon(name)).value();
        int punkte = neuesterStatusFuer(name).players().stream()
                .filter(p -> p.id().equals(playerId))
                .findFirst().orElseThrow().points();
        assertThat(punkte).isEqualTo(erwartetePunkte);
        return this;
    }

    public WiederanlaufStufen dasFensterIstFuer(String beobachter, String erwartetePhase) {
        assertThat(neuesterStatusFuer(beobachter).phase()).isEqualTo(erwartetePhase);
        return this;
    }

    public WiederanlaufStufen einNeuerAutoCloseIstEingeplant() {
        assertThat(scheduler.pendingCount()).isEqualTo(1);
        return this;
    }

    public WiederanlaufStufen kannWeiterhinTippen(String name) {
        actor.placePick(sessionVon(name), "touchdown", 100);
        actor.awaitIdle();
        assertThat(gateway.errorsFor(sessionVon(name))).isEmpty();
        return this;
    }

    public WiederanlaufStufen dasFensterSchliesstZumUrspruenglichVorgesehenenZeitpunkt(String beobachter, int restSekunden) {
        clock.advance(Duration.ofSeconds(restSekunden));
        scheduler.fireAll();
        actor.awaitIdle();
        assertThat(neuesterStatusFuer(beobachter).phase()).isEqualTo("CLOSED");
        return this;
    }

    public WiederanlaufStufen derWettkatalogEintragDerRundeWirdDurchEineUnbekannteWetteErsetzt() throws Exception {
        String kaputt = Files.readString(snapshotFile).replace("\"drive-outcome\"", "\"es-gibt-diese-wette-nicht-mehr\"");
        Files.writeString(snapshotFile, kaputt);
        return this;
    }

    public WiederanlaufStufen dieDateiWirdDurchKaputtesJsonErsetzt() throws Exception {
        Files.writeString(snapshotFile, "{ das ist kein json");
        return this;
    }

    public WiederanlaufStufen keinePersistenzAktiv() {
        scheduler = new FakeScheduler();
        SnapshotStore store = new SnapshotStore(null);
        actor = new RoomActor(clock, scheduler, store, gateway);
        alleActors.add(actor);
        alleStores.add(store);
        actor.loadOnStartup();
        actor.awaitIdle();
        return this;
    }

    public WiederanlaufStufen keineDateiWirdGeschrieben() {
        assertThat(Files.exists(snapshotFile)).isFalse();
        return this;
    }

    private String tokenVon(String name) {
        return gateway.messagesFor(sessionVon(name)).stream()
                .filter(Messages.Welcome.class::isInstance)
                .map(Messages.Welcome.class::cast)
                .reduce((first, second) -> second)
                .orElseThrow()
                .token();
    }

    private String sessionVon(String name) {
        return Objects.requireNonNull(sessionByName.get(name), "kein Beitritt fuer " + name);
    }

    private Messages.State neuesterStatusFuer(String name) {
        return gateway.lastStateFor(sessionVon(name));
    }
}
