package de.fourteen.watchparty.domain.model.league;

import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class LeagueTest {

    private static final Instant NOW = Instant.parse("2026-08-17T20:00:00Z");
    private static final EmailAddress ANNA = EmailAddress.of("anna@example.org");
    private static final EmailAddress BEN = EmailAddress.of("ben@example.org");

    @Test
    @Anforderung("13.6-a")
    void derAnlegendeTipperIstVerwalterUndErstesMitglied() {
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);

        assertThat(league.getManagerEmail()).isEqualTo(ANNA);
        assertThat(league.isMember(ANNA)).isTrue();
        assertThat(league.getMembers()).hasSize(1);
    }

    @Test
    void traegtSaisonNameUndEinenCode() {
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);

        assertThat(league.getSeason()).isEqualTo(SeasonId.of(2026));
        assertThat(league.getName()).isEqualTo(LeagueName.of("Büro-Liga"));
        assertThat(league.getCode()).isNotNull();
    }

    @Test
    void zweiNeueLigenBekommenUnterschiedlicheCodes() {
        League a = League.create(SeasonId.of(2026), LeagueName.of("Liga A"), ANNA, NOW);
        League b = League.create(SeasonId.of(2026), LeagueName.of("Liga B"), ANNA, NOW);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    void joinFuegtEinNeuesMitgliedHinzu() {
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);

        league.join(BEN, NOW);

        assertThat(league.isMember(BEN)).isTrue();
        assertThat(league.getMembers()).hasSize(2);
    }

    @Test
    void joinIstOhneWirkungWennBereitsMitglied() {
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);

        league.join(ANNA, NOW.plusSeconds(60));

        assertThat(league.getMembers()).hasSize(1);
    }

    @Test
    @Anforderung("13.6-d")
    void leaveEntferntDieMitgliedschaft() {
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);
        league.join(BEN, NOW);

        league.leave(BEN);

        assertThat(league.isMember(BEN)).isFalse();
        assertThat(league.getMembers()).hasSize(1);
    }

    @Test
    void leaveEinesNichtMitgliedsIstOhneWirkung() {
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);

        league.leave(BEN);

        assertThat(league.getMembers()).hasSize(1);
    }

    @Test
    void ofBautEineBestehendeLigaUnveraendertWiederAuf() {
        LeagueId id = LeagueId.newId();
        LeagueCode code = LeagueCode.random();
        var members = java.util.List.of(Membership.of(ANNA, NOW));

        League league = League.of(id, SeasonId.of(2026), code, LeagueName.of("Büro-Liga"), ANNA, members);

        assertThat(league.getId()).isEqualTo(id);
        assertThat(league.getCode()).isEqualTo(code);
        assertThat(league.getMembers()).hasSize(1);
    }
}
