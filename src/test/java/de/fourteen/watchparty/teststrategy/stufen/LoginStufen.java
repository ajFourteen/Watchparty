package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.application.FakeClock;
import de.fourteen.watchparty.application.league.FakeAccountRepository;
import de.fourteen.watchparty.application.league.FakeAccountSessionRepository;
import de.fourteen.watchparty.application.league.FakeLoginLinkRepository;
import de.fourteen.watchparty.application.league.FakeRateLimiter;
import de.fourteen.watchparty.application.league.LoginService;
import de.fourteen.watchparty.application.league.RecordingMailSender;
import de.fourteen.watchparty.application.league.port.in.LoginCommands;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.AccountSession;
import de.fourteen.watchparty.domain.model.league.ClientIp;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.LoginLink;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port-to-Port-Stufe (docs/teststrategie.md, Abschnitt 2.2) fuer die
 * Anmeldung (ADR-036): Eingang ist {@link LoginService} als Umsetzung von
 * {@link LoginCommands}, Ausgaenge sind handgeschriebene Test Doubles
 * (ADR-025) statt einer echten Datenbank oder eines echten Mailversands —
 * dieselbe Aufteilung wie {@link RaumStufen} fuer {@code RoomActor}.
 */
public class LoginStufen extends DeutscheStufe<LoginStufen> {

    private static final Instant START = Instant.parse("2026-08-17T20:00:00Z");
    private static final ClientIp IP = ClientIp.of("203.0.113.1");

    private final FakeClock clock = new FakeClock(START);
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeLoginLinkRepository loginLinks = new FakeLoginLinkRepository();
    private final FakeAccountSessionRepository sessions = new FakeAccountSessionRepository();
    private final RecordingMailSender mailSender = new RecordingMailSender();
    private final FakeRateLimiter rateLimiter = new FakeRateLimiter();
    private final LoginCommands login = new LoginService(clock, accounts, loginLinks, sessions, mailSender, rateLimiter);

    private Optional<AccountSession> letzteAntwort = Optional.empty();

    public LoginStufen keinKontoExistiertFuer(String email) {
        assertThat(accounts.findByEmail(EmailAddress.of(email))).isEmpty();
        return self();
    }

    public LoginStufen einKontoMitNamenExistiertFuer(String name, String email) {
        accounts.save(Account.of(EmailAddress.of(email), DisplayName.of(name), clock.instant()));
        return self();
    }

    public LoginStufen dasRateLimitFuerDieAdresseGreiftBereits(String email) {
        rateLimiter.sperre("email:" + email);
        return self();
    }

    public LoginStufen fordertMitNamenEinenLinkAnFuer(String name, String email) {
        login.requestLink(EmailAddress.of(email), DisplayName.of(name), IP);
        return self();
    }

    public LoginStufen fordertFuerBeideAdressenEinenLinkAn(String emailA, String emailB) {
        login.requestLink(EmailAddress.of(emailA), DisplayName.of("Name A"), IP);
        login.requestLink(EmailAddress.of(emailB), DisplayName.of("Name B"), IP);
        return self();
    }

    public LoginStufen loestDenLetztenLinkFuerEin(String email) {
        letzteAntwort = login.redeemLink(letzterLinkFuer(email).getToken());
        return self();
    }

    /** Loest exakt den zuletzt ausgestellten Token ein zweites Mal ein — derselbe Token wie beim ersten Mal. */
    public LoginStufen loestDenselbenLinkNochEinmalEinFuer(String email) {
        letzteAntwort = login.redeemLink(letzterLinkFuer(email).getToken());
        return self();
    }

    public LoginStufen wirdIhrKontoGeloescht(String email) {
        login.deleteAccount(EmailAddress.of(email));
        return self();
    }

    public LoginStufen vergehenNeunzigTageMinusEineSekunde() {
        clock.advance(Duration.ofDays(90).minusSeconds(1));
        return self();
    }

    public LoginStufen vergehtNochEineSekunde() {
        clock.advance(Duration.ofSeconds(1));
        return self();
    }

    public LoginStufen istSieDamitAngemeldet() {
        assertThat(letzteAntwort).as("Anmeldung sollte erfolgreich sein").isPresent();
        return self();
    }

    public LoginStufen istSieDamitNichtAngemeldet() {
        assertThat(letzteAntwort).as("Anmeldung sollte abgelehnt sein").isEmpty();
        return self();
    }

    public LoginStufen bekommenBeideAdressenGenauEineNachricht(String emailA, String emailB) {
        assertThat(mailSender.gesendeteLinksAn(EmailAddress.of(emailA))).hasSize(1);
        assertThat(mailSender.gesendeteLinksAn(EmailAddress.of(emailB))).hasSize(1);
        return self();
    }

    public LoginStufen wurdeKeineNachrichtVersendet() {
        assertThat(mailSender.anzahlGesendet()).as("es sollte keine Nachricht verschickt worden sein").isZero();
        return self();
    }

    public LoginStufen existiertEinKontoMitDemNamenFuer(String erwarteterName, String email) {
        Optional<Account> account = accounts.findByEmail(EmailAddress.of(email));
        assertThat(account).isPresent();
        assertThat(account.get().getDisplayName()).isEqualTo(DisplayName.of(erwarteterName));
        return self();
    }

    public LoginStufen existiertKeinKontoMehrFuer(String email) {
        assertThat(accounts.findByEmail(EmailAddress.of(email))).isEmpty();
        return self();
    }

    public LoginStufen istIhreSitzungNochGueltig() {
        assertThat(letzteAntwort).isPresent();
        assertThat(letzteAntwort.get().isValid(clock.instant())).isTrue();
        return self();
    }

    public LoginStufen istIhreSitzungAbgelaufen() {
        assertThat(letzteAntwort).isPresent();
        assertThat(letzteAntwort.get().isValid(clock.instant())).isFalse();
        return self();
    }

    public LoginStufen istIhreBisherigeSitzungNichtMehrHinterlegt() {
        assertThat(letzteAntwort).isPresent();
        assertThat(sessions.findByToken(letzteAntwort.get().getToken())).isEmpty();
        return self();
    }

    private LoginLink letzterLinkFuer(String email) {
        var links = mailSender.gesendeteLinksAn(EmailAddress.of(email));
        assertThat(links).as("es sollte ein Anmeldelink fuer " + email + " verschickt worden sein").isNotEmpty();
        return links.get(links.size() - 1);
    }
}
