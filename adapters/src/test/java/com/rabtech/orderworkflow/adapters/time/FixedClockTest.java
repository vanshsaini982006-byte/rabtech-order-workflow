package com.rabtech.orderworkflow.adapters.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FixedClockTest {

    private static final Instant START = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void alwaysReturnsTheSameInstantUntilAdvanced() {
        FixedClock clock = new FixedClock(START);

        assertEquals(START, clock.now());
        assertEquals(START, clock.now());
    }

    @Test
    void advanceMovesTheClockForward() {
        FixedClock clock = new FixedClock(START);

        clock.advance(Duration.ofMinutes(30));

        assertEquals(Instant.parse("2026-01-15T10:30:00Z"), clock.now());
    }

    @Test
    void systemClockReturnsTheCurrentTime() {
        Instant before = Instant.now();

        Instant reading = new SystemClock().now();

        assertFalse(reading.isBefore(before));
        assertFalse(reading.isAfter(Instant.now()));
    }
}
