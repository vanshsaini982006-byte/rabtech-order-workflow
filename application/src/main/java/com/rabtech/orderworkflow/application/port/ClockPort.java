package com.rabtech.orderworkflow.application.port;

import java.time.Instant;

public interface ClockPort {

    Instant now();
}
