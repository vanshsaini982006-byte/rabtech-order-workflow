package com.rabtech.orderworkflow.domain.order;

public record OrderCancelled(OrderId orderId, OrderStatus cancelledFrom) implements DomainEvent {
}
