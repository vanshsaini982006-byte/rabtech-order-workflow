package com.rabtech.orderworkflow.application.port;

import com.rabtech.orderworkflow.domain.order.Order;
import com.rabtech.orderworkflow.domain.order.OrderId;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);
}
