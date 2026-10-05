package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.DomainException;

public class OrderNotModifiableException extends DomainException {

    public OrderNotModifiableException(OrderId orderId, OrderStatus status) {
        super("Order " + orderId + " is " + status + " and its lines can no longer be changed");
    }
}
