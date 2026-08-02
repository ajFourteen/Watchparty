package de.fourteen.watchparty;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Haelt die Ringregel der Onion-Architektur nach: Abhaengigkeiten zeigen nur
 * nach innen.
 *
 * Ohne diesen Test ist die Struktur eine Absichtserklaerung — ein einziger
 * bequemer Import genuegt, um sie zu durchloechern, und niemand merkt es. Der
 * Umbau selbst hat drei solche Verstoesse ans Licht gebracht (unter anderem
 * hielt der {@code RoomActor} {@code ClientSession}-Objekte); damit sie nicht
 * zurueckkommen, steht die Regel hier als Test statt nur in der Dokumentation.
 *
 * Geprueft wird auf dem Quelltext, nicht per Bytecode-Analyse: Das kostet
 * keine zusaetzliche Abhaengigkeit und die Importzeile ist genau die Stelle,
 * an der ein Verstoss entsteht.
 */
class ArchitectureTest {

    private static final Path SOURCES = Path.of("src/main/java/de/fourteen/watchparty");
    private static final String BASE = "de.fourteen.watchparty.";

    @Test
    void dieDomaeneKenntWederAnwendungNochAdapter() {
        assertNoImports("domain", BASE + "application", BASE + "adapter", BASE + "config");
    }

    @Test
    void derAnwendungsringKenntKeineAdapter() {
        assertNoImports("application", BASE + "adapter", BASE + "config");
    }

    /**
     * Der Kern bleibt framework-frei: Spring wird ausschliesslich in
     * {@code config} und in den Adaptern verdrahtet. Sonst waere der
     * {@code RoomActor} ohne Spring-Kontext nicht mehr zu instanziieren — und
     * genau das machen die Actor-Tests.
     */
    @Test
    void kernOhneSpring() {
        assertNoImports("domain", "org.springframework", "jakarta.annotation");
        assertNoImports("application", "org.springframework", "jakarta.annotation");
    }

    /**
     * Jackson darf ausschliesslich in {@code application.message} vorkommen.
     *
     * Das ist die eine bewusst zugelassene Ausnahme: Die Nachrichtentypen
     * muessen im Anwendungsring liegen, weil {@code RoomView} sie erzeugt,
     * tragen aber {@code @JsonInclude}/{@code @JsonProperty}. Sie in den
     * Adapter zu schieben hiesse, Invariante 4 (verdeckte Tipps) dorthin zu
     * verlegen; sie ueber Mixins zu entkoppeln waere fuer fuenf Records mehr
     * Zeremonie als Gewinn. Annotationen sind Metadaten, kein Framework-Aufruf
     * — serialisiert wird allein im Adapter.
     */
    @Test
    void jacksonNurInDenNachrichtentypen() {
        List<String> verstoesse = importsMatching("domain", "com.fasterxml.jackson");
        verstoesse.addAll(importsMatching("application", "com.fasterxml.jackson").stream()
                .filter(zeile -> !zeile.startsWith("application/message/"))
                .toList());

        assertThat(verstoesse)
                .as("Jackson gehoert in den Adapter, ausser an den Nachrichtentypen selbst")
                .isEmpty();
    }

    @Test
    void alleQuellenWurdenWirklichGelesen() {
        // Schutz gegen einen gruenen Test, weil der Pfad nicht stimmt.
        assertThat(javaFiles("domain")).hasSizeGreaterThan(5);
        assertThat(javaFiles("application")).isNotEmpty();
    }

    private static void assertNoImports(String ring, String... verbotenePraefixe) {
        List<String> verstoesse = new ArrayList<>();
        for (String praefix : verbotenePraefixe) {
            verstoesse.addAll(importsMatching(ring, praefix));
        }
        assertThat(verstoesse)
                .as("%s darf nicht nach aussen zeigen", ring)
                .isEmpty();
    }

    /** Liefert "pfad: importzeile" fuer jeden Treffer, damit der Fehler die Stelle nennt. */
    private static List<String> importsMatching(String ring, String praefix) {
        List<String> treffer = new ArrayList<>();
        for (Path datei : javaFiles(ring)) {
            try {
                for (String zeile : Files.readAllLines(datei)) {
                    String trimmed = zeile.strip();
                    if (trimmed.startsWith("import ") && trimmed.substring(7).stripLeading().startsWith(praefix)) {
                        treffer.add(SOURCES.relativize(datei) + ": " + trimmed);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return treffer;
    }

    private static List<Path> javaFiles(String ring) {
        Path wurzel = SOURCES.resolve(ring);
        assertThat(wurzel).as("Ring %s gefunden", ring).exists();
        try (Stream<Path> pfade = Files.walk(wurzel)) {
            return pfade.filter(p -> p.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
