package com.jfl.appointment.n8n.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IdentifyByTokenRequest(@NotNull Long clinicId, @NotBlank String token) {}
