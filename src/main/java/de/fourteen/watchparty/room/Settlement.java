package de.fourteen.watchparty.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

/**
 * Die Punkte-Oekonomie einer Runde als reine Funktion, ohne Bezug zu
 * {@link Room}, {@link Player} oder {@code RoomActor}. Liefert Deltas,
 * wendet sie aber nicht an — das macht der Actor beim Uebergang nach
 * RESOLVED. So ist die gesamte Punkte-Oekonomie ohne Raumzustand testbar.
 *
 * {@code balances} wird nur zum Kappen der Nicht-Tipper-Strafe auf den
 * Kontostand gebraucht (Anforderung 8.1); die Auszahlung selbst kennt keine
 * Kontostaende, nur Einsaetze und Anteile.
 */
public final class Settlement {

    private Settlement() {
    }

    /**
     * Das vollstaendige Ergebnis einer Abrechnung. Pool und Annullierung
     * gehoeren hierher und nicht zum Aufrufer: Beide folgen aus denselben
     * Regeln wie die Deltas, und wer sie danebenher selbst ausrechnet,
     * pflegt das Kappen der Strafe (8.1) zwangslaeufig doppelt.
     *
     * {@code pool} ist der Pool im Sinn von Anforderung 7: alle Einsaetze
     * plus die *tatsaechlich eingesammelten* Strafen. Beim Push (8.2) ist er
     * deshalb groesser als das, was umverteilt wird — die Einsaetze gehen
     * zurueck, verteilt werden nur die Strafen. Das ist Absicht: Der Pool
     * beschreibt, was hineingeflossen ist.
     */
    public record Result(Map<String, Integer> deltas, int pool, boolean annulled) {
    }

    public static Result settle(List<Pick> picks, Set<String> nonPickers,
            Map<String, Integer> balances, String winningOutcome, Params params) {
        Map<String, Integer> deltas = new LinkedHashMap<>();

        // 8.4: Ohne einen einzigen Tipp gibt es niemanden, der etwas gewinnen
        // oder verlieren koennte — die Runde wird annulliert, auch fuer
        // Nicht-Tipper. Kein Pool, keine Strafen.
        if (picks.isEmpty()) {
            return new Result(deltas, 0, true);
        }

        int collectedPenalties = 0;
        for (String playerId : nonPickers) {
            int balance = balances.getOrDefault(playerId, 0);
            int collected = Math.min(params.penalty(), balance);
            if (collected > 0) {
                deltas.merge(playerId, -collected, Integer::sum);
                collectedPenalties += collected;
            }
        }

        // Jeder Einsatz wandert erstmal in den Pool (Anforderung 7); wer
        // gewinnt, bekommt seinen Anteil per distributeShares zurueckaddiert.
        for (Pick pick : picks) {
            deltas.merge(pick.playerId(), -pick.stake(), Integer::sum);
        }
        int totalStakes = picks.stream().mapToInt(Pick::stake).sum();
        int pool = totalStakes + collectedPenalties;

        List<Pick> winners = picks.stream()
                .filter(pick -> pick.outcomeId().equals(winningOutcome))
                .toList();

        if (winners.isEmpty()) {
            // 8.2 Push: kein Ausgang getroffen, Einsaetze zurueck, nur die
            // Strafen werden anteilig unter allen Tippern verteilt.
            for (Pick pick : picks) {
                deltas.merge(pick.playerId(), pick.stake(), Integer::sum);
            }
            distributeShares(picks, collectedPenalties, params, deltas);
            return new Result(deltas, pool, false);
        }

        distributeShares(winners, pool, params, deltas);
        return new Result(deltas, pool, false);
    }

    /**
     * Verteilt {@code pool} auf {@code recipients} nach Anteilen
     * {@code max(Einsatz, Mindesteinsatz)} (7.1) und rundet nach dem
     * Groesste-Reste-Verfahren (Hamilton, 7.2), damit die Summe der
     * Auszahlungen exakt {@code pool} ergibt.
     */
    private static void distributeShares(List<Pick> recipients, int pool, Params params,
            Map<String, Integer> deltas) {
        if (pool <= 0 || recipients.isEmpty()) {
            return;
        }

        List<String> order = new ArrayList<>();
        Map<String, Integer> shareOf = new LinkedHashMap<>();
        for (Pick pick : recipients) {
            if (!shareOf.containsKey(pick.playerId())) {
                order.add(pick.playerId());
            }
            shareOf.merge(pick.playerId(), Math.max(pick.stake(), params.minStake()), Integer::sum);
        }
        int totalShares = shareOf.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, Integer> payout = new LinkedHashMap<>();
        Map<String, Long> remainder = new LinkedHashMap<>();
        int distributed = 0;
        for (String playerId : order) {
            long raw = (long) shareOf.get(playerId) * pool;
            int floorPart = (int) (raw / totalShares);
            payout.put(playerId, floorPart);
            remainder.put(playerId, raw % totalShares);
            distributed += floorPart;
        }

        // Rest bekommen die groessten Nachkomma-Reste; bei Gleichstand
        // entscheidet die stabile Reihenfolge der ersten Nennung.
        List<String> byRemainder = new ArrayList<>(order);
        byRemainder.sort((a, b) -> Long.compare(remainder.get(b), remainder.get(a)));
        int remaining = pool - distributed;
        for (int i = 0; i < remaining; i++) {
            payout.merge(byRemainder.get(i), 1, Integer::sum);
        }

        for (String playerId : order) {
            deltas.merge(playerId, payout.get(playerId), Integer::sum);
        }
    }
}
