package com.rabtech.orderworkflow.domain.order;

import java.util.Objects;

public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Product id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
