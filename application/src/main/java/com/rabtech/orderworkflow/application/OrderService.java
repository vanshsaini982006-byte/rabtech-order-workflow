package com.rabtech.orderworkflow.application;

import com.rabtech.orderworkflow.application.port.ClockPort;
import com.rabtech.orderworkflow.application.port.NotificationPort;
import com.rabtech.orderworkflow.application.port.OrderRepository;
import com.rabtech.orderworkflow.domain.money.Money;
import com.rabtech.orderworkflow.domain.order.Order;
import com.rabtech.orderworkflow.domain.order.OrderId;
import com.rabtech.orderworkflow.domain.order.ProductId;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.function.Consumer;

public class OrderService {

    private final OrderRepository orders;
    private final NotificationPort notifications;
    private final ClockPort clock;

    public OrderService(OrderRepository orders, NotificationPort notifications, ClockPort clock) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
        this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public OrderId createDraft(Currency currency) {
        Order order = Order.draft(OrderId.generate(), currency);
        orders.save(order);
        return order.id();
    }

    public void addLine(OrderId orderId, ProductId productId, Money unitPrice, int quantity) {
        Order order = load(orderId);
        order.addLine(productId, unitPrice, quantity);
        orders.save(order);
    }

    public void changeQuantity(OrderId orderId, ProductId productId, int newQuantity) {
        Order order = load(orderId);
        order.changeQuantity(productId, newQuantity);
        orders.save(order);
    }

    public void confirm(OrderId orderId) {
        apply(orderId, Order::confirm);
    }

    public void recordPayment(OrderId orderId) {
        apply(orderId, Order::recordPayment);
    }

    public void cancel(OrderId orderId) {
        apply(orderId, Order::cancel);
    }

    public Order getOrder(OrderId orderId) {
        return load(orderId);
    }

    private void apply(OrderId orderId, Consumer<Order> transition) {
        Order order = load(orderId);
        transition.accept(order);
        orders.save(order);
        Instant occurredAt = clock.now();
        order.pullDomainEvents().forEach(event -> notifications.publish(event, occurredAt));
    }

    private Order load(OrderId orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
