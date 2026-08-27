package com.jfl.appointment.dashboard.dto;

public record PatientResponseDto(
        Long id,
        String name,
        String phoneNo,
        Long clinicId
) {
}
