package com.jfl.appointment.dashboard.dto;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        String path
) {
}