package com.rabtech.orderworkflow.adapters.time;

import com.rabtech.orderworkflow.application.port.ClockPort;
import java.time.Instant;

public class SystemClock implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
