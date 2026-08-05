package de.fourteen.watchparty.teststrategy.stufen;

import com.tngtech.jgiven.Stage;

/**
 * Deutsche Fortsetzung fuer Stufen, analog zu {@code DeutschesSzenario} fuer
 * die Einstiege: {@code und()} statt {@code and()} (docs/teststrategie.md,
 * Abschnitt 8 -- "Und fuer Fortsetzungen").
 *
 * Jede Stufe dieses Pakets erbt hiervon statt direkt von {@link Stage}.
 */
public abstract class DeutscheStufe<SELF extends DeutscheStufe<?>> extends Stage<SELF> {

    public SELF und() {
        return and();
    }
}
