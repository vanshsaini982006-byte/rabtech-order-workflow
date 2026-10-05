package com.rabtech.orderworkflow.domain.order;

import static com.rabtech.orderworkflow.domain.order.OrderStatus.CANCELLED;
import static com.rabtech.orderworkflow.domain.order.OrderStatus.CONFIRMED;
import static com.rabtech.orderworkflow.domain.order.OrderStatus.DRAFT;
import static com.rabtech.orderworkflow.domain.order.OrderStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderStatusTest {

    private static final Map<OrderStatus, Set<OrderStatus>> LEGAL_MOVES = Map.of(
            DRAFT, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(PAID, CANCELLED),
            PAID, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class));

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("everyPair")
    void onlyTheDocumentedMovesAreLegal(OrderStatus from, OrderStatus to) {
        assertEquals(LEGAL_MOVES.get(from).contains(to), from.canMoveTo(to));
    }

    private static Stream<Arguments> everyPair() {
        return Stream.of(OrderStatus.values())
                .flatMap(from -> Stream.of(OrderStatus.values()).map(to -> Arguments.of(from, to)));
    }
}
