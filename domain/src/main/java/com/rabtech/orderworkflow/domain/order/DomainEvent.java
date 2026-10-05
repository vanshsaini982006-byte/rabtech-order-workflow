package com.rabtech.orderworkflow.domain.order;

public sealed interface DomainEvent permits OrderConfirmed, PaymentRecorded, OrderCancelled {

    OrderId orderId();
}
