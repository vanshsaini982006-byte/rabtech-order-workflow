package com.rabtech.orderworkflow.adapters.persistence;

import com.rabtech.orderworkflow.application.port.OrderRepository;
import com.rabtech.orderworkflow.domain.order.Order;
import com.rabtech.orderworkflow.domain.order.OrderId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> orders = new HashMap<>();

    @Override
    public void save(Order order) {
        orders.put(order.id(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(orders.get(id));
    }
}
