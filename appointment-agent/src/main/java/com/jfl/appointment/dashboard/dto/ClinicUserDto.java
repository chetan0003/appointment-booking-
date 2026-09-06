package com.jfl.appointment.dashboard.dto;

public record ClinicUserDto(
        Long clinicUserId,
        String name,
        String email,
        String role,
        boolean status,
        String lastLogin
) {
}