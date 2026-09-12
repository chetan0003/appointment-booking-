package com.jfl.appointment.dashboard.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        String code,
        String path,
        LocalDateTime timestamp,
        Map<String, String> validationErrors
) {
}
