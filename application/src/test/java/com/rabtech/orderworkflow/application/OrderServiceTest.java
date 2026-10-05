package com.rabtech.orderworkflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rabtech.orderworkflow.application.port.NotificationPort;
import com.rabtech.orderworkflow.application.port.OrderRepository;
import com.rabtech.orderworkflow.domain.money.Money;
import com.rabtech.orderworkflow.domain.order.DomainEvent;
import com.rabtech.orderworkflow.domain.order.EmptyOrderException;
import com.rabtech.orderworkflow.domain.order.InvalidOrderLineException;
import com.rabtech.orderworkflow.domain.order.InvalidOrderTransitionException;
import com.rabtech.orderworkflow.domain.order.Order;
import com.rabtech.orderworkflow.domain.order.OrderCancelled;
import com.rabtech.orderworkflow.domain.order.OrderConfirmed;
import com.rabtech.orderworkflow.domain.order.OrderId;
import com.rabtech.orderworkflow.domain.order.OrderStatus;
import com.rabtech.orderworkflow.domain.order.PaymentRecorded;
import com.rabtech.orderworkflow.domain.order.ProductId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Currency USD = Currency.getInstance("USD");
    private static final ProductId KEYBOARD = new ProductId("KEYBOARD");

    private final FakeOrders orders = new FakeOrders();
    private final RecordingNotifications notifications = new RecordingNotifications();
    private final OrderService service = new OrderService(orders, notifications, () -> NOW);

    private static Money usd(String amount) {
        return Money.of(amount, "USD");
    }

    private OrderId draftWithKeyboard() {
        OrderId id = service.createDraft(USD);
        service.addLine(id, KEYBOARD, usd("49.99"), 2);
        return id;
    }

    @Test
    void createDraftStoresANewDraftOrder() {
        OrderId id = service.createDraft(USD);

        Order stored = orders.stored.get(id);
        assertEquals(OrderStatus.DRAFT, stored.status());
        assertEquals(USD, stored.currency());
    }

    @Test
    void addLineIsStored() {
        OrderId id = draftWithKeyboard();

        assertEquals(usd("99.98"), orders.stored.get(id).total());
    }

    @Test
    void invalidQuantityIsRejectedByTheDomain() {
        OrderId id = service.createDraft(USD);

        assertThrows(InvalidOrderLineException.class, () -> service.addLine(id, KEYBOARD, usd("49.99"), 0));
        assertTrue(orders.stored.get(id).lines().isEmpty());
    }

    @Test
    void quantityChangeIsStored() {
        OrderId id = draftWithKeyboard();

        service.changeQuantity(id, KEYBOARD, 5);

        assertEquals(usd("249.95"), orders.stored.get(id).total());
    }

    @Test
    void confirmSavesTheOrderAndPublishesOrderConfirmedWithTheClockTime() {
        OrderId id = draftWithKeyboard();

        service.confirm(id);

        assertEquals(OrderStatus.CONFIRMED, orders.stored.get(id).status());
        assertEquals(List.of(new Sent(new OrderConfirmed(id, usd("99.98")), NOW)), notifications.sent);
    }

    @Test
    void paymentPublishesPaymentRecorded() {
        OrderId id = draftWithKeyboard();
        service.confirm(id);

        service.recordPayment(id);

        assertEquals(OrderStatus.PAID, orders.stored.get(id).status());
        assertEquals(new Sent(new PaymentRecorded(id, usd("99.98")), NOW), notifications.last());
    }

    @Test
    void cancelPublishesOrderCancelled() {
        OrderId id = draftWithKeyboard();

        service.cancel(id);

        assertEquals(OrderStatus.CANCELLED, orders.stored.get(id).status());
        assertEquals(List.of(new Sent(new OrderCancelled(id, OrderStatus.DRAFT), NOW)), notifications.sent);
    }

    @Test
    void eachEventIsPublishedOnlyOnce() {
        OrderId id = draftWithKeyboard();

        service.confirm(id);
        service.recordPayment(id);

        assertEquals(2, notifications.sent.size());
    }

    @Test
    void rejectedConfirmationPublishesNothing() {
        OrderId id = service.createDraft(USD);

        assertThrows(EmptyOrderException.class, () -> service.confirm(id));

        assertTrue(notifications.sent.isEmpty());
        assertEquals(OrderStatus.DRAFT, orders.stored.get(id).status());
    }

    @Test
    void rejectedPaymentPublishesNothingAndKeepsTheStatus() {
        OrderId id = draftWithKeyboard();
        service.cancel(id);
        notifications.sent.clear();

        assertThrows(InvalidOrderTransitionException.class, () -> service.recordPayment(id));

        assertTrue(notifications.sent.isEmpty());
        assertEquals(OrderStatus.CANCELLED, orders.stored.get(id).status());
    }

    @Test
    void unknownOrderIsReported() {
        OrderId unknown = OrderId.generate();

        assertThrows(OrderNotFoundException.class, () -> service.confirm(unknown));
        assertThrows(OrderNotFoundException.class, () -> service.getOrder(unknown));
        assertTrue(notifications.sent.isEmpty());
    }

    private record Sent(DomainEvent event, Instant at) {
    }

    private static class FakeOrders implements OrderRepository {

        private final Map<OrderId, Order> stored = new HashMap<>();

        @Override
        public void save(Order order) {
            stored.put(order.id(), order);
        }

        @Override
        public Optional<Order> findById(OrderId id) {
            return Optional.ofNullable(stored.get(id));
        }
    }

    private static class RecordingNotifications implements NotificationPort {

        private final List<Sent> sent = new ArrayList<>();

        @Override
        public void publish(DomainEvent event, Instant occurredAt) {
            sent.add(new Sent(event, occurredAt));
        }

        private Sent last() {
            return sent.get(sent.size() - 1);
        }
    }
}
