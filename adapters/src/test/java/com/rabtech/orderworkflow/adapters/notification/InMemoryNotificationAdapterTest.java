package com.rabtech.orderworkflow.adapters.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rabtech.orderworkflow.adapters.notification.InMemoryNotificationAdapter.Published;
import com.rabtech.orderworkflow.domain.order.OrderCancelled;
import com.rabtech.orderworkflow.domain.order.OrderId;
import com.rabtech.orderworkflow.domain.order.OrderStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryNotificationAdapterTest {

    private final InMemoryNotificationAdapter adapter = new InMemoryNotificationAdapter();

    @Test
    void startsEmpty() {
        assertTrue(adapter.published().isEmpty());
    }

    @Test
    void keepsPublishedEventsInOrderWithTheirTime() {
        OrderId id = OrderId.generate();
        Instant first = Instant.parse("2026-01-15T10:00:00Z");
        Instant second = Instant.parse("2026-01-15T10:05:00Z");
        OrderCancelled fromDraft = new OrderCancelled(id, OrderStatus.DRAFT);
        OrderCancelled fromConfirmed = new OrderCancelled(id, OrderStatus.CONFIRMED);

        adapter.publish(fromDraft, first);
        adapter.publish(fromConfirmed, second);

        assertEquals(List.of(new Published(fromDraft, first), new Published(fromConfirmed, second)),
                adapter.published());
    }

    @Test
    void publishedListCannotBeModifiedFromOutside() {
        assertThrows(UnsupportedOperationException.class, () -> adapter.published().clear());
    }
}
