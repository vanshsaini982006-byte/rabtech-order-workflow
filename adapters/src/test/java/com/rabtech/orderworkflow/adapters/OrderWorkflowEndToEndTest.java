package com.rabtech.orderworkflow.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rabtech.orderworkflow.adapters.notification.InMemoryNotificationAdapter;
import com.rabtech.orderworkflow.adapters.notification.InMemoryNotificationAdapter.Published;
import com.rabtech.orderworkflow.adapters.persistence.InMemoryOrderRepository;
import com.rabtech.orderworkflow.adapters.time.FixedClock;
import com.rabtech.orderworkflow.application.OrderService;
import com.rabtech.orderworkflow.domain.money.Money;
import com.rabtech.orderworkflow.domain.order.InvalidOrderTransitionException;
import com.rabtech.orderworkflow.domain.order.OrderCancelled;
import com.rabtech.orderworkflow.domain.order.OrderConfirmed;
import com.rabtech.orderworkflow.domain.order.OrderId;
import com.rabtech.orderworkflow.domain.order.OrderStatus;
import com.rabtech.orderworkflow.domain.order.PaymentRecorded;
import com.rabtech.orderworkflow.domain.order.ProductId;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderWorkflowEndToEndTest {

    private static final Instant START = Instant.parse("2026-01-15T10:00:00Z");
    private static final ProductId KEYBOARD = new ProductId("KEYBOARD");
    private static final ProductId MOUSE = new ProductId("MOUSE");

    private final InMemoryNotificationAdapter notifications = new InMemoryNotificationAdapter();
    private final FixedClock clock = new FixedClock(START);
    private final OrderService service =
            new OrderService(new InMemoryOrderRepository(), notifications, clock);

    private OrderId draftWithTwoLines() {
        OrderId id = service.createDraft(Currency.getInstance("USD"));
        service.addLine(id, KEYBOARD, Money.of("49.99", "USD"), 1);
        service.addLine(id, MOUSE, Money.of("19.50", "USD"), 2);
        return id;
    }

    @Test
    void orderGoesFromDraftToPaidAndEveryStepIsAnnounced() {
        OrderId id = draftWithTwoLines();
        Money total = Money.of("88.99", "USD");

        service.confirm(id);
        clock.advance(Duration.ofMinutes(10));
        service.recordPayment(id);

        assertEquals(OrderStatus.PAID, service.getOrder(id).status());
        assertEquals(total, service.getOrder(id).total());
        assertEquals(List.of(
                new Published(new OrderConfirmed(id, total), START),
                new Published(new PaymentRecorded(id, total), START.plus(Duration.ofMinutes(10)))),
                notifications.published());
    }

    @Test
    void cancelledOrderCanNeverBePaid() {
        OrderId id = draftWithTwoLines();
        service.confirm(id);
        service.cancel(id);

        assertThrows(InvalidOrderTransitionException.class, () -> service.recordPayment(id));

        assertEquals(OrderStatus.CANCELLED, service.getOrder(id).status());
        assertEquals(2, notifications.published().size());
        assertEquals(new OrderCancelled(id, OrderStatus.CONFIRMED), notifications.published().get(1).event());
    }
}
