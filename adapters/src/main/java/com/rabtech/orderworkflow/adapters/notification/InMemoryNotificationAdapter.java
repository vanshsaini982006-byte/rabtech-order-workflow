package com.rabtech.orderworkflow.adapters.notification;

import com.rabtech.orderworkflow.application.port.NotificationPort;
import com.rabtech.orderworkflow.domain.order.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InMemoryNotificationAdapter implements NotificationPort {

    public record Published(DomainEvent event, Instant occurredAt) {
    }

    private final List<Published> published = new ArrayList<>();

    @Override
    public void publish(DomainEvent event, Instant occurredAt) {
        published.add(new Published(event, occurredAt));
    }

    public List<Published> published() {
        return List.copyOf(published);
    }
}
