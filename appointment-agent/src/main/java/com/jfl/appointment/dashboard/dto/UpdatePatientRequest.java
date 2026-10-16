package com.jfl.appointment.dashboard.dto;

import com.jfl.appointment.entity.Gender;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdatePatientRequest(
        @NotBlank
        String name,

        @NotBlank
        String whatsappNumber,

        String email,

        LocalDate dateOfBirth,

        Gender gender
) {
}
