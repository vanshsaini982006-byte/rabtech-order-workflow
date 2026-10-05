package com.rabtech.orderworkflow.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rabtech.orderworkflow.domain.money.Money;
import com.rabtech.orderworkflow.domain.order.Order;
import com.rabtech.orderworkflow.domain.order.OrderId;
import com.rabtech.orderworkflow.domain.order.OrderStatus;
import com.rabtech.orderworkflow.domain.order.ProductId;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();

    private static Order orderWithLine() {
        Order order = Order.draft(OrderId.generate(), Currency.getInstance("USD"));
        order.addLine(new ProductId("KEYBOARD"), Money.of("49.99", "USD"), 2);
        return order;
    }

    @Test
    void savedOrderCanBeRetrievedById() {
        Order order = orderWithLine();

        repository.save(order);

        assertSame(order, repository.findById(order.id()).orElseThrow());
    }

    @Test
    void retrievedOrderKeepsItsLinesAndTotal() {
        Order order = orderWithLine();
        repository.save(order);

        Order found = repository.findById(order.id()).orElseThrow();

        assertEquals(1, found.lines().size());
        assertEquals(Money.of("99.98", "USD"), found.total());
    }

    @Test
    void unknownIdGivesAnEmptyResult() {
        assertTrue(repository.findById(OrderId.generate()).isEmpty());
    }

    @Test
    void savingAgainReplacesTheStoredState() {
        Order order = orderWithLine();
        repository.save(order);

        order.confirm();
        repository.save(order);

        assertEquals(OrderStatus.CONFIRMED, repository.findById(order.id()).orElseThrow().status());
    }
}
