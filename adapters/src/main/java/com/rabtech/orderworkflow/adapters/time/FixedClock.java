package com.rabtech.orderworkflow.adapters.time;

import com.rabtech.orderworkflow.application.port.ClockPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class FixedClock implements ClockPort {

    private Instant current;

    public FixedClock(Instant start) {
        this.current = Objects.requireNonNull(start, "start must not be null");
    }

    @Override
    public Instant now() {
        return current;
    }

    public void advance(Duration duration) {
        current = current.plus(duration);
    }
}
