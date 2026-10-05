package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.money.Money;

public record OrderConfirmed(OrderId orderId, Money total) implements DomainEvent {
}
