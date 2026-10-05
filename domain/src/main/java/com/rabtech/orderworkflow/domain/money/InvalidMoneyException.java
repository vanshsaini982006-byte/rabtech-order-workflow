package com.rabtech.orderworkflow.domain.money;

import com.rabtech.orderworkflow.domain.DomainException;

public class InvalidMoneyException extends DomainException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}
