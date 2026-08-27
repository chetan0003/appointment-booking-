package com.jfl.appointment.dashboard.dto;

import java.time.LocalDateTime;

public record ClinicResponse(
        Long id,
        String name,
        String whatsappNumber,
        String timezone,
        boolean active,
        LocalDateTime createdAt
) {
}