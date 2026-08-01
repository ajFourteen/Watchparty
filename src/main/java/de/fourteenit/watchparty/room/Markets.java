package de.fourteenit.watchparty.room;

import java.util.List;

/**
 * Der Marktkatalog. Zum Start genau ein Markt (Anforderung 4), die sieben
 * kanonischen Drive-Ausgaenge aus Anforderung 4.1.
 */
public final class Markets {

    private Markets() {
    }

    public static final Market DRIVE_OUTCOME = new Market(
            "drive-outcome",
            "Ausgang des naechsten Drives",
            List.of(
                    new Outcome("touchdown", "Touchdown", null),
                    new Outcome("field-goal", "Field Goal", "nur bei erfolgreichem Kick"),
                    new Outcome("punt", "Punt", null),
                    new Outcome("turnover", "Turnover", "Interception oder verlorener Fumble"),
                    new Outcome("turnover-on-downs", "Turnover on Downs",
                            "umfasst auch den verschossenen Field Goal (Gegner uebernimmt am Ort)"),
                    new Outcome("safety", "Safety", null),
                    new Outcome("end-of-half", "End of Half / Game",
                            "Drive laeuft mit Halbzeit- oder Spielende aus")));
}
