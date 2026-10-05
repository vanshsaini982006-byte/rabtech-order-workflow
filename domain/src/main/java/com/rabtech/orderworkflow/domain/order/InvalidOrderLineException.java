package com.rabtech.orderworkflow.domain.order;

import com.rabtech.orderworkflow.domain.DomainException;

public class InvalidOrderLineException extends DomainException {

    public InvalidOrderLineException(String message) {
        super(message);
    }
}
