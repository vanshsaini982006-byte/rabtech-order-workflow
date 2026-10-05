package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.DomainException;

public class InvalidOrderTransitionException extends DomainException {

    private final OrderStatus from;
    private final OrderStatus to;

    public InvalidOrderTransitionException(OrderId orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot move from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public OrderStatus from() {
        return from;
    }

    public OrderStatus to() {
        return to;
    }
}
