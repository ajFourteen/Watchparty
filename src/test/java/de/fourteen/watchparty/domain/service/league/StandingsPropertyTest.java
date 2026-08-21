package de.fourteen.watchparty.domain.service.league;

import de.fourteen.watchparty.domain.model.league.DisplayName;
import de.fourteen.watchparty.domain.model.league.EmailAddress;
import de.fourteen.watchparty.domain.model.league.GameId;
import de.fourteen.watchparty.domain.model.league.GameScore;
import de.fourteen.watchparty.teststrategy.Anforderung;
import de.fourteen.watchparty.teststrategy.UnitTest;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Allaussagen ueber {@link Standings} (docs/teststrategie.md, Abschnitt 4).
 *
 * Die Gleichstandsregel aus 13.6-g ist genau die Bauart, an der
 * Beispieltests vorbeilaufen: Ob die Formel-1-Zaehlung wirklich fuer jede
 * Gruppengroesse stimmt und ob die Reihenfolge der Eingabe die Rangliste
 * unberuehrt laesst, entscheidet kein einzelnes Beispiel. Beides sind
 * Aussagen ueber *alle* Konstellationen und gehoeren deshalb hierher und
 * nicht in ein weiteres JGiven-Szenario.
 */
@UnitTest
class StandingsPropertyTest {

    private static final int SPIELE = 6;

    /**
     * 13.6-g: Die Reihenfolge, in der die Mitglieder hereingereicht werden,
     * darf die Rangliste nicht veraendern. Ein Sortierverfahren, das bei
     * Gleichstand die Eingabereihenfolge durchschlagen laesst, wuerde
     * dieselbe Liga je nach Datenbankantwort verschieden ranken -- und
     * genau das faellt an einem einzelnen Beispiel nicht auf.
     */
    @Property
    @Anforderung("13.6-g")
    void dieEingabereihenfolgeDerMitgliederAendertDieRanglisteNicht(
            @ForAll @Size(min = 1, max = 6) List<@IntRange(min = 0, max = 3) Integer> tippMuster,
            @ForAll long mischung) {

        Aufbau aufbau = Aufbau.aus(tippMuster);

        List<Standings.Entry> original = Standings.compute(aufbau.mitglieder(), aufbau.spiele(), aufbau.tipps());

        List<Standings.Member> gemischt = new ArrayList<>(aufbau.mitglieder());
        Collections.shuffle(gemischt, new Random(mischung));
        List<Standings.Entry> nachMischen = Standings.compute(gemischt, aufbau.spiele(), aufbau.tipps());

        assertThat(platzJeTipper(nachMischen))
                .as("Dieselben Tipper muessen unabhaengig von der Eingabereihenfolge denselben Platz bekommen")
                .isEqualTo(platzJeTipper(original));
    }

    /**
     * 13.6-g: Die Platzvergabe folgt der Formel-1-Zaehlung -- der erste Platz
     * ist 1, jeder weitere Platz ist die Zahl der davor stehenden Tipper plus
     * eins, und Gleichstand teilt den Platz. Damit ist ausgeschlossen, dass
     * nach einem geteilten Platz weitergezaehlt wird (1., 1., 2.).
     */
    @Property
    @Anforderung("13.6-g")
    void geteilteRaengeUeberspringenGenauSoVielePlaetzeWieSieTeilen(
            @ForAll @Size(min = 1, max = 6) List<@IntRange(min = 0, max = 3) Integer> tippMuster) {

        Aufbau aufbau = Aufbau.aus(tippMuster);
        List<Standings.Entry> rangliste = Standings.compute(aufbau.mitglieder(), aufbau.spiele(), aufbau.tipps());

        assertThat(rangliste.get(0).rank()).isEqualTo(1);
        for (int i = 0; i < rangliste.size(); i++) {
            Standings.Entry eintrag = rangliste.get(i);
            long davor = rangliste.stream().filter(anderer -> istBesser(anderer, eintrag)).count();
            assertThat(eintrag.rank())
                    .as("Platz von %s", eintrag.displayName().value())
                    .isEqualTo((int) davor + 1);
        }
    }

    /**
     * 13.6-e: Die Rangliste ist absteigend sortiert -- ein spaeterer Eintrag
     * hat nie mehr Punkte als ein frueherer, und der Platz waechst monoton.
     */
    @Property
    @Anforderung("13.6-e")
    void dieRanglisteIstAbsteigendSortiertUndDiePlaetzeWachsenMonoton(
            @ForAll @Size(min = 1, max = 6) List<@IntRange(min = 0, max = 3) Integer> tippMuster) {

        Aufbau aufbau = Aufbau.aus(tippMuster);
        List<Standings.Entry> rangliste = Standings.compute(aufbau.mitglieder(), aufbau.spiele(), aufbau.tipps());

        for (int i = 1; i < rangliste.size(); i++) {
            assertThat(rangliste.get(i).totalPoints().value())
                    .isLessThanOrEqualTo(rangliste.get(i - 1).totalPoints().value());
            assertThat(rangliste.get(i).rank()).isGreaterThanOrEqualTo(rangliste.get(i - 1).rank());
        }
    }

