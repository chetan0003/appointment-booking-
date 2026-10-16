package com.jfl.appointment.dashboard.dto;


import jakarta.validation.constraints.NotBlank;

public record MarkFailedRequest(@NotBlank String errorMessage) {}