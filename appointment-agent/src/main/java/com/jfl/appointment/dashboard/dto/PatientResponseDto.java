package com.jfl.appointment.dashboard.dto;

import java.time.LocalDate;

public record PatientResponseDto(
        Long id,
        String name,
        String phoneNo,
        String email,
        LocalDate dateOfBirth,
        Long clinicId
) {
}
