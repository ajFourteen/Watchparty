package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.LeaguePoints;
import de.fourteen.watchparty.domain.service.league.Scoring;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Das Bausteinvokabular der Domaenen-Ebene fuer {@link Scoring}
 * (docs/teststrategie.md, Abschnitt 2.1) — Kapitel 13.5.
 */
public class WertungStufen extends DeutscheStufe<WertungStufen> {

    private GameScore endergebnis;
    private final Map<String, LeaguePoints> wertungen = new LinkedHashMap<>();

    public WertungStufen einSpielEndeteMit(int heim, int gast) {
        endergebnis = GameScore.of(heim, gast);
        return this;
    }

    public WertungStufen tipptMitErgebnis(String tipper, int heim, int gast) {
        wertungen.put(tipper, Scoring.score(GameScore.of(heim, gast), requireEndergebnis()));
        return this;
    }

    public WertungStufen hatWertungspunkte(String tipper, int erwartet) {
        LeaguePoints punkte = wertungen.get(tipper);
        assertThat(punkte).as("keine Wertung fuer " + tipper).isNotNull();
        assertThat(punkte).isEqualTo(new LeaguePoints(erwartet));
        return this;
    }

    private GameScore requireEndergebnis() {
        return Objects.requireNonNull(endergebnis, "Es wurde noch kein Endergebnis angenommen");
    }
}
