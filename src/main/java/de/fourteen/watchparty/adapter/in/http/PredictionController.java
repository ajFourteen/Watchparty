package de.fourteen.watchparty.adapter.in.http;

import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.view.PredictionView;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.SeasonId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Spieltag abrufen und tippen (Kapitel 13.4). */
@RestController
@ConditionalOnProperty(prefix = "watchparty.league.db", name = "url")
class PredictionController {

    private final PredictionCommands predictionCommands;

    PredictionController(PredictionCommands predictionCommands) {
        this.predictionCommands = predictionCommands;
    }

    record PredictionRequest(String gameId, int home, int away) {
    }

    @GetMapping("/api/league/schedule/{seasonYear}/{week}")
    ResponseEntity<PredictionView.MatchdayView> schedule(@AuthenticatedAccount EmailAddress account,
            @PathVariable int seasonYear, @PathVariable int week) {
        return ResponseEntity.ok(predictionCommands.viewMatchday(account, Matchday.of(SeasonId.of(seasonYear), week)));
    }

    @PostMapping("/api/league/predictions")
    ResponseEntity<Void> submit(@AuthenticatedAccount EmailAddress account, @RequestBody PredictionRequest body) {
        predictionCommands.submitPrediction(account, GameId.of(body.gameId()), GameScore.of(body.home(), body.away()));
        return ResponseEntity.ok().build();
    }
}
