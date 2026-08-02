package de.fourteen.watchparty.domain.model;

import java.util.List;

/**
 * Der Wettkatalog (Anforderung 4). Jede Wette ist ein Datensatz, kein
 * Sonderfall im Code (ADR-017) — diese Klasse ist deshalb die einzige
 * Stelle, an der neue Wetten entstehen.
 *
 * Die Ausgaenge einer Wette muessen lueckenlos und ueberschneidungsfrei
 * sein: Jeder reale Verlauf faellt in genau einen Eimer. Wo das nicht
 * offensichtlich ist, haelt {@code note} die Konvention fest — der Host
 * loest von Hand auf und braucht eine Regel, ueber die am Tisch nicht
 * gestritten wird.
 */
public final class Bets {

    private Bets() {
    }

    /** Anforderung 4.1: die sieben kanonischen Drive-Ausgänge. */
    public static final Bet DRIVE_OUTCOME = new Bet(
            BetId.of("drive-outcome"),
            "Ausgang des nächsten Drives",
            null,
            List.of(
                    Outcome.of("touchdown", "Touchdown", null),
                    Outcome.of("field-goal", "Field Goal", "nur bei erfolgreichem Kick"),
                    Outcome.of("punt", "Punt", null),
                    Outcome.of("turnover", "Turnover", "Interception oder verlorener Fumble"),
                    Outcome.of("turnover-on-downs", "Turnover on Downs",
                            "umfasst auch den verschossenen Field Goal (Gegner übernimmt am Ort)"),
                    Outcome.of("safety", "Safety", null),
                    Outcome.of("end-of-half", "End of Half / Game",
                            "Drive läuft mit Halbzeit- oder Spielende aus")));

    public static final Bet FIELD_GOAL = new Bet(
            BetId.of("field-goal-attempt"),
            "Field Goal: gut?",
            null,
            List.of(
                    Outcome.of("good", "Gut", null),
                    Outcome.of("no-good", "Kein Field Goal", "verschossen oder geblockt")));

    /**
     * Extrapunkt und Two-Point Conversion sind eine Wette, nicht zwei: Beim
     * Oeffnen weiss niemand, welche Variante kommt — genau das ist die Frage.
     * Getrennte Wetten haetten den Host gezwungen, die Entscheidung des Teams
     * vorwegzunehmen; lag er falsch, gab es keinen passenden Ausgang. Und die
     * Two-Point-Ausgaenge sind der eigentliche Reiz: Sie werden selten
     * getippt und zahlen deshalb gut (Anforderung 2).
     */
    public static final Bet TRY_AFTER_TOUCHDOWN = new Bet(
            BetId.of("try"),
            "Versuch nach dem Touchdown?",
            "Gilt für den kompletten Versuch, egal ob gekickt wird oder nicht.",
            List.of(
                    Outcome.of("extra-point-good", "Extrapunkt gut", "der Kick sitzt, 1 Punkt"),
                    Outcome.of("extra-point-no-good", "Extrapunkt vergeben",
                            "verschossen, geblockt oder durch Strafe vertan"),
                    Outcome.of("two-point-good", "Two-Point gut", "2 Punkte"),
                    Outcome.of("two-point-no-good", "Two-Point gescheitert",
                            "auch wenn die Verteidigung den Ball zurückträgt")));

    /**
     * Die Schwellen sind eine gesetzte Konvention, keine Liga-Statistik: drei
     * Zahlen, die sich am Tisch merken lassen und die im Fernsehen eingeblendet
     * werden. Getrennt nach Spielzugart, weil zwanzig Yards am Boden etwas
     * anderes wert sind als zwanzig durch die Luft.
     */
    public static final Bet BIG_PLAY = new Bet(
            BetId.of("big-play"),
            "Big Play im nächsten Drive?",
            "Big Play = ein einzelner Spielzug mit Lauf ab 20, Pass ab 30 oder Return ab 50 Yards.",
            List.of(
                    Outcome.of("yes", "Ja", null),
                    Outcome.of("no", "Nein", null)));

    /** Reihenfolge der Host-Auswahl: der Drive-Ausgang zuerst, er läuft am häufigsten. */
    public static final List<Bet> CATALOG =
            List.of(DRIVE_OUTCOME, BIG_PLAY, FIELD_GOAL, TRY_AFTER_TOUCHDOWN);

    /** {@code null}, wenn die ID unbekannt ist — der Aufrufer meldet das als Fehler. */
    public static Bet byId(BetId id) {
        return CATALOG.stream()
                .filter(bet -> bet.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}
