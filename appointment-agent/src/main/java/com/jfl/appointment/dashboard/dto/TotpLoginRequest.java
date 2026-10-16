package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpLoginRequest(

        @NotBlank
        String challengeToken,

        @NotBlank
        @Pattern(regexp = "\\d{6}")
        String code

) {}