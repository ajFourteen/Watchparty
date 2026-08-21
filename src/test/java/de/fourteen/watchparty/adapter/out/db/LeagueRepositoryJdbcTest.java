package de.fourteen.watchparty.adapter.out.db;

import de.fourteen.watchparty.adapter.out.db.support.PostgresAdapterSupport;
import de.fourteen.watchparty.domain.model.league.Account;
import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.ReportMailToken;
import de.fourteen.watchparty.domain.model.league.League;
import de.fourteen.watchparty.domain.model.league.LeagueCode;
import de.fourteen.watchparty.domain.model.league.LeagueId;
import de.fourteen.watchparty.domain.model.league.LeagueName;
import de.fourteen.watchparty.domain.model.league.SeasonId;
import de.fourteen.watchparty.teststrategy.AdapterTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rundlauf gegen echtes Postgres (ADR-035, Abschnitt 2.3): Kann der Adapter
 * alles uebertragen, was {@link de.fourteen.watchparty.application.league.port.out.LeagueRepository}
 * ausdrueckt?
 */
@AdapterTest
class LeagueRepositoryJdbcTest extends PostgresAdapterSupport {

    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private static final EmailAddress ANNA = EmailAddress.of("anna@example.org");
    private static final EmailAddress BEN = EmailAddress.of("ben@example.org");

    private final AccountRepositoryJdbc accounts = new AccountRepositoryJdbc(JDBC);
    private final LeagueRepositoryJdbc repository = new LeagueRepositoryJdbc(JDBC);

    private void legeKontenAn() {
        accounts.save(Account.of(ANNA, DisplayName.of("Anna"), NOW, false, ReportMailToken.generate()));
        accounts.save(Account.of(BEN, DisplayName.of("Ben"), NOW, false, ReportMailToken.generate()));
    }

    @Test
    void eineGespeicherteLigaIstUeberDieIdWiederAuffindbarMitIhremMitglied() {
        legeKontenAn();
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);

        repository.save(league);

        Optional<League> gefunden = repository.findById(league.getId());
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getSeason()).isEqualTo(SeasonId.of(2026));
        assertThat(gefunden.get().getCode()).isEqualTo(league.getCode());
        assertThat(gefunden.get().getName()).isEqualTo(LeagueName.of("Büro-Liga"));
        assertThat(gefunden.get().getManagerEmail()).isEqualTo(ANNA);
        assertThat(gefunden.get().isMember(ANNA)).isTrue();
    }

    @Test
    void istUeberDenCodeWiederAuffindbar() {
        legeKontenAn();
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);
        repository.save(league);

        assertThat(repository.findByCode(league.getCode())).isPresent();
    }

    @Test
    void unbekannterCodeLiefertLeer() {
        assertThat(repository.findByCode(LeagueCode.random())).isEmpty();
    }

    @Test
    void unbekannteIdLiefertLeer() {
        assertThat(repository.findById(LeagueId.newId())).isEmpty();
    }

    @Test
    void speichernSchreibtEinenNeuHinzugekommenenBeitrittMit() {
        legeKontenAn();
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);
        repository.save(league);

        league.join(BEN, NOW.plusSeconds(60));
        repository.save(league);

        League gefunden = repository.findById(league.getId()).orElseThrow();
        assertThat(gefunden.isMember(BEN)).isTrue();
        assertThat(gefunden.getMembers()).hasSize(2);
    }

    @Test
    void speichernEntferntEinenAusgetretenenWiederAusDerMitgliederliste() {
        legeKontenAn();
        League league = League.create(SeasonId.of(2026), LeagueName.of("Büro-Liga"), ANNA, NOW);
        league.join(BEN, NOW);
        repository.save(league);

        league.leave(BEN);
        repository.save(league);

        League gefunden = repository.findById(league.getId()).orElseThrow();
        assertThat(gefunden.isMember(BEN)).isFalse();
        assertThat(gefunden.getMembers()).hasSize(1);
    }

    @Test
    void findByMemberLiefertAlleLigenEinesKontos() {
        legeKontenAn();
        League ligaA = League.create(SeasonId.of(2026), LeagueName.of("Liga A"), ANNA, NOW);
        League ligaB = League.create(SeasonId.of(2026), LeagueName.of("Liga B"), BEN, NOW);
        ligaB.join(ANNA, NOW);
        repository.save(ligaA);
        repository.save(ligaB);

        List<League> gefunden = repository.findByMember(ANNA);

        assertThat(gefunden).extracting(League::getId).containsExactlyInAnyOrder(ligaA.getId(), ligaB.getId());
    }
}
