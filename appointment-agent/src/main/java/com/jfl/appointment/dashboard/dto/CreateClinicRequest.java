package com.jfl.appointment.dashboard.dto;

public record CreateClinicRequest(
        String name,
        String whatsappNumber,
        String timezone
) {
}