package de.fourteen.watchparty.teststrategy;

import com.tngtech.jgiven.junit5.ScenarioTest;

/**
 * Deutsche Einstiege statt {@code given()}/{@code when()}/{@code then()}
 * (docs/teststrategie.md, Abschnitt 8): Der Testcode spricht denselben
 * Dialekt wie der JGiven-Report, den eine Fachabteilung liest.
 *
 * Jede JGiven-Szenarioklasse dieses Projekts erbt von hier statt direkt von
 * {@link ScenarioTest}.
 */
public abstract class DeutschesSzenario<GEGEBEN, WENN, DANN> extends ScenarioTest<GEGEBEN, WENN, DANN> {

    protected GEGEBEN angenommen() {
        return given();
    }

    protected WENN wenn() {
        return when();
    }

    protected DANN dann() {
        return then();
    }
}
