package de.fourteen.watchparty.adapter.in.ws;

import de.fourteen.watchparty.teststrategy.ApiTest;
import de.fourteen.watchparty.teststrategy.DeutschesSzenario;
import de.fourteen.watchparty.teststrategy.stufen.RundenablaufStufen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pilotszenario der API-Ebene (Phase 1 der Teststrategie-Umsetzung): echter
 * Server, echter Socket, echtes JSON. Prueft die Verdrahtung aus
 * {@code config} anhand eines vollstaendigen Rundenablaufs, keine neue
 * fachliche Abdeckung (docs/teststrategie.md, Abschnitt 2.4).
 *
 * {@code @DirtiesContext}: siehe {@link WireProtocolSmokeTest} -- ohne sie
 * teilt sich dieser Test den gecachten Spring-Kontext und damit den
 * Room-Singleton mit jedem anderen {@code @SpringBootTest} dieser Ebene.
 */
@ApiTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RundenablaufScenarioTest
        extends DeutschesSzenario<RundenablaufStufen, RundenablaufStufen, RundenablaufStufen> {

    @LocalServerPort
    private int port;

    @Test
    void einVollstaendigerRundenablaufUeberEchtenSocket() throws Exception {
        angenommen()
                .einServerLaeuftAufPort(port)
                .und().hostUndAnnaTretenBei();

        wenn()
                .derHostOeffnetDieErsteWetteAusDemKatalog()
                .und().beideTippenAufDenErstenAusgang()
                .und().derHostSchliesstUndLoestZuGunstenDesErstenAusgangsAuf();

        dann().alleSehenAmEndeDiePhaseResolvedMitDemErgebnis();
    }
}
