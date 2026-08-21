package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.league.FakeAccountRepository;
import de.fourteen.watchparty.application.league.FakeGameRepository;
import de.fourteen.watchparty.application.league.FakePredictionRepository;
import de.fourteen.watchparty.application.league.FakeScheduleFeed;
import de.fourteen.watchparty.application.league.PredictionService;
import de.fourteen.watchparty.application.league.ReportMailService;
import de.fourteen.watchparty.application.league.RecordingMailSender;
import de.fourteen.watchparty.application.league.ScheduleSyncService;
import de.fourteen.watchparty.application.league.port.in.PredictionCommands;
import de.fourteen.watchparty.application.league.port.in.ReportMailCommands;
import de.fourteen.watchparty.application.league.port.in.ScheduleCommands;
import de.fourteen.watchparty.application.league.view.ReportView;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port-to-Port-Stufe (docs/teststrategie.md, Abschnitt 2.2) fuer den
 * Mailversand des Spieltags-Reports (Kapitel 13.9, Feature 010, Schnitt 5):
 * spannt drei zusammenspielende Dienste -- {@link ScheduleSyncService}
 * erkennt den FINAL-Uebergang, {@link ReportMailService} haelt Opt-in und
 * Abmeldung und versendet ueber den {@link RecordingMailSender}, dessen
 * Inhalt aus {@link PredictionService} stammt (dieselbe Bilanz-Berechnung
 * wie die Seite, 13.9-a).
 */
public class ReportMailStufen extends DeutscheStufe<ReportMailStufen> {

    private static final Instant START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Team HOME = Team.of(TeamId.of("KC"), "Kansas City Chiefs");
    private static final Team AWAY = Team.of(TeamId.of("SF"), "San Francisco 49ers");
    private static final Matchday MATCHDAY = Matchday.of(SeasonId.of(2026), 1);

    private final FakeClock clock = new FakeClock(START);
    private final FakeGameRepository games = new FakeGameRepository();
    private final FakeScheduleFeed feed = new FakeScheduleFeed();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakePredictionRepository predictions = new FakePredictionRepository();
    private final RecordingMailSender mailSender = new RecordingMailSender();
    private final PredictionCommands tippen = new PredictionService(clock, games, predictions, accounts);
    private final ReportMailService reportMail = new ReportMailService(accounts, tippen, mailSender);
    private final ReportMailCommands reportMailCommands = reportMail;
    private final ScheduleCommands schedule = new ScheduleSyncService(feed, games, reportMail);

    public ReportMailStufen einKontoMitNamenExistiertFuer(String name, String email) {
        accounts.save(Account.of(EmailAddress.of(email), DisplayName.of(name), clock.instant(),
                false, ReportMailToken.generate()));
        return self();
    }

    public ReportMailStufen hatDenMailversandBestellt(String email) {
        reportMailCommands.optIn(EmailAddress.of(email));
        return self();
    }

    public ReportMailStufen hatDenMailversandAbbestellt(String email) {
        reportMailCommands.optOut(EmailAddress.of(email));
        return self();
    }

    public ReportMailStufen einSpielMitAnstossIn(String gameId, Duration abstand) {
        games.save(Game.of(GameId.of(gameId), MATCHDAY, HOME, AWAY, clock.instant().plus(abstand),
                GameStatus.SCHEDULED, null, false));
        return self();
    }

    public ReportMailStufen tipptFuerDasSpiel(String email, String gameId, int heim, int gast) {
        tippen.submitPrediction(EmailAddress.of(email), GameId.of(gameId), GameScore.of(heim, gast));
        return self();
    }

    public ReportMailStufen derFeedMeldetFuerDasSpielDasEndergebnis(String gameId, int heim, int gast) {
        Game bestehend = games.findById(GameId.of(gameId)).orElseThrow();
        feed.antworteMit(MATCHDAY, Game.of(bestehend.getId(), MATCHDAY, HOME, AWAY, bestehend.getKickoff(),
                GameStatus.FINAL, GameScore.of(heim, gast), false));
        schedule.syncMatchday(MATCHDAY);
        return self();
    }

    public ReportMailStufen derFeedMeldetDasSpielAlsAbgesagt(String gameId) {
        Game bestehend = games.findById(GameId.of(gameId)).orElseThrow();
        feed.antworteMit(MATCHDAY,
                Game.of(bestehend.getId(), MATCHDAY, HOME, AWAY, bestehend.getKickoff(), GameStatus.CANCELLED, null, false));
        schedule.syncMatchday(MATCHDAY);
        return self();
    }

    public ReportMailStufen wirdFuerDasSpielEinErgebnisVonHandGesetzt(String gameId, int heim, int gast) {
        schedule.setResultManually(GameId.of(gameId), GameScore.of(heim, gast));
        return self();
    }

    public ReportMailStufen wirdDerAbmeldelinkVonAufgerufen(String email) {
        ReportMailToken token = accounts.findByEmail(EmailAddress.of(email)).orElseThrow().getReportMailToken();
        reportMailCommands.unsubscribe(token);
        return self();
    }

    public ReportMailStufen wirdEinUnbekannterAbmeldelinkAufgerufen() {
        reportMailCommands.unsubscribe(ReportMailToken.generate());
        return self();
    }

    public ReportMailStufen bekommtEineReportMailFuerDenSpieltag(String email) {
        assertThat(mailSender.gesendeteReportsAn(EmailAddress.of(email))).isNotEmpty();
        return self();
    }

    public ReportMailStufen bekommtKeineReportMail(String email) {
        assertThat(mailSender.gesendeteReportsAn(EmailAddress.of(email))).isEmpty();
        return self();
    }

    public ReportMailStufen bekommtGenauEineReportMail(String email) {
        assertThat(mailSender.gesendeteReportsAn(EmailAddress.of(email))).hasSize(1);
        return self();
    }

    public ReportMailStufen dieReportMailZeigtDieSpieltagssumme(String email, int punkte) {
        ReportView.MatchdayReportView bericht = letzterReport(email);
        assertThat(bericht.totalPoints()).isEqualTo(punkte);
        return self();
    }

    public ReportMailStufen dasOptInIstAktivFuer(String email) {
        assertThat(accounts.findByEmail(EmailAddress.of(email)).orElseThrow().isReportMailOptIn()).isTrue();
        return self();
    }

    public ReportMailStufen dasOptInIstInaktivFuer(String email) {
        assertThat(accounts.findByEmail(EmailAddress.of(email)).orElseThrow().isReportMailOptIn()).isFalse();
        return self();
    }

    private ReportView.MatchdayReportView letzterReport(String email) {
        var reports = mailSender.gesendeteReportsAn(EmailAddress.of(email));
        assertThat(reports).as("es haette bereits eine Report-Mail an " + email + " versendet worden sein sollen").isNotEmpty();
        return reports.get(reports.size() - 1);
    }
}
