package com.rabtech.orderworkflow.application;

import com.rabtech.orderworkflow.domain.order.OrderId;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(OrderId orderId) {
        super("No order found with id " + orderId);
    }
}
