package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.domain.model.Bet;
import de.fourteen.watchparty.domain.model.Bets;
import de.fourteen.watchparty.domain.model.Outcome;
import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Phase;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.Player;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.PointsDelta;
import de.fourteen.watchparty.domain.model.Room;
import de.fourteen.watchparty.domain.model.Round;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Projektion vom Raumzustand auf die Nachrichten an den Client. Rein
 * lesend und ohne eigenen Zustand: was der Client sehen darf, haengt allein
 * vom uebergebenen {@link Room} ab.
 *
 * Steht bewusst hier und nicht im {@code RoomActor}. Der Actor stellt die
 * Reihenfolge her und ruft die Fachlichkeit auf; welche Felder eine
 * Nachricht traegt, ist eine Frage des Protokolls. Dass die Mutatoren von
 * {@link Room} und {@link Round} paket-privat sind, macht die Trennung
 * belastbar: Von hier aus laesst sich der Raum gar nicht veraendern.
 *
 * Hier werden auch die Value Objects der Domaene auf einfache Typen
 * abgewickelt. Das Protokoll ist eine Zusage nach draussen und soll sich
 * nicht aendern, nur weil das Modell innen praeziser geworden ist — dieselbe
 * Ueberlegung wie bei {@code RoomSnapshot} fuer die Platte.
 *
 * Invariante 1 bleibt unberuehrt — aufgerufen wird ausschliesslich vom
 * Raum-Thread, wie jede andere Lesung des Raumzustands.
 */
@Criticality(level = Criticality.Level.HIGH, requirements = { "6-b", "9-b" })
public final class RoomView {

    private RoomView() {
    }

    /**
     * Baut den vollstaendigen Zustand fuer STATE. Welche Felder gesetzt sind,
     * haengt an der Phase: In OPEN nur der Zaehler — kein einzelner Tipp
     * verlaesst den Server, solange das Fenster offen ist (Invariante 4,
     * ADR-013). Erst ab CLOSED liegen die Tipps offen, ab RESOLVED zusaetzlich
     * Ergebnis, Pool und Deltas.
     */
    public static Messages.State state(Room room, long serverNow) {
        List<Messages.PlayerView> views = new ArrayList<>();
        for (Player player : room.players()) {
            views.add(new Messages.PlayerView(
                    player.getId().value(),
                    player.getName().value(),
                    player.getPoints().value(),
                    player.isConnected(),
                    player.isPaused(),
                    room.isHost(player.getId())));
        }

        Round round = room.getCurrentRound();
        Phase phase = room.getPhase();
        Messages.BetView bet = null;
        Long roundId = null;
        Long closesAt = null;
        Integer pickCount = null;
        Integer participantCount = null;
        List<Messages.RevealedPick> revealedPicks = null;
        String winningOutcomeId = null;
        Integer pool = null;
        Boolean annulled = null;
        String annulReason = null;
        Map<String, Integer> deltas = null;

        if (round != null) {
            roundId = round.getId().value();
            bet = bet(round.getBet());

            if (phase == Phase.OPEN) {
                // Invariante 4 / ADR-013: waehrend OPEN nur der Zaehler, nie
                // einzelne Tipps.
                closesAt = round.getClosesAt().toEpochMilli();
                pickCount = round.getPicks().size();
                participantCount = round.getParticipants().size();
            } else if (phase == Phase.CLOSED || phase == Phase.RESOLVED) {
                revealedPicks = new ArrayList<>();
                for (Pick pick : round.picksInOrder()) {
                    revealedPicks.add(new Messages.RevealedPick(
                            pick.playerId().value(), pick.outcomeId().value(), pick.stake().value()));
                }
                if (phase == Phase.RESOLVED) {
                    OutcomeId winner = round.getWinningOutcomeId();
                    winningOutcomeId = winner == null ? null : winner.value();
                    pool = round.getPool().value();
                    annulled = round.isAnnulled();
                    annulReason = annulled ? (round.isAnnulledByHost() ? "HOST" : "NO_PICKS") : null;
                    deltas = deltas(round.getDeltas());
                }
            }
        }

        return new Messages.State(views, hostId(room), phase.name(), roundId, bet,
                closesAt, serverNow, pickCount, participantCount, revealedPicks,
                winningOutcomeId, pool, annulled, annulReason, deltas);
    }

    private static @Nullable String hostId(Room room) {
        PlayerId hostPlayerId = room.getHostPlayerId();
        return hostPlayerId == null ? null : hostPlayerId.value();
    }

    private static @Nullable Map<String, Integer> deltas(@Nullable Map<PlayerId, PointsDelta> deltas) {
        if (deltas == null) {
            return null;
        }
        Map<String, Integer> flach = new LinkedHashMap<>();
        for (Map.Entry<PlayerId, PointsDelta> entry : deltas.entrySet()) {
            flach.put(entry.getKey().value(), entry.getValue().value());
        }
        return flach;
    }

    public static Messages.BetView bet(Bet bet) {
        List<Messages.OutcomeView> outcomes = new ArrayList<>();
        for (Outcome outcome : bet.outcomes()) {
            outcomes.add(new Messages.OutcomeView(outcome.id().value(), outcome.label(), outcome.note()));
        }
        return new Messages.BetView(bet.id().value(), bet.question(), bet.note(), outcomes);
    }

    /** Der ganze Wettkatalog (ADR-017) fuer WELCOME. */
    public static List<Messages.BetView> catalog() {
        return Bets.CATALOG.stream().map(RoomView::bet).toList();
    }
}
