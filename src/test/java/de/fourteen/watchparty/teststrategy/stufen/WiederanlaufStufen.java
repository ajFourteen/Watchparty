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

/**
 * Wiederanlauf aus dem Snapshot (ADR-023) auf der Port-to-Port-Ebene
 * (docs/teststrategie.md, Abschnitt 2.2), belegt 1-c, 1-j. Jede Generation
 * des {@code RoomActor} teilt sich dieselbe {@link FakeClock} und dasselbe
 * Verzeichnis -- ein Neustart ist ein neuer Actor auf demselben Stand. Da
 * eine frische Generation niemanden verbunden hat, sendet sie von sich aus
 * nichts; sichtbar wird der wiederhergestellte Zustand erst, sobald jemand
 * (wieder) beitritt -- genau wie bei einem echten Server-Neustart.
 *
 * Seit ADR-033 kann ein Verzeichnis mehrere Watchpartys enthalten. Die
 * meisten Szenarien hier drehen sich weiterhin um eine einzelne ({@link
 * #raumCode}); wer bewusst zwei parallele Watchpartys braucht (Kriterium 17,
 * der Aufraeum-Sweep), nutzt die Gegenstuecke mit "Zweite" im Namen.
 */
public class WiederanlaufStufen extends DeutscheStufe<WiederanlaufStufen> {

    private static final Instant START = Instant.parse("2026-08-01T20:00:00Z");

    private final RecordingClientGateway gateway = new RecordingClientGateway();
    private final FakeClock clock = new FakeClock(START);
    private final Map<String, String> sessionByName = new LinkedHashMap<>();
    private final Map<String, PlayerId> playerIdVorNeustart = new LinkedHashMap<>();
    private int sessionCounter;

    private Path snapshotDirectory;
    private RoomActor actor;
    private FakeScheduler scheduler;

    /** Der Code der (ersten) Watchparty dieses Szenarios -- vom ersten {@link #trittBei} erfragt. */
    private String raumCode;

