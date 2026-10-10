package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpDisableRequest(

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Authenticator code is required")
        @Pattern(regexp = "\\d{6}", message = "Code must contain 6 digits")
        String code
) {}