package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

import java.security.SecureRandom;

/**
 * Der Beitrittscode einer Liga (Kriterium 29). Value Object.
 *
 * Dieselbe Bauweise wie {@code RoomCode} auf der Live-Wetten-Seite —
 * Zeichen aus {@link #ALPHABET} ohne {@code O}, {@code I}, {@code L}, {@link
 * #parse} faltet Verwechselbares — aber eigenstaendig implementiert, weil
 * die Liga keinen Live-Wetten-Typ importieren darf ({@code
 * ArchitectureTest}). Sechs statt vier Zeichen: Eine Liga lebt eine ganze
 * Saison statt eines Abends, ein laengerer Code senkt das Kollisions- und
 * Errateensrisiko ueber diese laengere Zeit.
 */
@ValueObject
public record LeagueCode(String value) {

    static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTUVWXYZ";
    static final int LENGTH = 6;

    public LeagueCode {
        if (value.length() != LENGTH || !value.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
            throw new IllegalArgumentException(
                    "Ein Liga-Code hat sechs Zeichen aus " + ALPHABET + ", war: " + value);
        }
    }

    public static LeagueCode of(String value) {
        return new LeagueCode(value);
    }

    /** Fuer rohe Eingaben von aussen: getrimmt, grossgeschrieben, verwechselbare Zeichen gefaltet. {@code null} bei ungueltiger Form. */
    public static @Nullable LeagueCode parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String gefaltet = raw.trim().toUpperCase()
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
        try {
            return new LeagueCode(gefaltet);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static LeagueCode random() {
        StringBuilder code = new StringBuilder(LENGTH);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new LeagueCode(code.toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
