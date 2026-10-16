package com.jfl.appointment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class SubscriptionFeatureNotAvailableException
        extends RuntimeException {

    public SubscriptionFeatureNotAvailableException(
            String message) {
        super(message);
    }
}
