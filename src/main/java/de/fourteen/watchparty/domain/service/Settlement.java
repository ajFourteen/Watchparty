package de.fourteen.watchparty.domain.service;

import org.jmolecules.ddd.annotation.Service;

import de.fourteen.watchparty.criticality.Criticality;
import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Params;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PointsDelta;
import de.fourteen.watchparty.domain.model.Share;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Die Punkte-Oekonomie einer Runde als <b>Domain Service</b>: eine reine
 * Funktion ohne Bezug zu {@code Room}, {@code Player} oder dem Actor.
 *
 * Steht hier und nicht am Aggregat, weil die Rechnung zu keiner einzelnen
 * Entity gehoert — sie betrifft alle Tipper einer Runde gleichzeitig. Sie
 * liefert Deltas, wendet sie aber nicht an; das macht {@code Room} beim
 * Uebergang nach RESOLVED (ADR-020). So ist die gesamte Punkte-Oekonomie
 * ohne Raumzustand testbar.
 *
 * {@code balances} wird nur zum Kappen der Nicht-Tipper-Strafe auf den
 * Kontostand gebraucht (Anforderung 8.1); die Auszahlung selbst kennt keine
 * Kontostaende, nur Einsaetze und Anteile.
 */
@Service
@Criticality(level = Criticality.Level.HIGH,
        requirements = { "7.1", "7.1-a", "7.2", "8.1-c", "8.2", "8.2-a", "8.3", "8.5" })
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
    public record Result(Map<PlayerId, PointsDelta> deltas, Points pool, boolean annulled) {
    }

    public static Result settle(List<Pick> picks, Set<PlayerId> nonPickers,
            Map<PlayerId, Points> balances, OutcomeId winningOutcome, Params params) {
        Map<PlayerId, PointsDelta> deltas = new LinkedHashMap<>();

        // 8.4: Ohne einen einzigen Tipp gibt es niemanden, der etwas gewinnen
        // oder verlieren koennte — die Runde wird annulliert, auch fuer
        // Nicht-Tipper. Kein Pool, keine Strafen.
        if (picks.isEmpty()) {
            return new Result(deltas, Points.ZERO, true);
        }

        Points collectedPenalties = Points.ZERO;
        for (PlayerId playerId : nonPickers) {
            Points balance = balances.getOrDefault(playerId, Points.ZERO);
            // Anforderung 8.1: eingesammelt wird min(Strafe, Kontostand),
            // damit kein Konto negativ wird (Invariante 5).
            Points collected = params.penalty().min(balance);
            if (!collected.isZero()) {
                merge(deltas, playerId, PointsDelta.loss(collected));
                collectedPenalties = collectedPenalties.plus(collected);
            }
        }

        // Jeder Einsatz wandert erstmal in den Pool (Anforderung 7); wer
        // gewinnt, bekommt seinen Anteil per distributeShares zurueckaddiert.
        Points totalStakes = Points.ZERO;
        for (Pick pick : picks) {
            merge(deltas, pick.playerId(), PointsDelta.loss(pick.stake()));
            totalStakes = totalStakes.plus(pick.stake());
        }
        Points pool = totalStakes.plus(collectedPenalties);

        List<Pick> winners = picks.stream().filter(pick -> pick.isOn(winningOutcome)).toList();

        if (winners.isEmpty()) {
            // 8.2 Push: kein Ausgang getroffen, Einsaetze zurueck, nur die
            // Strafen werden anteilig unter allen Tippern verteilt.
            for (Pick pick : picks) {
                merge(deltas, pick.playerId(), PointsDelta.gain(pick.stake()));
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
     * Groessste-Reste-Verfahren (Hamilton, 7.2), damit die Summe der
     * Auszahlungen exakt {@code pool} ergibt.
     *
     * Anteile sind {@link Share}, ausgezahlt wird in {@link Points}. Die
     * beiden Einheiten koennen sich hier nicht mehr vermischen — genau die
     * Trennung, die Anforderung 7 verlangt.
     */
    private static void distributeShares(List<Pick> recipients, Points pool, Params params,
            Map<PlayerId, PointsDelta> deltas) {
        if (pool.isZero() || recipients.isEmpty()) {
            return;
        }

        List<PlayerId> order = new ArrayList<>();
        Map<PlayerId, Share> shareOf = new LinkedHashMap<>();
        for (Pick pick : recipients) {
            if (!shareOf.containsKey(pick.playerId())) {
                order.add(pick.playerId());
            }
            shareOf.merge(pick.playerId(), pick.share(params), Share::plus);
        }
        int totalShares = shareOf.values().stream().mapToInt(Share::value).sum();

        Map<PlayerId, Integer> payout = new LinkedHashMap<>();
        Map<PlayerId, Long> remainder = new LinkedHashMap<>();
        int distributed = 0;
        for (PlayerId playerId : order) {
            long raw = (long) requireShare(shareOf, playerId).value() * pool.value();
            int floorPart = (int) (raw / totalShares);
            payout.put(playerId, floorPart);
            remainder.put(playerId, raw % totalShares);
            distributed += floorPart;
        }

        // Rest bekommen die groessten Nachkomma-Reste; bei Gleichstand
        // entscheidet die stabile Reihenfolge der ersten Nennung.
        //
        // Die Objects.requireNonNull-Aufrufe unten sind keine Verteidigung
        // gegen einen echten Fehlerfall, sondern die Invariante explizit
        // gemacht: order/payout/remainder werden Zeile fuer Zeile aus
        // denselben Spielern aufgebaut, ein get() kann hier strukturell nicht
        // leer sein. NullAway kann das nicht selbst sehen; die Assertion
        // sagt es ihm (und der naechsten Person, die das hier liest).
        List<PlayerId> byRemainder = new ArrayList<>(order);
        byRemainder.sort((a, b) -> Long.compare(
                Objects.requireNonNull(remainder.get(b)),
                Objects.requireNonNull(remainder.get(a))));
        int remaining = pool.value() - distributed;
        for (int i = 0; i < remaining; i++) {
            payout.merge(byRemainder.get(i), 1, Integer::sum);
        }

        for (PlayerId playerId : order) {
            int share = Objects.requireNonNull(payout.get(playerId));
            merge(deltas, playerId, PointsDelta.gain(Points.of(share)));
        }
    }

    private static Share requireShare(Map<PlayerId, Share> shareOf, PlayerId playerId) {
        return Objects.requireNonNull(shareOf.get(playerId));
    }

    private static void merge(Map<PlayerId, PointsDelta> deltas, PlayerId playerId, PointsDelta delta) {
        deltas.merge(playerId, delta, PointsDelta::plus);
    }
}
