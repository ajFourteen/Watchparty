package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Eine Liga: eine Menge von Mitgliedern und eine Rangliste darueber, fuer
 * genau eine Saison (ADR-034). Aggregate Root, {@link Membership} lebt
 * innerhalb dieses Aggregats.
 *
 * Besitzt keine Ergebnistipps — die gehoeren dem Tipper und dem Spiel
 * (Feature-Dokument), nicht der Liga. Genau deshalb kann ein Konto einer
 * Liga spaeter beitreten und bringt seine bereits abgegebenen Tipps mit
 * (Kriterium 30/34).
 */
@AggregateRoot
public class League {

    @Identity
    private final LeagueId id;
    private final SeasonId season;
    private final LeagueCode code;
    private final LeagueName name;
    private final EmailAddress managerEmail;
    private final List<Membership> members;

    private League(LeagueId id, SeasonId season, LeagueCode code, LeagueName name,
            EmailAddress managerEmail, List<Membership> members) {
        this.id = id;
        this.season = season;
        this.code = code;
        this.name = name;
        this.managerEmail = managerEmail;
        this.members = new ArrayList<>(members);
    }

    /** Legt eine neue Liga an (Kriterium 28) — der anlegende Tipper ist Verwalter und zugleich erstes Mitglied. */
    public static League create(SeasonId season, LeagueName name, EmailAddress manager, Instant now) {
        League league = new League(LeagueId.newId(), season, LeagueCode.random(), name, manager, new ArrayList<>());
        league.join(manager, now);
        return league;
    }

    /** Baut eine bestehende Liga aus ihren gespeicherten Werten wieder auf. */
    public static League of(LeagueId id, SeasonId season, LeagueCode code, LeagueName name,
            EmailAddress managerEmail, List<Membership> members) {
        return new League(id, season, code, name, managerEmail, members);
    }

    /** Kriterium 29: wer den Code hat, tritt bei. Bereits Mitglied zu sein ist kein Fehler, nur ohne Wirkung. */
    public void join(EmailAddress account, Instant now) {
        if (isMember(account)) {
            return;
        }
        members.add(Membership.of(account, now));
    }

    /** Kriterium 34: seine Tipps bleiben bestehen — die gehoeren dem Konto, nicht dieser Liga. */
    public void leave(EmailAddress account) {
        members.removeIf(m -> m.getAccountEmail().equals(account));
    }

    public boolean isMember(EmailAddress account) {
        return members.stream().anyMatch(m -> m.getAccountEmail().equals(account));
    }

    public LeagueId getId() {
        return id;
    }

    public SeasonId getSeason() {
        return season;
    }

    public LeagueCode getCode() {
        return code;
    }

    public LeagueName getName() {
        return name;
    }

    public EmailAddress getManagerEmail() {
        return managerEmail;
    }

    public List<Membership> getMembers() {
        return List.copyOf(members);
    }
}
