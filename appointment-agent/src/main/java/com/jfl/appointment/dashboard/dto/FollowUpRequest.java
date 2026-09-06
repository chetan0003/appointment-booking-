package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FollowUpRequest(
        @NotNull
        LocalDate suggestedFollowUpDate
) {
}
