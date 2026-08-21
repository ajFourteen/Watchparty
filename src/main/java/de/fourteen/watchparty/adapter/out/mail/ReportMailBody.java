package de.fourteen.watchparty.adapter.out.mail;

import de.fourteen.watchparty.application.league.view.PredictionView;
import de.fourteen.watchparty.application.league.view.ReportView;

/**
 * Formt die Spieltags-Bilanz zu einer schlichten Textmail, an einer Stelle
 * statt in jedem Mail-Adapter einzeln (Behelf, Skill `schneiden`: Text
 * statt gestalteter HTML-Mail). Eine Zeile je Spiel, dieselben Angaben wie
 * die Seite zum Abruf (13.9-a).
 */
final class ReportMailBody {

    private ReportMailBody() {
    }

    static String of(ReportView.MatchdayReportView report) {
        StringBuilder body = new StringBuilder();
        for (ReportView.GameEntryView game : report.games()) {
            body.append("- ").append(game.homeTeamName()).append(" ").append(scoreOf(game.finalScore()))
                    .append(" ").append(game.awayTeamName());
            if (game.ownPrediction() != null) {
                body.append(" (dein Tipp: ").append(scoreOf(game.ownPrediction())).append(")");
            } else {
                body.append(" (kein eigener Tipp)");
            }
            body.append(" -- ").append(game.points()).append(" Punkte\n");
        }
        return body.toString().stripTrailing();
    }

    private static String scoreOf(PredictionView.ScoreView score) {
        return score.home() + ":" + score.away();
    }
}
