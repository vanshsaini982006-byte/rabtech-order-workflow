package com.rabtech.orderworkflow.application.port;

import com.rabtech.orderworkflow.domain.order.DomainEvent;
import java.time.Instant;

public interface NotificationPort {

    void publish(DomainEvent event, Instant occurredAt);
}
