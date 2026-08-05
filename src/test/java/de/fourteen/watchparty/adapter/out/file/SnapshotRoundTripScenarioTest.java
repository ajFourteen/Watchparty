package de.fourteen.watchparty.adapter.out.file;

import de.fourteen.watchparty.teststrategy.AdapterTest;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.stufen.SnapshotStufen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Pilotszenario der Adapter-Ebene (Phase 1 der Teststrategie-Umsetzung):
 * Kann der Datei-Adapter alles uebertragen, was der Port
 * ({@link de.fourteen.watchparty.domain.model.RoomSnapshot}) ausdrueckt?
 * Grundlage fuer den vollstaendig gefuellten Raum aus Phase 3.4 (ADR-023).
 */
@AdapterTest
class SnapshotRoundTripScenarioTest extends DeutschesSzenario<SnapshotStufen, SnapshotStufen, SnapshotStufen> {

    @Test
    void schreibenUndLadenErgibtDenselbenStand(@TempDir Path verzeichnis) {
        angenommen().einRaumSnapshotMitEinerAbgeschlossenenRunde(verzeichnis);

        wenn().wirdGeschriebenUndWiederGeladen();

        dann().ergibtWiederExaktDenselbenStand();
    }
}
