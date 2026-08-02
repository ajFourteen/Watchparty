package de.fourteen.watchparty.room;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Uhr mit von aussen gesetzter Zeit, damit Tests {@code closesAt}-Vergleiche
 * (ADR-011) deterministisch durchspielen koennen, statt echte Zeit verstreichen
 * zu lassen.
 */
public class FakeClock extends Clock {

    private Instant now;

    public FakeClock(Instant start) {
        this.now = start;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("wird im Test nicht gebraucht");
    }

    @Override
    public Instant instant() {
        return now;
    }
}
