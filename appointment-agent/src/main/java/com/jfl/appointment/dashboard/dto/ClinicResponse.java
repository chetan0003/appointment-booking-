package com.jfl.appointment.dashboard.dto;

import java.time.LocalDateTime;

public record ClinicResponse(
        Long id,
        String name,
        String whatsappNumber,
        String timezone,
        boolean active,
        String countryCode,
        String state,
        String city,
        String postalCode,
        String addressLine1,
        String addressLine2,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) {
}