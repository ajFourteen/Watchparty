package de.fourteen.watchparty.application.league;

import de.fourteen.watchparty.application.league.port.out.AlertSender;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import java.util.ArrayList;
import java.util.List;

/** Handgeschriebenes Test Double (ADR-025, kein Mockito). */
public class FakeAlertSender implements AlertSender {

    public record Alert(SeasonId season, int consecutiveFailedRuns) {
    }

    private final List<Alert> alerts = new ArrayList<>();

    @Override
    public void feedUnreachable(SeasonId season, int consecutiveFailedRuns) {
        alerts.add(new Alert(season, consecutiveFailedRuns));
    }

    public List<Alert> alerts() {
        return alerts;
    }
}
