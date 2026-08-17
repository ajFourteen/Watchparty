package de.fourteen.watchparty.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

import java.util.Random;

/**
 * Die Identitaet einer Watchparty (ADR-033). Value Object.
 *
 * Vier Zeichen aus {@link #ALPHABET} — Ziffern und Grossbuchstaben ohne
 * {@code O}, {@code I}, {@code L}: Diese lassen sich beim Vorlesen am Tisch
 * leicht mit {@code 0} und {@code 1} verwechseln. Erzeugt wird deshalb nie
 * mit ihnen, angenommen werden sie trotzdem — {@link #parse} faltet sie auf
 * die entsprechende Ziffer, bevor geprueft wird.
 */
@ValueObject
public record RoomCode(String value) {

    static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTUVWXYZ";
    static final int LENGTH = 4;

    public RoomCode {
        if (value.length() != LENGTH || !value.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
            throw new IllegalArgumentException(
                    "Ein Raum-Code hat vier Zeichen aus " + ALPHABET + ", war: " + value);
        }
    }

    /** Fuer bereits kanonische Werte — Snapshot-Restore, das Ergebnis von {@link #random()}. */
    public static RoomCode of(String value) {
        return new RoomCode(value);
    }

    /**
     * Fuer rohe Eingaben von aussen: getrimmt, grossgeschrieben, {@code O}
     * zu {@code 0} und {@code I}/{@code L} zu {@code 1} gefaltet — wer "oh"
     * hoert und {@code O} tippt, kommt trotzdem an. {@code null} bei leerer
     * Eingabe (bedeutet "kein Code") ebenso wie bei einer Eingabe, die auch
     * nach der Faltung keine gueltige Form hat — beides behandelt der
     * Aufrufer gleich: kein Code, keine automatische Fehlermeldung an dieser
     * Stelle.
     */
    public static @Nullable RoomCode parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String gefaltet = raw.trim().toUpperCase()
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
        try {
            return new RoomCode(gefaltet);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Ein zufaelliger Code aus {@link #ALPHABET}. Kollisionsvermeidung
     * gegen bereits vergebene Codes ist bewusst nicht Sache dieser Methode
     * — sie kennt keine anderen Raeume, das weiss nur der Anwendungsring.
     */
    public static RoomCode random() {
        StringBuilder code = new StringBuilder(LENGTH);
        Random random = new Random();
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new RoomCode(code.toString());
    }

    @Override
    public String toString() {
        // Anders als Token: der Code soll ja staendig sichtbar sein (Anforderung 1-k).
        return value;
    }
}
