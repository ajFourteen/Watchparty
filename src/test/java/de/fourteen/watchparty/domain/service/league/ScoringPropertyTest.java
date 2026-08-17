package de.fourteen.watchparty.domain.service.league;

import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.domain.model.league.LeaguePoints;
import de.fourteen.watchparty.domain.model.league.ScoreBucket;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Allaussagen ueber {@link Scoring} und {@link ScoreBucket}, die ein
 * Beispieltest prinzipiell nicht treffen kann (docs/teststrategie.md,
 * Abschnitt 4; Konsequenzen von ADR-038).
 */
@UnitTest
class ScoringPropertyTest {

    /** 13.5-a/13.5-d: Es gibt nur die vier Wertungsstufen, sonst nichts. */
    @Property
    @Anforderung({ "13.5-a", "13.5-d" })
    void ergebnisIstImmerEineDerVierWertungsstufen(
            @ForAll @IntRange(min = 0, max = 60) int heimTipp,
            @ForAll @IntRange(min = 0, max = 60) int gastTipp,
            @ForAll @IntRange(min = 0, max = 60) int heimErgebnis,
            @ForAll @IntRange(min = 0, max = 60) int gastErgebnis) {

        LeaguePoints punkte = Scoring.score(
                GameScore.of(heimTipp, gastTipp), GameScore.of(heimErgebnis, gastErgebnis));

        assertThat(punkte.value()).isIn(0, 3, 5, 6);
    }

    /** 13.5-a: Ein exakt getroffenes Ergebnis bringt immer 6 Punkte, fuer jedes moegliche Ergebnis. */
    @Property
    @Anforderung("13.5-a")
    void exaktesErgebnisBringtImmerSechsPunkte(
            @ForAll @IntRange(min = 0, max = 60) int heim,
            @ForAll @IntRange(min = 0, max = 60) int gast) {

        GameScore ergebnis = GameScore.of(heim, gast);
        assertThat(Scoring.score(ergebnis, ergebnis)).isEqualTo(LeaguePoints.EXACT);
    }

    /**
     * 13.5-e: Vertauscht man Ergebnistipp und Endergebnis, bleibt die
     * Punktzahl gleich -- Tendenz und Abstand sind bei beiden Argumenten
     * gleichermassen gespiegelt, die Funktion ist symmetrisch.
     */
    @Property
    @Anforderung("13.5-e")
    void vertauschenVonTippUndErgebnisAendertDiePunktzahlNicht(
            @ForAll @IntRange(min = 0, max = 60) int heimTipp,
            @ForAll @IntRange(min = 0, max = 60) int gastTipp,
            @ForAll @IntRange(min = 0, max = 60) int heimErgebnis,
            @ForAll @IntRange(min = 0, max = 60) int gastErgebnis) {

        GameScore tipp = GameScore.of(heimTipp, gastTipp);
        GameScore ergebnis = GameScore.of(heimErgebnis, gastErgebnis);

        assertThat(Scoring.score(tipp, ergebnis)).isEqualTo(Scoring.score(ergebnis, tipp));
    }

    /** 13.5-c: Die Eimergrenzen liegen exakt bei 8/9 und 16/17, fuer jeden moeglichen Abstand. */
    @Property
    @Anforderung("13.5-c")
    void dieEimergrenzenLiegenBeiAchtUndSechzehn(@ForAll @IntRange(min = 0, max = 200) int abstand) {
        ScoreBucket eimer = ScoreBucket.of(abstand);

        if (abstand == 0) {
            assertThat(eimer).isEqualTo(ScoreBucket.UNENTSCHIEDEN);
        } else if (abstand <= 8) {
            assertThat(eimer).isEqualTo(ScoreBucket.EIN_SCORE);
        } else if (abstand <= 16) {
            assertThat(eimer).isEqualTo(ScoreBucket.ZWEI_SCORE);
        } else {
            assertThat(eimer).isEqualTo(ScoreBucket.DREI_PLUS_SCORE);
        }
    }
}
