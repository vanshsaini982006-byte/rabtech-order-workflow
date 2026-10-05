package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.money.Money;

public record PaymentRecorded(OrderId orderId, Money amountPaid) implements DomainEvent {
}
