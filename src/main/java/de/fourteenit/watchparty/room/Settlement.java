package de.fourteenit.watchparty.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

/**
 * Die Punkte-Oekonomie einer Runde als reine Funktion, ohne Bezug zu
 * {@link Room}, {@link Player} oder {@code RoomActor} (mvp-plan.md,
 * Etappe 2). Liefert Deltas, wendet sie aber nicht an — das macht der
 * Actor beim Uebergang nach RESOLVED.
 *
 * {@code balances} wird nur zum Kappen der Nicht-Tipper-Strafe auf den
 * Kontostand gebraucht (Anforderung 8.1); die Auszahlung selbst kennt keine
 * Kontostaende, nur Einsaetze und Anteile.
 */
public final class Settlement {

    private Settlement() {
    }

    public static Map<String, Integer> settle(List<Bet> bets, Set<String> nonBettors,
            Map<String, Integer> balances, String winningOutcome, Params params) {
        Map<String, Integer> deltas = new LinkedHashMap<>();

        // 8.4: Ohne einen einzigen Tipp gibt es niemanden, der etwas gewinnen
        // oder verlieren koennte — die Runde wird annulliert, auch fuer
        // Nicht-Tipper.
        if (bets.isEmpty()) {
            return deltas;
        }

        int collectedPenalties = 0;
        for (String playerId : nonBettors) {
            int balance = balances.getOrDefault(playerId, 0);
            int collected = Math.min(params.penalty(), balance);
            if (collected > 0) {
                deltas.merge(playerId, -collected, Integer::sum);
                collectedPenalties += collected;
            }
        }

        // Jeder Einsatz wandert erstmal in den Pool (Anforderung 7); wer
        // gewinnt, bekommt seinen Anteil per distributeShares zurueckaddiert.
        for (Bet bet : bets) {
            deltas.merge(bet.playerId(), -bet.stake(), Integer::sum);
        }
        int totalStakes = bets.stream().mapToInt(Bet::stake).sum();

        List<Bet> winners = bets.stream()
                .filter(bet -> bet.outcomeId().equals(winningOutcome))
                .toList();

        if (winners.isEmpty()) {
            // 8.2 Push: kein Ausgang getroffen, Einsaetze zurueck, nur die
            // Strafen werden anteilig unter allen Tippern verteilt.
            for (Bet bet : bets) {
                deltas.merge(bet.playerId(), bet.stake(), Integer::sum);
            }
            distributeShares(bets, collectedPenalties, params, deltas);
            return deltas;
        }

        int pool = totalStakes + collectedPenalties;
        distributeShares(winners, pool, params, deltas);
        return deltas;
    }

    /**
     * Verteilt {@code pool} auf {@code recipients} nach Anteilen
     * {@code max(Einsatz, Mindesteinsatz)} (7.1) und rundet nach dem
     * Groesste-Reste-Verfahren (Hamilton, 7.2), damit die Summe der
     * Auszahlungen exakt {@code pool} ergibt.
     */
    private static void distributeShares(List<Bet> recipients, int pool, Params params,
            Map<String, Integer> deltas) {
        if (pool <= 0 || recipients.isEmpty()) {
            return;
        }

        List<String> order = new ArrayList<>();
        Map<String, Integer> shareOf = new LinkedHashMap<>();
        for (Bet bet : recipients) {
            if (!shareOf.containsKey(bet.playerId())) {
                order.add(bet.playerId());
            }
            shareOf.merge(bet.playerId(), Math.max(bet.stake(), params.minStake()), Integer::sum);
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
