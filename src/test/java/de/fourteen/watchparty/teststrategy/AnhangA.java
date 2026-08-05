package de.fourteen.watchparty.teststrategy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Liest Anhang A aus {@code docs/anforderungen.md} ein — bewusst zur
 * Laufzeit aus der einen Datei, keine zweite Wahrheit in einer Kopie
 * (docs/teststrategie.md, Abschnitt 5.2). Wird sowohl von
 * {@link TeststrategyArchitectureTest} (Existenz jeder verwendeten ID) als
 * auch vom Gradle-Task {@code abdeckung} (Differenz zu den grün gelaufenen
 * Szenarien) gebraucht -- Letzterer parst dieselbe Tabelle noch einmal in
 * Kotlin, weil ein Gradle-Task nicht ohne Weiteres Testcode dieses Moduls
 * mitbenutzen kann. Oeffentlich, weil {@code ArchitectureTest} (prueft
 * {@code @Criticality} auf Produktivcode) in einem anderen Paket liegt als
 * {@code TeststrategyArchitectureTest} (prueft {@code @Anforderung} auf
 * Testcode).
 */
public final class AnhangA {

    private AnhangA() {
    }

    /**
     * Eine Tabellenzeile in Anhang A: {@code | ID | Regel-Text | Marke |}.
     * Der Regel-Text selbst wird nicht gebraucht, nur ID und Marke.
     */
    private static final Pattern ZEILE = Pattern.compile(
            "^\\|\\s*([0-9]+(?:\\.[0-9]+)?(?:-[a-z])?)\\s*\\|.*\\|\\s*"
                    + "(backend|frontend|organisatorisch|beobachtung)\\s*\\|\\s*$");

    private static final String ABSCHNITT_MARKE = "## Anhang A";

    /** ID -> Marke, für jede Regel in Anhang A. */
    public static Map<String, String> alleRegeln() {
        Map<String, String> regeln = new LinkedHashMap<>();
        boolean inAnhangA = false;
        for (String zeile : zeilenDerAnforderungen()) {
            if (zeile.startsWith(ABSCHNITT_MARKE)) {
                inAnhangA = true;
                continue;
            }
            if (!inAnhangA) {
                continue;
            }
            Matcher matcher = ZEILE.matcher(zeile);
            if (matcher.matches()) {
                regeln.put(matcher.group(1), matcher.group(2));
            }
        }
        return regeln;
    }

    /** Alle IDs mit der Marke {@code backend} -- die Bezugsmenge der Feature-Abdeckung. */
    public static Set<String> backendRegeln() {
        return alleRegeln().entrySet().stream()
                .filter(eintrag -> eintrag.getValue().equals("backend"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static Iterable<String> zeilenDerAnforderungen() {
        Path pfad = Path.of("docs", "anforderungen.md");
        try {
            return Files.readAllLines(pfad);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Anhang A konnte nicht gelesen werden (Arbeitsverzeichnis: "
                            + Path.of("").toAbsolutePath() + ")", e);
        }
    }
}
