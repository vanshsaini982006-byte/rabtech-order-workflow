package com.rabtech.orderworkflow.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rabtech.orderworkflow.domain.money.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderLineTest {

    private static final ProductId KEYBOARD = new ProductId("KEYBOARD");
    private static final Money PRICE = Money.of("49.99", "USD");

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100, Integer.MAX_VALUE})
    void positiveQuantitiesAreAccepted(int quantity) {
        assertEquals(quantity, new OrderLine(KEYBOARD, PRICE, quantity).quantity());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void zeroAndNegativeQuantitiesAreRejected(int quantity) {
        assertThrows(InvalidOrderLineException.class, () -> new OrderLine(KEYBOARD, PRICE, quantity));
    }

    @Test
    void lineTotalIsUnitPriceTimesQuantity() {
        assertEquals(Money.of("149.97", "USD"), new OrderLine(KEYBOARD, PRICE, 3).lineTotal());
    }

    @Test
    void changingQuantityProducesANewLineAndLeavesTheOriginalAlone() {
        OrderLine original = new OrderLine(KEYBOARD, PRICE, 1);

        OrderLine changed = original.withQuantity(4);

        assertNotSame(original, changed);
        assertEquals(1, original.quantity());
        assertEquals(4, changed.quantity());
        assertEquals(original.unitPrice(), changed.unitPrice());
    }

    @Test
    void changedQuantityIsValidatedLikeAnyOther() {
        OrderLine line = new OrderLine(KEYBOARD, PRICE, 1);

        assertThrows(InvalidOrderLineException.class, () -> line.withQuantity(0));
        assertThrows(InvalidOrderLineException.class, () -> line.withQuantity(-2));
    }
}
