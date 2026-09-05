package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePatientRequest(

        @NotBlank(message = "Patient name is required")
        @Size(max = 150, message = "Patient name must not exceed 150 characters")
        String name,

        @NotBlank(message = "WhatsApp number is required")
        @Pattern(
                regexp = "^\\+?[0-9]{10,15}$",
                message = "Invalid WhatsApp number"
        )
        String whatsappNumber,

        @Email(message = "Invalid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        LocalDate dateOfBirth
) {
}
