package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.money.Money;
import java.util.Objects;

public record OrderLine(ProductId productId, Money unitPrice, int quantity) {

    public OrderLine {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new InvalidOrderLineException("Quantity must be a positive whole number but was " + quantity);
        }
    }

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }

    public OrderLine withQuantity(int newQuantity) {
        return new OrderLine(productId, unitPrice, newQuantity);
    }
}
