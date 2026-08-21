package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.adapter.out.db.support.PostgresAdapterSupport;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.Game;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.GameStatus;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueName;
import de.fourteen.watchparty.domain.model.league.Matchday;
import de.fourteen.watchparty.domain.model.league.Membership;
import de.fourteen.watchparty.domain.model.league.Prediction;
import de.fourteen.watchparty.domain.model.league.PredictionId;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.domain.model.league.Team;
import de.fourteen.watchparty.domain.model.league.TeamId;
import de.fourteen.watchparty.teststrategy.AdapterTest;
import de.fourteen.watchparty.teststrategy.Anforderung;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gleichzeitige Schreibzugriffe im Tippspiel (docs/teststrategie.md,
 * Abschnitt 2.3).
 *
 * Diese Ebene braucht das Tippspiel und die Live-Wetten nicht braucht: Bei
 * den Live-Wetten schliesst Invariante 1 gleichzeitige Aenderungen
 * strukturell aus — aller Zustand wird auf dem Raum-Thread veraendert, und
 * ArchUnit haelt das nach. Das Tippspiel laeuft auf Request-Threads gegen
 * ein echtes Postgres; dort ist Gleichzeitigkeit moeglich und muss geprueft
 * werden.
 *
 * Geprueft wird **ohne Threads**, mit ausgeschriebener Verschraenkung: zwei
 * Aufrufer lesen denselben Stand, danach schreiben beide. Das ist genau die
 * Reihenfolge, an der ein Lese-Aendere-Schreibe-Zyklus verliert — nur eben
 * reproduzierbar statt zufaellig. Echte Threads wuerden denselben Fall
 * gelegentlich treffen und waeren damit ein sporadisch fehlschlagender
 * Test, den Abschnitt 10 ausdruecklich verbietet. Es ist dieselbe Antwort
 * wie beim {@code FakeScheduler} auf der Live-Wetten-Seite: Die
 * Verschraenkung wird gesetzt, nicht abgewartet.
 */
@AdapterTest
class NebenlaeufigeSchreibzugriffeTest extends PostgresAdapterSupport {

    private static final EmailAddress CHEF = EmailAddress.of("chefin@example.org");
    private static final EmailAddress ANNA = EmailAddress.of("anna@example.org");
    private static final EmailAddress BEN = EmailAddress.of("ben@example.org");
    private static final Instant JETZT = Instant.parse("2026-09-13T18:00:00Z");

    private final AccountRepositoryJdbc konten = new AccountRepositoryJdbc(JDBC);
    private final LeagueRepositoryJdbc ligen = new LeagueRepositoryJdbc(JDBC);
    private final PredictionRepositoryJdbc tipps = new PredictionRepositoryJdbc(JDBC);
    private final GameRepositoryJdbc spiele = new GameRepositoryJdbc(JDBC);

    @BeforeEach
    void legeKontenAn() {
        for (EmailAddress mail : new EmailAddress[] { CHEF, ANNA, BEN }) {
            konten.save(Account.of(mail, DisplayName.of(mail.value().split("@")[0]), JETZT,
                    false, ReportMailToken.generate()));
        }
    }

    /**
     * 13.6-b: Wer den Code hat, tritt bei — auch dann, wenn jemand anderes
     * im selben Augenblick beitritt.
     *
     * Vor dem Wechsel auf {@code addMember} ging hier ein Beitritt spurlos
     * verloren: Beide Aufrufer lasen dieselbe Liga, und {@code save(League)}
     * ersetzte die Mitgliedschaften vollstaendig, sodass der zweite Schreiber
     * den ersten Beitritt wegloeschte. Am Tisch heisst das: Zwei Freunde
     * loesen denselben Code ein, einer ist danach nicht in der Liga und
     * niemand bekommt eine Fehlermeldung.
     */
    @Test
    @Anforderung("13.6-b")
    void zweiBeitritteAufDemselbenGelesenenStandGehenBeideEin() {
        League liga = League.create(SeasonId.of(2026), LeagueName.of("Bueroliga"), CHEF, JETZT);
        ligen.save(liga);

        League sichtVonAnna = ligen.findByCode(liga.getCode()).orElseThrow();
        League sichtVonBen = ligen.findByCode(liga.getCode()).orElseThrow();

        sichtVonAnna.join(ANNA, JETZT);
        ligen.addMember(sichtVonAnna.getId(), Membership.of(ANNA, JETZT));

        sichtVonBen.join(BEN, JETZT);
        ligen.addMember(sichtVonBen.getId(), Membership.of(BEN, JETZT));

        League endstand = ligen.findById(liga.getId()).orElseThrow();
        assertThat(endstand.isMember(ANNA)).as("Annas Beitritt darf nicht verloren gehen").isTrue();
        assertThat(endstand.isMember(BEN)).as("Bens Beitritt darf nicht verloren gehen").isTrue();
        assertThat(endstand.getMembers()).hasSize(3);
    }

