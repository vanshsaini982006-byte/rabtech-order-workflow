package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.money.Money;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Order {

    private final OrderId id;
    private final Currency currency;
    private final Map<ProductId, OrderLine> lines = new LinkedHashMap<>();
    private final List<DomainEvent> pendingEvents = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;

    private Order(OrderId id, Currency currency) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
    }

    public static Order draft(OrderId id, Currency currency) {
        return new Order(id, currency);
    }

    public void addLine(ProductId productId, Money unitPrice, int quantity) {
        requireModifiable();
        OrderLine line = new OrderLine(productId, unitPrice, quantity);
        if (!currency.equals(unitPrice.currency())) {
            throw new InvalidOrderLineException(
                    "Unit price is in " + unitPrice.currency() + " but the order is in " + currency);
        }
        if (lines.containsKey(productId)) {
            throw new InvalidOrderLineException(
                    "Order already has a line for product " + productId + ", change its quantity instead");
        }
        lines.put(productId, line);
    }

    public void changeQuantity(ProductId productId, int newQuantity) {
        requireModifiable();
        OrderLine existing = lines.get(productId);
        if (existing == null) {
            throw new InvalidOrderLineException("Order has no line for product " + productId);
        }
        lines.put(productId, existing.withQuantity(newQuantity));
    }

    public void confirm() {
        requireTransitionTo(OrderStatus.CONFIRMED);
        if (lines.isEmpty()) {
            throw new EmptyOrderException(id);
        }
        transitionTo(OrderStatus.CONFIRMED);
        pendingEvents.add(new OrderConfirmed(id, total()));
    }

    public void recordPayment() {
        transitionTo(OrderStatus.PAID);
        pendingEvents.add(new PaymentRecorded(id, total()));
    }

    public void cancel() {
        OrderStatus previous = status;
        transitionTo(OrderStatus.CANCELLED);
        pendingEvents.add(new OrderCancelled(id, previous));
    }

    public Money total() {
        return lines.values().stream()
                .map(OrderLine::lineTotal)
                .reduce(Money.zero(currency), Money::plus);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    public OrderId id() {
        return id;
    }

    public OrderStatus status() {
        return status;
    }

    public Currency currency() {
        return currency;
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines.values());
    }

    void transitionTo(OrderStatus target) {
        requireTransitionTo(target);
        status = target;
    }

    private void requireTransitionTo(OrderStatus target) {
        if (!status.canMoveTo(target)) {
            throw new InvalidOrderTransitionException(id, status, target);
        }
    }

    private void requireModifiable() {
        if (status != OrderStatus.DRAFT) {
            throw new OrderNotModifiableException(id, status);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Order that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Order[" + id + ", " + status + ", " + lines.size() + " lines]";
    }
}
