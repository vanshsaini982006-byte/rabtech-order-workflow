package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.DomainException;

public class EmptyOrderException extends DomainException {

    public EmptyOrderException(OrderId orderId) {
        super("Order " + orderId + " has no lines and cannot be confirmed");
    }
}