    /**
     * 13.6-b: Zweimal denselben Code einloesen bleibt wirkungslos statt zu
     * scheitern — dieselbe Zusage wie {@code League.join}, nur jetzt in der
     * Datenbankanweisung selbst statt in einem vorgelagerten SELECT, das
     * wieder ein Zeitfenster oeffnen wuerde.
     */
    @Test
    @Anforderung("13.6-b")
    void derselbeBeitrittZweimalGeschriebenLegtKeineZweiteMitgliedschaftAn() {
        League liga = League.create(SeasonId.of(2026), LeagueName.of("Bueroliga"), CHEF, JETZT);
        ligen.save(liga);

        ligen.addMember(liga.getId(), Membership.of(ANNA, JETZT));
        ligen.addMember(liga.getId(), Membership.of(ANNA, JETZT.plusSeconds(5)));

        League endstand = ligen.findById(liga.getId()).orElseThrow();
        assertThat(endstand.getMembers()).hasSize(2);
        assertThat(endstand.getMembers().stream()
                .filter(m -> m.getAccountEmail().equals(ANNA))
                .findFirst().orElseThrow().getJoinedAt())
                .as("Der erste Beitritt zaehlt, der zweite bleibt wirkungslos")
                .isEqualTo(JETZT);
    }

    /**
     * 13.6-d: Austreten trifft nur das austretende Mitglied. Auch das lief
     * frueher ueber den vollstaendigen Ersatz und haette einen gleichzeitigen
     * Beitritt mitgeloescht.
     */
    @Test
    @Anforderung("13.6-d")
    void einAustrittLoeschtKeinenGleichzeitigenBeitritt() {
        League liga = League.create(SeasonId.of(2026), LeagueName.of("Bueroliga"), CHEF, JETZT);
        ligen.save(liga);
        ligen.addMember(liga.getId(), Membership.of(ANNA, JETZT));

        League sichtVonAnna = ligen.findById(liga.getId()).orElseThrow();
        League sichtVonBen = ligen.findById(liga.getId()).orElseThrow();

        sichtVonAnna.leave(ANNA);
        ligen.removeMember(sichtVonAnna.getId(), ANNA);

        sichtVonBen.join(BEN, JETZT);
        ligen.addMember(sichtVonBen.getId(), Membership.of(BEN, JETZT));

        League endstand = ligen.findById(liga.getId()).orElseThrow();
        assertThat(endstand.isMember(ANNA)).isFalse();
        assertThat(endstand.isMember(BEN)).as("Bens Beitritt darf der Austritt nicht mitnehmen").isTrue();
    }

    /**
     * 13.4-c: Ein neuer Tipp ersetzt den alten per Upsert ueber dieselbe
     * PredictionId. Hier ist das Ueberschreiben die *gewollte* Wirkung —
     * anders als bei der Mitgliedschaft geht dabei nichts verloren, was
     * jemand anderes geschrieben hat, weil die Identitaet das Paar
     * (Konto, Spiel) ist. Der Test haelt genau diesen Unterschied fest.
     */
    @Test
    @Anforderung("13.4-c")
    void zweiTippsAufDasselbeSpielUeberschreibenSichKontrolliert() {
        GameId spielId = GameId.of("nebenlaeufig-1");
        spiele.save(Game.of(spielId, Matchday.of(SeasonId.of(2026), 3),
                Team.of(TeamId.of("KC"), "Kansas City Chiefs"), Team.of(TeamId.of("SF"), "San Francisco 49ers"),
                JETZT.plusSeconds(3600), GameStatus.SCHEDULED, null, false));

        tipps.save(Prediction.of(PredictionId.of(ANNA, spielId), GameScore.of(21, 17)));
        tipps.save(Prediction.of(PredictionId.of(ANNA, spielId), GameScore.of(24, 10)));
        tipps.save(Prediction.of(PredictionId.of(BEN, spielId), GameScore.of(7, 35)));

        assertThat(tipps.findByGame(spielId)).hasSize(2);
        assertThat(tippVon(spielId, ANNA)).isEqualTo(GameScore.of(24, 10));
        assertThat(tippVon(spielId, BEN))
                .as("Annas zweiter Tipp darf Bens Tipp nicht beruehren")
                .isEqualTo(GameScore.of(7, 35));
    }

    private GameScore tippVon(GameId spielId, EmailAddress konto) {
        return tipps.findByGame(spielId).stream()
                .filter(t -> t.getId().accountEmail().equals(konto))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Kein Tipp von " + konto.value()))
                .getScore();
    }
}