    /**
     * 13.6-f: Wer nichts getippt hat, steht mit null Punkten in der Liste --
     * ohne Strafe und ohne herauszufallen. Geprueft fuer jede Zahl von
     * Mitgliedern und jedes Ergebnisbild.
     */
    @Property
    @Anforderung("13.6-f")
    void werNichtsGetipptHatStehtMitNullPunktenInDerListe(
            @ForAll @Size(min = 1, max = 6) List<@IntRange(min = 0, max = 3) Integer> tippMuster) {

        Aufbau aufbau = Aufbau.aus(tippMuster);
        List<Standings.Member> mitStiller = new ArrayList<>(aufbau.mitglieder());
        Standings.Member stiller = new Standings.Member(
                EmailAddress.of("stiller@example.org"), DisplayName.of("Stiller"));
        mitStiller.add(stiller);

        List<Standings.Entry> rangliste = Standings.compute(mitStiller, aufbau.spiele(), aufbau.tipps());

        Standings.Entry eintrag = rangliste.stream()
                .filter(e -> e.email().equals(stiller.email()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Wer nicht getippt hat, muss trotzdem in der Rangliste stehen"));
        assertThat(eintrag.totalPoints().value()).isZero();
        assertThat(eintrag.exactCount()).isZero();
        assertThat(eintrag.correctTendencyCount()).isZero();
        assertThat(eintrag.rank()).isEqualTo(rangliste.size() == 1 ? 1 : rangliste.get(rangliste.size() - 1).rank());
    }

    /** Steht {@code a} in der Rangordnung echt vor {@code b}? Dieselben drei Stufen wie 13.6-g. */
    private static boolean istBesser(Standings.Entry a, Standings.Entry b) {
        if (a.totalPoints().value() != b.totalPoints().value()) {
            return a.totalPoints().value() > b.totalPoints().value();
        }
        if (a.exactCount() != b.exactCount()) {
            return a.exactCount() > b.exactCount();
        }
        return a.correctTendencyCount() > b.correctTendencyCount();
    }

    private static Map<EmailAddress, Integer> platzJeTipper(List<Standings.Entry> rangliste) {
        return rangliste.stream().collect(Collectors.toMap(Standings.Entry::email, Standings.Entry::rank));
    }

    /**
     * Baut aus einem erzeugten Muster einen vollstaendigen Ligastand. Jede
     * Zahl im Muster steht fuer einen Tipper und dafuer, wie genau er tippt
     * (0 = gar nicht, 1 = falsche Tendenz, 2 = richtige Tendenz, 3 = exakt).
     * Dadurch entstehen Gleichstaende haeufig statt zufaellig -- genau der
     * Fall, um den es bei 13.6-g geht.
     */
    private record Aufbau(List<Standings.Member> mitglieder, List<Standings.ScoredGame> spiele,
            BiFunction<EmailAddress, GameId, Optional<GameScore>> tipps) {

        static Aufbau aus(List<Integer> tippMuster) {
            List<Standings.Member> mitglieder = IntStream.range(0, tippMuster.size())
                    .mapToObj(i -> new Standings.Member(
                            EmailAddress.of("tipper" + i + "@example.org"), DisplayName.of("Tipper " + i)))
                    .toList();

            List<Standings.ScoredGame> spiele = IntStream.range(0, SPIELE)
                    .mapToObj(i -> new Standings.ScoredGame(GameId.of("spiel-" + i), GameScore.of(24, 17)))
                    .toList();

            BiFunction<EmailAddress, GameId, Optional<GameScore>> tipps = (email, gameId) -> {
                // Wer nicht zu diesem Aufbau gehoert, hat schlicht nicht getippt --
                // so kann ein Test der Rangliste ein zusaetzliches Mitglied
                // hinzufuegen, ohne das Muster umbauen zu muessen (13.6-f).
                if (!email.value().startsWith("tipper")) {
                    return Optional.empty();
                }
                int index = Integer.parseInt(
                        email.value().substring("tipper".length(), email.value().indexOf('@')));
                return switch (tippMuster.get(index)) {
                    case 0 -> Optional.empty();
                    case 1 -> Optional.of(GameScore.of(10, 31));
                    case 2 -> Optional.of(GameScore.of(30, 20));
                    default -> Optional.of(GameScore.of(24, 17));
                };
            };

            return new Aufbau(mitglieder, spiele, tipps);
        }
    }
}