    /** Der Code einer zweiten, parallelen Watchparty (Kriterium 17, Aufraeum-Sweep). */
    private String zweiterRaumCode;

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
        snapshotDirectory = verzeichnis;
        return this;
    }

    public WiederanlaufStufen derRaumStartet() {
        scheduler = new FakeScheduler();
        SnapshotStore store = new SnapshotStore(snapshotDirectory);
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
        // Wartet, bis der Schreib-Thread der vorigen Generation alle bis
        // hierhin eingereihten Staende tatsaechlich geschrieben hat --
        // unabhaengig davon, wie viele Watchpartys das waren (ADR-033).
        alleStores.get(alleStores.size() - 1).awaitWritten();
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
        if (raumCode == null) {
            actor.createRoom(sessionId, name);
        } else {
            actor.join(sessionId, name, null, raumCode);
        }
        actor.awaitIdle();
        if (raumCode == null) {
            raumCode = roomCodeVon(name);
        }
        return this;
    }

    /**
     * Wie {@link #trittBei}, erzwingt aber eine <em>neue</em> Watchparty,
     * unabhaengig davon, was {@link #raumCode} gerade traegt -- fuer die
     * Faelle, in denen die alte Watchparty den Neustart nachweislich nicht
     * ueberlebt hat (abgelaufener oder kaputter Snapshot) und ein
     * versehentliches Wiederverwenden des alten Codes den Test nur zufaellig
     * bestehen liesse.
     */
    public WiederanlaufStufen trittBeiInEinerNeuenWatchparty(String name) {
        String sessionId = name + "-" + (++sessionCounter);
        sessionByName.put(name, sessionId);
        actor.connected(sessionId);
        actor.createRoom(sessionId, name);
        actor.awaitIdle();
        raumCode = roomCodeVon(name);
        return this;
    }

    public WiederanlaufStufen trittEinerZweitenWatchpartyBei(String name) {
        String sessionId = name + "-" + (++sessionCounter);
        sessionByName.put(name, sessionId);
        actor.connected(sessionId);
        if (zweiterRaumCode == null) {
            actor.createRoom(sessionId, name);
        } else {
            actor.join(sessionId, name, null, zweiterRaumCode);
        }
        actor.awaitIdle();
        if (zweiterRaumCode == null) {
            zweiterRaumCode = roomCodeVon(name);
        }
        return this;
    }

    public WiederanlaufStufen trittMitDemAltenTokenWiederBei(String name) {
        String sessionId = wiederverbinden(name, raumCode);
        assertThat(gateway.playerIdOf(sessionId))
                .as("Reconnect nach Neustart liefert dieselbe Spieler-ID (ADR-023/ADR-014)")
                .isEqualTo(playerIdVorNeustart.get(name));
        return this;
    }

    public WiederanlaufStufen trittMitDemAltenTokenWiederBeiDerZweitenWatchparty(String name) {
        String sessionId = wiederverbinden(name, zweiterRaumCode);
        assertThat(gateway.playerIdOf(sessionId))
                .as("Reconnect nach Neustart liefert dieselbe Spieler-ID (ADR-023/ADR-014)")
                .isEqualTo(playerIdVorNeustart.get(name));
        return this;
    }

    private String wiederverbinden(String name, String code) {
        String token = tokenVon(name);
        String sessionId = name + "-reconnect-" + (++sessionCounter);
        sessionByName.put(name, sessionId);
        actor.connected(sessionId);
        actor.join(sessionId, name, token, code);
        actor.awaitIdle();
        return sessionId;
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

    /**
     * Fuer Kriterium 14/15: die erste Watchparty ist seit mehr als sechs
     * Stunden inaktiv, die zweite noch nicht -- beide teilen sich dieselbe
     * Uhr, deshalb wird zunaechst so weit vorgespult, dass beide "alt" waeren,
     * und danach in der zweiten Watchparty frische Aktivitaet ausgeloest.
     */
    public WiederanlaufStufen dieErsteWatchpartyIstUeberDieVerfallszeitHinausInaktivDieZweiteNicht(String hostZweite) {
        clock.advance(Duration.ofHours(6).plusMinutes(5));
        // Ein zustandsaenderndes Kommando -- nur das aktualisiert lastActivity
        // (Anforderung 1-j: dieselbe Marke wie das Speichern des Snapshots).
        actor.openBet(sessionVon(hostZweite), null);
        actor.awaitIdle();
        return this;
    }

    public WiederanlaufStufen wirdAufgeraeumt() {
        scheduler.fireAll();
        actor.awaitIdle();
        // delete() reiht nur ein (Invariante 2) -- ohne das hier koennte die
        // folgende Pruefung die Datei noch sehen.
        alleStores.get(alleStores.size() - 1).awaitWritten();
        return this;
    }

    /** Ein neuer Beitrittsversuch mit dem alten Code scheitert -- die Watchparty existiert nicht mehr. */
    public WiederanlaufStufen dieWatchpartyExistiertNichtMehr() {
        String sessionId = "Sonde-" + (++sessionCounter);
        actor.connected(sessionId);
        actor.join(sessionId, "Sonde", null, raumCode);
        actor.awaitIdle();
        assertThat(gateway.errorsFor(sessionId)).contains("Unbekannter Raum-Code.");
        return this;
    }

    public WiederanlaufStufen derSnapshotDerErstenWatchpartyIstVonDerPlatteVerschwunden() {
        assertThat(Files.exists(snapshotFileFuer(raumCode))).isFalse();
        return this;
    }

    /** Ein neuer Beitrittsversuch mit dem Code der zweiten Watchparty gelingt weiterhin. */
    public WiederanlaufStufen dieZweiteWatchpartyExistiertWeiterhin() {
        String sessionId = "Sonde2-" + (++sessionCounter);
        actor.connected(sessionId);
        actor.join(sessionId, "Sonde", null, zweiterRaumCode);
        actor.awaitIdle();
        assertThat(gateway.errorsFor(sessionId)).isEmpty();
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

    /**
     * Seit ADR-033 plant {@code handleRestore} zusaetzlich zum Auto-Close
     * immer den wiederkehrenden Aufraeum-Sweep (Anforderung 1-j) ein --
     * deshalb zwei ausstehende Tasks, nicht mehr einer.
     */
    public WiederanlaufStufen einNeuerAutoCloseIstEingeplant() {
        assertThat(scheduler.pendingCount())
                .as("Auto-Close-Task der wiederhergestellten Runde plus der wiederkehrende Aufraeum-Sweep")
                .isEqualTo(2);
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
        Path datei = snapshotFileFuer(raumCode);
        String kaputt = Files.readString(datei).replace("\"drive-outcome\"", "\"es-gibt-diese-wette-nicht-mehr\"");
        Files.writeString(datei, kaputt);
        return this;
    }

    public WiederanlaufStufen dieDateiWirdDurchKaputtesJsonErsetzt() throws Exception {
        Files.writeString(snapshotFileFuer(raumCode), "{ das ist kein json");
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
        assertThat(Files.exists(snapshotFileFuer(raumCode))).isFalse();
        return this;
    }

    private Path snapshotFileFuer(String code) {
        return Objects.requireNonNull(snapshotDirectory, "kein Snapshot-Verzeichnis gesetzt")
                .resolve(Objects.requireNonNull(code, "noch keine Watchparty bekannt") + ".json");
    }

    private String tokenVon(String name) {
        return welcomeVon(name).token();
    }

    private String roomCodeVon(String name) {
        return welcomeVon(name).roomCode();
    }

    private Messages.Welcome welcomeVon(String name) {
        return gateway.messagesFor(sessionVon(name)).stream()
                .filter(Messages.Welcome.class::isInstance)
                .map(Messages.Welcome.class::cast)
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private String sessionVon(String name) {
        return Objects.requireNonNull(sessionByName.get(name), "kein Beitritt fuer " + name);
    }

    private Messages.State neuesterStatusFuer(String name) {
        return gateway.lastStateFor(sessionVon(name));
    }
}
