package de.fourteen.watchparty.teststrategy.stufen;

import de.fourteen.watchparty.domain.model.OutcomeId;
import de.fourteen.watchparty.domain.model.Params;
import de.fourteen.watchparty.domain.model.Pick;
import de.fourteen.watchparty.domain.model.PlayerId;
import de.fourteen.watchparty.domain.model.Points;
import de.fourteen.watchparty.domain.model.PointsDelta;
import de.fourteen.watchparty.domain.service.Settlement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Das Bausteinvokabular der Domaenen-Ebene fuer {@link Settlement}
 * (docs/teststrategie.md, Abschnitt 2.1). Pilotszenario aus Phase 1 der
 * Teststrategie-Umsetzung, belegt 8.1-c (gekappte Strafe).
 */
public class AbrechnungStufen extends DeutscheStufe<AbrechnungStufen> {

    private static final OutcomeId TOUCHDOWN = OutcomeId.of("touchdown");
    private static final OutcomeId PUNT = OutcomeId.of("punt");

    private final List<Pick> picks = new ArrayList<>();
    private final Set<PlayerId> nichtTipper = new LinkedHashSet<>();
    private final Map<PlayerId, Points> kontostaende = new LinkedHashMap<>();

    private Settlement.Result ergebnis;

    public AbrechnungStufen einTippAufTouchdownMitEinsatz(String spieler, int einsatz) {
        picks.add(new Pick(PlayerId.of(spieler), TOUCHDOWN, Points.of(einsatz)));
        return this;
    }

    public AbrechnungStufen einTippAufPuntMitEinsatz(String spieler, int einsatz) {
        picks.add(new Pick(PlayerId.of(spieler), PUNT, Points.of(einsatz)));
        return this;
    }

    public AbrechnungStufen einNichtTipperMitKontostand(String spieler, int kontostand) {
        PlayerId playerId = PlayerId.of(spieler);
        nichtTipper.add(playerId);
        kontostaende.put(playerId, Points.of(kontostand));
        return this;
    }

    public AbrechnungStufen dieRundeWirdMitAusgangTouchdownAbgerechnet() {
        return dieRundeWirdAbgerechnetGegen(TOUCHDOWN);
    }

    /** Sieger-Ausgang ist die Wette, auf die {@code touchdown} verliert und {@code punt} gewinnt. */
    public AbrechnungStufen dieRundeWirdMitAusgangPuntAbgerechnet() {
        return dieRundeWirdAbgerechnetGegen(PUNT);
    }

    private AbrechnungStufen dieRundeWirdAbgerechnetGegen(OutcomeId siegerAusgang) {
        ergebnis = Settlement.settle(picks, nichtTipper, kontostaende, siegerAusgang, Params.DEFAULT);
        return this;
    }

    public AbrechnungStufen zahlt(String spieler, int deltaWert) {
        assertThat(deltaVon(spieler)).as("Delta fuer " + spieler).isEqualTo(PointsDelta.of(deltaWert));
        return this;
    }

    public AbrechnungStufen bekommtNettoNichtsWeilEinsatzGleichAuszahlungIst(String spieler) {
        return zahlt(spieler, 0);
    }

    public AbrechnungStufen dieSummeAllerDeltasIstExaktNull() {
        assertThat(PointsDelta.sumIsZero(requireErgebnis().deltas().values())).isTrue();
        return this;
    }

    private PointsDelta deltaVon(String spieler) {
        PointsDelta delta = requireErgebnis().deltas().get(PlayerId.of(spieler));
        assertThat(delta).as("kein Delta fuer " + spieler).isNotNull();
        return delta;
    }

    private Settlement.Result requireErgebnis() {
        return java.util.Objects.requireNonNull(ergebnis, "Runde wurde noch nicht abgerechnet");
    }
}
