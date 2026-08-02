package de.fourteen.watchparty.application;

import de.fourteen.watchparty.application.message.Messages;
import de.fourteen.watchparty.domain.model.Bet;
import de.fourteen.watchparty.domain.model.Phase;
import de.fourteen.watchparty.domain.model.Player;
import de.fourteen.watchparty.domain.model.Room;
import de.fourteen.watchparty.domain.model.Round;
import de.fourteen.watchparty.domain.model.Bets;

import java.util.ArrayList;
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
 * Invariante 1 bleibt unberuehrt — aufgerufen wird ausschliesslich vom
 * Raum-Thread, wie jede andere Lesung des Raumzustands.
 *
 * Nicht zu verwechseln mit {@code Room.toSnapshot()}: Das ist der Abzug
 * fuer die Platte (ADR-023), hier entsteht die Sicht fuer den Client.
 */
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
                    player.getId(),
                    player.getName(),
                    player.getPoints(),
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
            roundId = round.getId();
            bet = bet(round.getBet());

            if (phase == Phase.OPEN) {
                // Invariante 4 / ADR-013: waehrend OPEN nur der Zaehler, nie
                // einzelne Tipps.
                closesAt = round.getClosesAt().toEpochMilli();
                pickCount = round.getPicks().size();
                participantCount = round.getParticipants().size();
            } else if (phase == Phase.CLOSED || phase == Phase.RESOLVED) {
                revealedPicks = round.getPicks().values().stream()
                        .map(pick -> new Messages.RevealedPick(pick.playerId(), pick.outcomeId(), pick.stake()))
                        .toList();
                if (phase == Phase.RESOLVED) {
                    winningOutcomeId = round.getWinningOutcomeId();
                    pool = round.getPool();
                    annulled = round.isAnnulled();
                    annulReason = annulled ? (round.isAnnulledByHost() ? "HOST" : "NO_PICKS") : null;
                    deltas = round.getDeltas();
                }
            }
        }

        return new Messages.State(views, room.getHostPlayerId(), phase.name(), roundId, bet,
                closesAt, serverNow, pickCount, participantCount, revealedPicks,
                winningOutcomeId, pool, annulled, annulReason, deltas);
    }

    public static Messages.BetView bet(Bet bet) {
        return new Messages.BetView(bet.id(), bet.question(), bet.note(), bet.outcomes());
    }

    /** Der ganze Wettkatalog (ADR-017) fuer WELCOME. */
    public static List<Messages.BetView> catalog() {
        return Bets.CATALOG.stream().map(RoomView::bet).toList();
    }
}
