package com.rabtech.orderworkflow.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Non-negative amount of money in a single currency.
 * Backed by BigDecimal and always held at the currency's own scale (2 for USD, 0 for JPY),
 * so 10 and 10.00 are the same value and no floating point is ever involved.
 * Amounts with more decimals than the currency allows are rejected instead of rounded.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        int fractionDigits = currency.getDefaultFractionDigits();
        if (fractionDigits < 0) {
            throw new InvalidMoneyException("Currency " + currency + " cannot hold a money amount");
        }
        if (amount.signum() < 0) {
            throw new InvalidMoneyException("Money cannot be negative: " + amount.toPlainString());
        }
        try {
            amount = amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidMoneyException(
                    amount.toPlainString() + " has more decimal places than " + currency + " allows");
        }
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new InvalidMoneyException("Cannot add " + other.currency + " to " + currency);
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
