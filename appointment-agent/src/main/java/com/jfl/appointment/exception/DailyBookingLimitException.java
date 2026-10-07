package com.jfl.appointment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS) // 429
public class DailyBookingLimitException extends RuntimeException {
    public DailyBookingLimitException(String message) {
        super(message);
    }
}
