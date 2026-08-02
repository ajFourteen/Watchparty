package de.fourteen.watchparty.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die Einsatzregel aus Anforderung 6/8.3, direkt geprueft — ohne Raum,
 * ohne Actor, ohne WebSocket.
 */
class PlayerTest {

    private static final Params PARAMS = Params.DEFAULT;

    private static Player playerWith(int points) {
        return new Player(PlayerId.of("p"), Token.of("t"), PlayerName.of("Anna"), Points.of(points));
    }

    @Test
    void ohneAngabeGiltDerMindesteinsatz() {
        assertThat(playerWith(1000).stakeFor(null, PARAMS)).isEqualTo(Points.of(25));
    }

    @Test
    void einWunschUeberDemMindestEinsatzWirdUebernommen() {
        assertThat(playerWith(1000).stakeFor(200, PARAMS)).isEqualTo(Points.of(200));
    }

    @Test
    void einWunschUnterDemMindesteinsatzWirdAngehoben() {
        assertThat(playerWith(1000).stakeFor(5, PARAMS)).isEqualTo(Points.of(25));
    }

    @Test
    void derKontostandBegrenztNachOben() {
        assertThat(playerWith(80).stakeFor(500, PARAMS)).isEqualTo(Points.of(80));
    }

    /**
     * 8.3: Wer weniger als den Mindesteinsatz hat, geht zwangsweise All-in —
     * sonst koennte er gar nicht mehr tippen.
     */
    @Test
    void unterDemMindesteinsatzGehtEsZwangsweiseAllIn() {
        assertThat(playerWith(10).stakeFor(null, PARAMS)).isEqualTo(Points.of(10));
        assertThat(playerWith(10).stakeFor(25, PARAMS)).isEqualTo(Points.of(10));
        assertThat(playerWith(10).stakeFor(1, PARAMS)).isEqualTo(Points.of(10));
    }

    /**
     * Die Null darf kein absorbierender Zustand sein (8.3): Ein Spieler bei 0
     * Punkten tippt mit Einsatz 0 weiter und kann echte Punkte gewinnen.
     */
    @Test
    void beiNullPunktenBleibtDasTippenMoeglich() {
        assertThat(playerWith(0).stakeFor(null, PARAMS)).isEqualTo(Points.ZERO);
        assertThat(playerWith(0).stakeFor(100, PARAMS)).isEqualTo(Points.ZERO);
    }
}
