package de.fourteen.watchparty.domain.model;

/**
 * Der angezeigte Name eines Spielers. Value Object.
 *
 * Die Regel "1 bis 20 Zeichen" stand bisher als if-Abfrage im RoomActor und
 * war damit nur ueber ihn pruefbar. Sie ist fachlich und gehoert hierher.
 * Die dazugehoerige <em>Meldung</em> bleibt draussen: Was der Spieler zu
 * lesen bekommt, entscheidet nicht die Domaene — deshalb {@link #isValid}
 * als Frage neben {@link #of} als Zusicherung.
 */
public record PlayerName(String value) {

    public static final int MAX_LENGTH = 20;

    public PlayerName {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Name muss 1 bis " + MAX_LENGTH + " Zeichen haben");
        }
        value = value.trim();
    }

    public static PlayerName of(String value) {
        return new PlayerName(value);
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && trimmed.length() <= MAX_LENGTH;
    }

    @Override
    public String toString() {
        return value;
    }
}
