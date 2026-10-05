package com.rabtech.orderworkflow.domain.order;

public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    PAID,
    CANCELLED;

    public boolean canMoveTo(OrderStatus target) {
        return switch (this) {
            case DRAFT -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PAID || target == CANCELLED;
            case PAID, CANCELLED -> false;
        };
    }
}
