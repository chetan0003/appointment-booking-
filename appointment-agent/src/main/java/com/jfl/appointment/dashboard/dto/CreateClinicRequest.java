package com.jfl.appointment.dashboard.dto;

public record CreateClinicRequest(
        String name,
        String whatsappNumber,
        String timezone,
        String countryCode,
        String state,
        String city,
        String postalCode,
        String addressLine1,
        String addressLine2,
        Double latitude,
        Double longitude
) {
}