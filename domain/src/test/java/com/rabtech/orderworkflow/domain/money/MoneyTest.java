package com.rabtech.orderworkflow.domain.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void amountIsHeldAtTheCurrencyScale() {
        assertEquals(new BigDecimal("10.00"), Money.of("10", "USD").amount());
        assertEquals(new BigDecimal("500"), Money.of("500", "JPY").amount());
    }

    @Test
    void sameValueWrittenDifferentlyIsEqual() {
        assertEquals(Money.of("10", "USD"), Money.of("10.00", "USD"));
    }

    @Test
    void tenCentsAddedThreeTimesIsExactlyThirtyCents() {
        Money tenCents = Money.of("0.10", "USD");

        assertEquals(Money.of("0.30", "USD"), tenCents.plus(tenCents).plus(tenCents));
    }

    @Test
    void timesMultipliesByAWholeFactor() {
        assertEquals(Money.of("59.97", "USD"), Money.of("19.99", "USD").times(3));
    }

    @Test
    void negativeAmountsAreRejected() {
        assertThrows(InvalidMoneyException.class, () -> Money.of("-0.01", "USD"));
    }

    @Test
    void negativeFactorIsRejected() {
        assertThrows(InvalidMoneyException.class, () -> Money.of("5.00", "USD").times(-1));
    }

    @Test
    void amountsFinerThanTheCurrencyAllowsAreRejectedNotRounded() {
        assertThrows(InvalidMoneyException.class, () -> Money.of("1.005", "USD"));
        assertThrows(InvalidMoneyException.class, () -> Money.of("0.5", "JPY"));
    }

    @Test
    void differentCurrenciesCannotBeAdded() {
        assertThrows(InvalidMoneyException.class, () -> Money.of("1.00", "USD").plus(Money.of("1.00", "EUR")));
    }

    @Test
    void zeroUsesTheGivenCurrency() {
        assertEquals(Money.of("0", "USD"), Money.zero(USD));
    }

    @Test
    void toStringShowsCurrencyAndAmount() {
        assertEquals("USD 12.50", Money.of("12.5", "USD").toString());
    }
}
